package com.ledgerx.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LedgerXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
