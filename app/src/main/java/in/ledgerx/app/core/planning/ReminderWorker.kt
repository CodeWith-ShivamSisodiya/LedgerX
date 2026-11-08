package com.ledgerx.app.core.planning

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ledgerx.app.R
import com.ledgerx.app.core.database.PlanningDao
import com.ledgerx.app.core.database.ExtendedDao
import com.ledgerx.app.core.security.SecurityPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies { fun planningDao(): PlanningDao; fun extendedDao(): ExtendedDao; fun securityPreferences(): SecurityPreferences }

    override suspend fun doWork(): Result = runCatching {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val dependencies = EntryPoints.get(applicationContext, Dependencies::class.java)
        if (!dependencies.securityPreferences().remindersEnabled.first()) return Result.success()
        val dao = dependencies.planningDao()
        val extended = dependencies.extendedDao()
        val today = LocalDate.now()
        createChannel()
        dao.billsDueThrough(today.plusDays(7).toEpochDay()).forEach { notify(it.id, "Bill due ${LocalDate.ofEpochDay(it.dueEpochDay)}", it.name) }
        dao.loansDueThrough(today.plusDays(7).toEpochDay()).forEach { notify(it.id, "Loan payment due ${LocalDate.ofEpochDay(it.nextDueEpochDay!!)}", it.name) }
        dao.policiesDueThrough(today.plusDays(30).toEpochDay()).forEach { notify(it.id, "Insurance renewal ${LocalDate.ofEpochDay(it.renewalEpochDay)}", it.name) }
        dao.recurringDueThrough(today.plusDays(7).toEpochDay()).forEach { notify(it.id, "${it.type.lowercase().replaceFirstChar(Char::uppercase)} due ${LocalDate.ofEpochDay(it.nextDueEpochDay)}", it.merchant ?: "Recurring transaction") }
        dao.depositsDueThrough(today.plusDays(30).toEpochDay()).forEach { notify(it.id, "Deposit matures ${LocalDate.ofEpochDay(it.maturityEpochDay)}", it.name) }
        dao.goalsDueThrough(today.plusDays(30).toEpochDay()).forEach { notify(it.id, "Goal milestone ${LocalDate.ofEpochDay(it.targetEpochDay)}", it.name) }
        extended.sipsDueThrough(today.plusDays(7).toEpochDay()).forEach { notify(it.id, "SIP due ${LocalDate.ofEpochDay(it.nextDueEpochDay)}", "Confirm the actual purchase price and units") }
        extended.cardStatementsDueThrough(today.plusDays(7).toEpochDay()).forEach { notify(it.id, "Card payment due ${LocalDate.ofEpochDay(it.dueEpochDay)}", "Statement payment remains ${it.status.lowercase().replace('_',' ')}") }
        val throughMillis = today.plusDays(30).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.notificationsDueThrough(throughMillis).forEach { notify(it.id, it.title, it.sourceType.replace('_',' ').lowercase().replaceFirstChar(Char::uppercase)) }
        Result.success()
    }.getOrElse { Result.retry() }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Financial reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Upcoming bills, EMIs, and insurance renewals"
        })
    }

    private fun notify(key: String, title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(key.hashCode(), notification)
    }

    companion object { const val CHANNEL_ID = "financial-reminders" }
}
