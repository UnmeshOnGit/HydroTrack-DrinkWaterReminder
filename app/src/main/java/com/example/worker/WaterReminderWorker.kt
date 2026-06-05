package com.example.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.data.WaterRepository
import com.example.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class WaterReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val repository = WaterRepository(applicationContext)
        
        // Only trigger reminders if they are enabled in the settings
        if (repository.getRemindersEnabled()) {
            NotificationHelper.showWaterReminderNotification(applicationContext)
        }
        
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "WaterReminderWork"

        /**
         * Schedules periodic reminders in WorkManager.
         * If the work is already enqueued, setting EXISTING_WORK_POLICY to UPDATE will apply changes
         * (e.g. if the user changes the interval).
         */
        fun scheduleReminder(context: Context, intervalMins: Int) {
            val workManager = WorkManager.getInstance(context)
            
            val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                intervalMins.toLong(), TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        /**
         * Cancels all scheduled reminder tasks in WorkManager.
         */
        fun cancelReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
