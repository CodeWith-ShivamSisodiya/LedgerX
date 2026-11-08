package com.ledgerx.app.core.database

import android.content.Context
import android.os.SystemClock
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
class LargeLedgerPerformanceTest {
    @Test fun indexedActivityPageRemainsResponsiveAtOneHundredThousandTransactions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "ledgerx-large-ledger-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE transactions(id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, merchant TEXT, note TEXT, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX index_transactions_occurredAtEpochMillis ON transactions(occurredAtEpochMillis)")
                    db.execSQL("CREATE INDEX index_transactions_type ON transactions(type)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        )
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val statement = db.compileStatement("INSERT INTO transactions VALUES(?,?,?,?,?,?)")
            repeat(100_000) { index ->
                statement.clearBindings()
                statement.bindString(1, "tx-$index")
                statement.bindString(2, if (index % 3 == 0) "INCOME" else "EXPENSE")
                statement.bindString(3, "merchant-${index % 500}")
                statement.bindString(4, "offline performance fixture")
                statement.bindLong(5, 1_700_000_000_000L + index)
                statement.bindLong(6, 1_700_000_000_000L + index)
                statement.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        val started = SystemClock.elapsedRealtime()
        val ids = db.query("SELECT id FROM transactions WHERE type = 'EXPENSE' AND (merchant LIKE '%merchant-42%' OR note LIKE '%merchant-42%') ORDER BY occurredAtEpochMillis DESC LIMIT 50").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        val elapsed = SystemClock.elapsedRealtime() - started
        assertEquals(50, ids.size)
        assertTrue("Paged query took ${elapsed}ms", elapsed < 5_000)
        helper.close()
        context.deleteDatabase(name)
    }
}
