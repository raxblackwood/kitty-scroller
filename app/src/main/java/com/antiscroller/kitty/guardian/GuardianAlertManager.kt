package com.antiscroller.kitty.guardian

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
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
 * Alerts are sent silently in the background exclusively to the guardian endpoint
 * with zero intrusive local notifications displayed on the monitored device.
 */
object GuardianAlertManager {

    private const val TAG = "GuardianAlertManager"

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
            Log.w(TAG, "No guardian endpoint configured. Alert $eventType skipped.")
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
            EVENT_UNINSTALL_ATTEMPT -> "🚨 CRITICAL: Clawse Uninstall Attempted!"
            EVENT_ADMIN_DISABLE_ATTEMPT -> "⚠️ WARNING: Device Admin Deactivation Attempted!"
            EVENT_ADMIN_DISABLED -> "🚨 DANGER: Device Admin Deactivated!"
            EVENT_HEARTBEAT -> "💚 Child Device Heartbeat: Active"
            EVENT_SETUP_TEST -> "🔔 Clawse Guardian Alert Setup Test"
            else -> "🛡️ Clawse Security Alert"
        }

        val summary = when (eventType) {
            EVENT_UNINSTALL_ATTEMPT -> "Someone attempted to uninstall Clawse on $deviceName. The system intercepted and cancelled the dialog."
            EVENT_ADMIN_DISABLE_ATTEMPT -> "An attempt to revoke Device Administrator privileges for Clawse was made on $deviceName."
            EVENT_ADMIN_DISABLED -> "Device Administrator protection was turned off on $deviceName!"
            EVENT_HEARTBEAT -> "$deviceName is online and Clawse screen-time shield is running."
            EVENT_SETUP_TEST -> "Testing connection from child device $deviceName. Guardian alerts are functioning properly."
            else -> details
        }

        val formattedMessage = "$title\nDevice: $deviceName\nTime: $timestamp\nDetails: $summary"

        val jsonPayload = JSONObject().apply {
            put("app", "Clawse")
            put("event", eventType)
            put("title", title)
            put("device", deviceName)
            put("timestamp", timestamp)
            put("message", summary)
            put("text", formattedMessage)     // Slack compatibility
            put("content", formattedMessage)  // Discord compatibility
            put("topic", "clawse_alerts")     // ntfy.sh compatibility
        }

        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "Clawse-Android-Guardian/1.0")
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
}
