package com.antiscroller.kitty.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.antiscroller.kitty.R

/**
 * Device Admin Receiver that protects AntiScroller against impulsive deletion.
 * When enabled, the user cannot uninstall the app from the home screen or settings
 * without first manually disabling Device Admin, introducing necessary cognitive friction.
 */
class KittyAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "ClawBack Protection Enabled! Impulsive uninstall is now guarded.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.admin_warning_disable)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "ClawBack Protection Disabled.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
