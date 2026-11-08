package com.ledgerx.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ledgerx.app.core.planning.RecurringWorker
import com.ledgerx.app.core.planning.ReminderWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class LedgerXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        val recurringWork = PeriodicWorkRequestBuilder<RecurringWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ledgerx-recurring-transactions",
            ExistingPeriodicWorkPolicy.UPDATE,
            recurringWork,
        )
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ledgerx-financial-reminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderWork,
        )
    }
}
