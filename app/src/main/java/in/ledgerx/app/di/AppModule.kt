package com.ledgerx.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.ledgerx.app.core.database.WealthNotesDao
import com.ledgerx.app.core.database.LedgerDao
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.PlanningDao
import com.ledgerx.app.core.database.DatabaseMigrations
import com.ledgerx.app.core.database.ExtendedDao
import com.ledgerx.app.core.security.SecureDatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        keyManager: SecureDatabaseKeyManager,
    ): LedgerXDatabase {
        val factory = SupportOpenHelperFactory(keyManager.getOrCreatePassphrase())
        return Room.databaseBuilder(context, LedgerXDatabase::class.java, "ledgerx.db")
            .openHelperFactory(factory)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun wealthNotesDao(database: LedgerXDatabase): WealthNotesDao = database.wealthNotesDao()
    @Provides fun ledgerDao(database: LedgerXDatabase): LedgerDao = database.ledgerDao()
    @Provides fun planningDao(database: LedgerXDatabase): PlanningDao = database.planningDao()
    @Provides fun extendedDao(database: LedgerXDatabase): ExtendedDao = database.extendedDao()
}
