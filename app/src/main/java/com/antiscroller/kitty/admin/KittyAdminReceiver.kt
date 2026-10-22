package com.antiscroller.kitty.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.antiscroller.kitty.R
import com.antiscroller.kitty.guardian.GuardianAlertManager

/**
 * Device Admin Receiver that protects ClawBack against impulsive deletion.
 * Supports standard Device Admin friction as well as Android Enterprise Device Owner mode
 * (which enforces OS-level setUninstallBlocked).
 */
class KittyAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val admin = ComponentName(context, KittyAdminReceiver::class.java)
        try {
            dpm.setUninstallBlocked(admin, context.packageName, true)
            Toast.makeText(
                context,
                "🛡️ Device Owner Active: Pawse is now un-uninstallable!",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "Pawse Protection Enabled! Impulsive uninstall is now guarded.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        GuardianAlertManager.sendAlert(
            context,
            GuardianAlertManager.EVENT_ADMIN_DISABLE_ATTEMPT,
            "User requested Device Administrator deactivation."
        )
        return context.getString(R.string.admin_warning_disable)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        GuardianAlertManager.sendAlert(
            context,
            GuardianAlertManager.EVENT_ADMIN_DISABLED,
            "Device Administrator privileges were revoked!"
        )
        Toast.makeText(
            context,
            "Pawse Protection Disabled.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
