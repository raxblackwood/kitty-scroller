package com.antiscroller.kitty.guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Handles real-time guardian security alert dispatches when tamper,
 * deactivation, or uninstallation attempts occur on a monitored child device.
 */
object GuardianAlertManager {

    private const val TAG = "GuardianAlertManager"
    private const val CHANNEL_ID = "clawback_guardian_alerts"

    const val EVENT_SETUP_TEST = "SETUP_TEST"
    const val EVENT_UNINSTALL_ATTEMPT = "UNINSTALL_ATTEMPT"
    const val EVENT_ADMIN_DISABLE_ATTEMPT = "ADMIN_DISABLE_ATTEMPT"
    const val EVENT_ADMIN_DISABLED = "ADMIN_DISABLED"
    const val EVENT_HEARTBEAT = "HEARTBEAT_PING"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendAlert(
        context: Context,
        eventType: String,
        details: String = "",
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        val dataManager = ScrollDataManager.getInstance(appContext)

        val isTest = eventType == EVENT_SETUP_TEST
        if (!isTest && !dataManager.isChildModeEnabled) {
            Log.d(TAG, "Child mode is not active; skipping guardian alert ($eventType)")
            callback?.let { mainHandler.post { it(true, "Child mode inactive") } }
            return
        }

        val endpoint = dataManager.guardianAlertEndpoint.trim()
        if (endpoint.isEmpty()) {
            Log.w(TAG, "No guardian endpoint configured. Alert $eventType recorded locally.")
            showLocalNotification(appContext, eventType, details, success = false)
            callback?.let { mainHandler.post { it(false, "No guardian endpoint configured") } }
            return
        }

        executor.execute {
            val success = try {
                dispatchNetworkAlert(appContext, dataManager, endpoint, eventType, details)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch alert $eventType to $endpoint: ${e.message}", e)
                false
            }

            mainHandler.post {
                showLocalNotification(appContext, eventType, details, success)
                callback?.invoke(success, if (success) "Alert sent successfully" else "Failed to connect to endpoint")
            }
        }
    }

    private fun dispatchNetworkAlert(
        context: Context,
        dataManager: ScrollDataManager,
        rawEndpoint: String,
        eventType: String,
        details: String
    ): Boolean {
        // Resolve URL (Support raw HTTPS URL or ntfy topic)
        val targetUrl = when {
            rawEndpoint.startsWith("http://") || rawEndpoint.startsWith("https://") -> rawEndpoint
            rawEndpoint.contains("@") -> "https://ntfy.sh/${rawEndpoint.replace("@", "_at_")}"
            else -> "https://ntfy.sh/$rawEndpoint"
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        val deviceName = dataManager.childDeviceName.ifEmpty { Build.MODEL }

        val title = when (eventType) {
            EVENT_UNINSTALL_ATTEMPT -> "🚨 CRITICAL: ClawBack Uninstall Attempted!"
            EVENT_ADMIN_DISABLE_ATTEMPT -> "⚠️ WARNING: Device Admin Deactivation Attempted!"
            EVENT_ADMIN_DISABLED -> "🚨 DANGER: Device Admin Deactivated!"
            EVENT_HEARTBEAT -> "💚 Child Device Heartbeat: Active"
            EVENT_SETUP_TEST -> "🔔 ClawBack Guardian Alert Setup Test"
            else -> "🛡️ ClawBack Security Alert"
        }

        val summary = when (eventType) {
            EVENT_UNINSTALL_ATTEMPT -> "Someone attempted to uninstall ClawBack on $deviceName. The system intercepted and cancelled the dialog."
            EVENT_ADMIN_DISABLE_ATTEMPT -> "An attempt to revoke Device Administrator privileges for ClawBack was made on $deviceName."
            EVENT_ADMIN_DISABLED -> "Device Administrator protection was turned off on $deviceName!"
            EVENT_HEARTBEAT -> "$deviceName is online and ClawBack screen-time shield is running."
            EVENT_SETUP_TEST -> "Testing connection from child device $deviceName. Guardian alerts are functioning properly."
            else -> details
        }

        val formattedMessage = "$title\nDevice: $deviceName\nTime: $timestamp\nDetails: $summary"

        val jsonPayload = JSONObject().apply {
            put("app", "ClawBack")
            put("event", eventType)
            put("title", title)
            put("device", deviceName)
            put("timestamp", timestamp)
            put("message", summary)
            put("text", formattedMessage)     // Slack compatibility
            put("content", formattedMessage)  // Discord compatibility
            put("topic", "clawback_alerts")   // ntfy.sh compatibility
        }

        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "ClawBack-Android-Guardian/1.0")
            if (targetUrl.contains("ntfy.sh")) {
                setRequestProperty("Title", title)
                setRequestProperty("Priority", if (eventType == EVENT_UNINSTALL_ATTEMPT || eventType == EVENT_ADMIN_DISABLED) "high" else "default")
                setRequestProperty("Tags", if (eventType == EVENT_HEARTBEAT) "shield" else "warning,rotating_light")
            }
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonPayload.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        conn.disconnect()
        val ok = responseCode in 200..299

        if (ok) {
            dataManager.lastHeartbeatTimestamp = System.currentTimeMillis()
            Log.i(TAG, "Guardian alert $eventType successfully sent to $targetUrl (HTTP $responseCode)")
        } else {
            Log.w(TAG, "Guardian alert $eventType returned HTTP $responseCode from $targetUrl")
        }

        return ok
    }

    private fun showLocalNotification(
        context: Context,
        eventType: String,
        details: String,
        success: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guardian Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when tampering or uninstall attempts occur on child phone"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (success) "🛡️ Guardian Alert Delivered" else "⚠️ Guardian Alert Failed to Send"
        val text = when (eventType) {
            EVENT_UNINSTALL_ATTEMPT -> "Uninstall attempt was blocked & guardian alerted."
            EVENT_ADMIN_DISABLE_ATTEMPT -> "Admin deactivation attempt notified to guardian."
            EVENT_SETUP_TEST -> if (success) "Guardian webhook test succeeded!" else "Could not reach guardian endpoint."
            else -> details.ifEmpty { "Security event logged." }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_kitty_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventType.hashCode(), notification)
    }
}
