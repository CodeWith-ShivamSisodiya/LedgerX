package com.ledgerx.app.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun migrationOneToThreePreservesLedgerAndCreatesAllFeatureTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("ledgerx-migration-test.db")
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v1 = factory.create(config(context, 1, onCreate = { db ->
            db.execSQL("CREATE TABLE accounts(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
            db.execSQL("CREATE TABLE categories(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL, icon TEXT NOT NULL, userDefined INTEGER NOT NULL)")
            db.execSQL("INSERT INTO accounts VALUES('kept','Preserved')")
        }))
        v1.writableDatabase
        v1.close()
        val v3 = factory.create(config(context, 3, onUpgrade = { db ->
            DatabaseMigrations.MIGRATION_1_2.migrate(db)
            DatabaseMigrations.MIGRATION_2_3.migrate(db)
        }))
        val db = v3.writableDatabase
        db.query("SELECT name FROM accounts WHERE id='kept'").use { assertTrue(it.moveToFirst()); assertEquals("Preserved", it.getString(0)) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='investment_instruments'").use { assertTrue(it.moveToFirst()) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='receipt_attachments'").use { assertTrue(it.moveToFirst()) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='credit_card_statements'").use { assertTrue(it.moveToFirst()) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='policy_documents'").use { assertTrue(it.moveToFirst()) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='planning_assumptions'").use { assertTrue(it.moveToFirst()) }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='income_breakdowns'").use { assertTrue(it.moveToFirst()) }
        db.query("PRAGMA table_info('categories')").use { cursor -> var found = false; while(cursor.moveToNext()) if(cursor.getString(1)=="parentCategoryId") found=true; assertTrue(found) }
        db.query("PRAGMA table_info('tax_profiles')").use { cursor -> var found = false; while(cursor.moveToNext()) if(cursor.getString(1)=="shortTermCapitalGain111AMinor") found=true; assertTrue(found) }
        db.query("PRAGMA table_info('deposits')").use { cursor -> var lockIn = false; while(cursor.moveToNext()) if(cursor.getString(1)=="lockInEndEpochDay") lockIn=true; assertTrue(lockIn) }
        db.query("PRAGMA index_list('imported_records')").use { cursor ->
            var uniqueFingerprint = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "index_imported_records_fingerprint" && cursor.getInt(2) == 1) uniqueFingerprint = true
            assertTrue(uniqueFingerprint)
        }
        v3.close()
        context.deleteDatabase("ledgerx-migration-test.db")
    }

    private fun config(context: Context, version: Int, onCreate: (SupportSQLiteDatabase) -> Unit = {}, onUpgrade: (SupportSQLiteDatabase) -> Unit = {}): SupportSQLiteOpenHelper.Configuration {
        val createBlock = onCreate
        val upgradeBlock = onUpgrade
        return SupportSQLiteOpenHelper.Configuration.builder(context).name("ledgerx-migration-test.db").callback(object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) = createBlock(db)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = upgradeBlock(db)
        }).build()
    }
}
