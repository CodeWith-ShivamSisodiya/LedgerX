package com.ledgerx.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.ledgerx.app.core.database.LedgerDao
import com.ledgerx.app.core.database.LedgerXDatabase
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
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun ledgerDao(database: LedgerXDatabase): LedgerDao = database.ledgerDao()
}
