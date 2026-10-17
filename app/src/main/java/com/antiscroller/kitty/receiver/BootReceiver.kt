package com.antiscroller.kitty.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antiscroller.kitty.worker.DailyResetWorker

/**
 * Reschedules daily midnight resets upon device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device reboot completed. Rescheduling Kitty Daily Reset...")
            DailyResetWorker.scheduleMidnightReset(context)
        }
    }
}
