package com.ledgerx.app.core.security

import android.content.Context
import com.ledgerx.app.core.database.LedgerXDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataDeletionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LedgerXDatabase,
    private val preferences: SecurityPreferences,
    private val keyManager: SecureDatabaseKeyManager,
) {
    suspend fun eraseAllLocalData(confirmation: String) = withContext(Dispatchers.IO) {
        require(confirmation == CONFIRMATION) { "Type $CONFIRMATION exactly" }
        preferences.clear()
        database.clearAllTables()
        database.openHelper.writableDatabase.execSQL("VACUUM")
        database.close()
        check(context.deleteDatabase(DATABASE_NAME) || !context.getDatabasePath(DATABASE_NAME).exists()) { "Unable to remove the local database" }
        keyManager.destroyKeyMaterial()
    }

    companion object {
        const val CONFIRMATION = "DELETE LEDGERX"
        private const val DATABASE_NAME = "ledgerx.db"
    }
}
