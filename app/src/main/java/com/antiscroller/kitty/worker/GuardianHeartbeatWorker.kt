package com.antiscroller.kitty.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.guardian.GuardianAlertManager
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that acts as a "Dead Man's Switch".
 * Regularly pings the guardian endpoint with a heartbeat. If the child device
 * goes completely offline or is force-uninstalled, missing heartbeats alert the guardian.
 */
class GuardianHeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dataManager = ScrollDataManager.getInstance(applicationContext)

        if (!dataManager.isChildModeEnabled) {
            Log.d(TAG, "Child mode is not active; skipping heartbeat ping.")
            return Result.success()
        }

        Log.i(TAG, "Dispatching guardian periodic heartbeat...")
        GuardianAlertManager.sendAlert(
            applicationContext,
            GuardianAlertManager.EVENT_HEARTBEAT,
            "Periodic keepalive ping"
        )

        return Result.success()
    }

    companion object {
        private const val TAG = "GuardianHeartbeatWorker"
        private const val WORK_NAME = "guardian_heartbeat_worker"

        fun scheduleHeartbeat(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GuardianHeartbeatWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Guardian heartbeat worker scheduled (15 min interval)")
        }

        fun cancelHeartbeat(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Guardian heartbeat worker cancelled")
        }
    }
}
