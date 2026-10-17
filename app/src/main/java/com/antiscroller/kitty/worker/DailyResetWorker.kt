package com.antiscroller.kitty.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.antiscroller.kitty.data.ScrollDataManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that executes at midnight (00:00) to reset the kitty curtain
 * back up to the ceiling for the new day.
 */
class DailyResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "DailyResetWorker"
        private const val WORK_NAME = "kitty_daily_reset_work"

        /**
         * Schedules a daily reset worker that triggers around midnight every day.
         */
        fun scheduleMidnightReset(context: Context) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }

            val initialDelayMs = midnight.timeInMillis - now.timeInMillis

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // 15 min flex window
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
            Log.i(TAG, "Midnight reset scheduled! Initial delay: ${initialDelayMs / 1000 / 60} minutes.")
        }
    }

    override fun doWork(): Result {
        Log.i(TAG, "Executing midnight reset of Kitty Curtain...")
        val dataManager = ScrollDataManager.getInstance(applicationContext)
        dataManager.resetDailyCount()
        return Result.success()
    }
}
