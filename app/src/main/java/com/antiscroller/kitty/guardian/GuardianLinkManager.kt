package com.antiscroller.kitty.guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.antiscroller.kitty.BuildConfig
import com.antiscroller.kitty.data.ScrollDataManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Firebase-backed pairing between a guardian phone and a child phone.
 *
 * Both phones sign in anonymously (no accounts). The guardian creates a
 * `links/{code}` document holding its FCM token; the child claims it by entering
 * the code. Child alerts are posted to the Cloudflare Worker relay (see /worker),
 * which pushes them to the guardian. Missed check-ins are detected by the relay's
 * scheduled sweep from `links/{code}.lastHeartbeat`.
 */
object GuardianLinkManager {

    private const val TAG = "GuardianLinkManager"

    const val CHANNEL_ID = "guardian_alerts"
    private const val COLLECTION_LINKS = "links"
    private const val CODE_LENGTH = 8
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I
    private const val PAIRING_WINDOW_MINUTES = 15L
    private const val NETWORK_TIMEOUT_SECONDS = 10L

    data class LinkStatus(
        val code: String,
        val isPaired: Boolean,
        val childDeviceName: String,
        val lastHeartbeat: Date?,
        val isOffline: Boolean
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val random = SecureRandom()

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun formatCode(code: String): String =
        if (code.length == CODE_LENGTH) "${code.take(4)}-${code.drop(4)}" else code

    fun normalizeCode(input: String): String =
        input.uppercase().filter { it in CODE_ALPHABET }

    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Guardian Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Uninstall attempts and missed check-ins from linked child phones"
        }
        manager.createNotificationChannel(channel)
    }

    // ==========================================
    // GUARDIAN PHONE
    // ==========================================

    /** Creates a new pairing code on the guardian phone. Callback receives the code, or null and an error. */
    fun createLink(context: Context, guardianName: String, callback: (String?, String) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val result = try {
                val uid = ensureSignedIn()
                val token = await(FirebaseMessaging.getInstance().token)
                val code = generateCode()
                val expires = Timestamp(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(PAIRING_WINDOW_MINUTES)))
                val data = hashMapOf(
                    "guardianUid" to uid,
                    "guardianToken" to token,
                    "guardianName" to guardianName,
                    "childUid" to null,
                    "childDeviceName" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "pairingExpiresAt" to expires,
                    "offlineAlertSent" to false
                )
                await(db.collection(COLLECTION_LINKS).document(code).set(data))
                val dataManager = ScrollDataManager.getInstance(appContext)
                dataManager.guardianModeLinkCodes = dataManager.guardianModeLinkCodes + code
                code to "Pairing code created"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create guardian link: ${e.message}", e)
                null to friendlyError(e)
            }
            mainHandler.post { callback(result.first, result.second) }
        }
    }

    /** Watches a link until a child phone claims it. Returns a registration to remove. */
    fun watchForPairing(code: String, onPaired: (String) -> Unit): ListenerRegistration =
        db.collection(COLLECTION_LINKS).document(code).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists() && snap.getString("childUid") != null) {
                onPaired(snap.getString("childDeviceName").orEmpty())
            }
        }

    /** Loads the status of every child phone linked to this guardian phone. */
    fun loadGuardianLinks(context: Context, callback: (List<LinkStatus>) -> Unit) {
        val dataManager = ScrollDataManager.getInstance(context.applicationContext)
        val codes = dataManager.guardianModeLinkCodes
        if (codes.isEmpty()) {
            callback(emptyList())
            return
        }
        executor.execute {
            val statuses = mutableListOf<LinkStatus>()
            val stale = mutableSetOf<String>()
            try {
                ensureSignedIn()
                for (code in codes) {
                    val snap = await(db.collection(COLLECTION_LINKS).document(code).get())
                    val childUid = if (snap.exists()) snap.getString("childUid") else null
                    val expires = snap.getTimestamp("pairingExpiresAt")?.toDate()
                    // Drop codes that were deleted or expired before a child claimed them.
                    if (!snap.exists() || (childUid == null && expires != null && expires.before(Date()))) {
                        stale += code
                        continue
                    }
                    statuses += LinkStatus(
                        code = code,
                        isPaired = childUid != null,
                        childDeviceName = snap.getString("childDeviceName").orEmpty(),
                        lastHeartbeat = snap.getTimestamp("lastHeartbeat")?.toDate(),
                        isOffline = snap.getBoolean("offlineAlertSent") == true
                    )
                }
                if (stale.isNotEmpty()) {
                    dataManager.guardianModeLinkCodes = dataManager.guardianModeLinkCodes - stale
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load guardian links: ${e.message}", e)
            }
            mainHandler.post { callback(statuses) }
        }
    }

    /** Removes a link from either side. */
    fun unlink(context: Context, code: String, callback: (Boolean) -> Unit) {
        val dataManager = ScrollDataManager.getInstance(context.applicationContext)
        executor.execute {
            val ok = try {
                ensureSignedIn()
                await(db.collection(COLLECTION_LINKS).document(code).delete())
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unlink $code: ${e.message}", e)
                false
            }
            if (ok) {
                dataManager.guardianModeLinkCodes = dataManager.guardianModeLinkCodes - code
                if (dataManager.guardianLinkCode == code) dataManager.guardianLinkCode = ""
            }
            mainHandler.post { callback(ok) }
        }
    }

    /** Pushes a refreshed FCM token to every link this guardian phone owns. */
    fun updateGuardianToken(context: Context, token: String) {
        val codes = ScrollDataManager.getInstance(context.applicationContext).guardianModeLinkCodes
        if (codes.isEmpty()) return
        executor.execute {
            try {
                ensureSignedIn()
                for (code in codes) {
                    await(db.collection(COLLECTION_LINKS).document(code).update("guardianToken", token))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update guardian token: ${e.message}", e)
            }
        }
    }

    // ==========================================
    // CHILD PHONE
    // ==========================================

    /** Claims a guardian's pairing code from the child phone. */
    fun claimLink(context: Context, rawCode: String, deviceName: String, callback: (Boolean, String) -> Unit) {
        val appContext = context.applicationContext
        val code = normalizeCode(rawCode)
        if (code.length != CODE_LENGTH) {
            callback(false, "Pairing codes are $CODE_LENGTH letters/numbers")
            return
        }
        executor.execute {
            val result = try {
                val uid = ensureSignedIn()
                val ref = db.collection(COLLECTION_LINKS).document(code)
                val snap = await(ref.get())
                val expires = snap.getTimestamp("pairingExpiresAt")?.toDate()
                when {
                    !snap.exists() -> false to "Code not found. Check it on the guardian phone."
                    snap.getString("childUid") == uid -> {
                        ScrollDataManager.getInstance(appContext).guardianLinkCode = code
                        true to "Already linked"
                    }
                    snap.getString("childUid") != null -> false to "This code is already used by another phone."
                    expires != null && expires.before(Date()) -> false to "Code expired. Create a new one on the guardian phone."
                    else -> {
                        await(ref.update(mapOf(
                            "childUid" to uid,
                            "childDeviceName" to deviceName,
                            "pairedAt" to FieldValue.serverTimestamp(),
                            "lastHeartbeat" to FieldValue.serverTimestamp()
                        )))
                        ScrollDataManager.getInstance(appContext).guardianLinkCode = code
                        try {
                            sendAlertBlocking(
                                code, "PAIRED", "🛡️ Phone linked",
                                "$deviceName is now protected. You'll be alerted here if Clawse is tampered with.",
                                deviceName
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Linked, but the confirmation push failed: ${e.message}")
                        }
                        true to "Linked to the guardian's phone"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to claim link $code: ${e.message}", e)
                false to friendlyError(e)
            }
            mainHandler.post { callback(result.first, result.second) }
        }
    }

    /** Blocking: asks the relay to push an alert to the guardian. Call from a background thread. */
    fun sendAlertBlocking(code: String, eventType: String, title: String, body: String, deviceName: String): Boolean {
        ensureSignedIn()
        val idToken = await(auth.currentUser!!.getIdToken(false)).token
            ?: throw IllegalStateException("No Firebase ID token")
        val payload = JSONObject().apply {
            put("code", code)
            put("type", eventType)
            put("title", title.take(120))
            put("body", body.take(500))
            put("deviceName", deviceName.take(60))
        }
        val conn = (URL("${BuildConfig.GUARDIAN_RELAY_URL}/alert").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $idToken")
        }
        try {
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val status = conn.responseCode
            if (status !in 200..299) Log.w(TAG, "Guardian relay returned HTTP $status for $eventType")
            return status in 200..299
        } finally {
            conn.disconnect()
        }
    }

    /** Blocking: records a check-in so the server knows this phone is alive. */
    fun sendHeartbeatBlocking(code: String, deviceName: String): Boolean {
        ensureSignedIn()
        await(db.collection(COLLECTION_LINKS).document(code).update(mapOf(
            "lastHeartbeat" to FieldValue.serverTimestamp(),
            "childDeviceName" to deviceName.take(60)
        )))
        return true
    }

    // ==========================================
    // INTERNALS
    // ==========================================

    private fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = await(auth.signInAnonymously())
        return result.user?.uid ?: throw IllegalStateException("Anonymous sign-in returned no user")
    }

    private fun friendlyError(e: Exception): String {
        val cause = (e as? java.util.concurrent.ExecutionException)?.cause ?: e
        return when {
            cause is FirebaseFirestoreException && cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "This code is already used or has expired."
            cause is java.util.concurrent.TimeoutException -> "No internet connection. Try again."
            else -> cause.message ?: "Could not reach Firebase"
        }
    }

    private fun generateCode(): String =
        (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    private fun <T> await(task: Task<T>): T = Tasks.await(task, NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
}
