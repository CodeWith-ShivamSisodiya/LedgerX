package com.ledgerx.app.core.planning

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class RecurringWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            Dependencies::class.java,
        )
        dependencies.processor().processDue()
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() },
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun processor(): RecurringProcessor
    }
}
