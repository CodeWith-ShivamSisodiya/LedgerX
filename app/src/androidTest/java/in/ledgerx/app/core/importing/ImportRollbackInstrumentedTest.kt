package com.ledgerx.app.core.importing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.model.Money
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportRollbackInstrumentedTest {
    @Test fun rollbackCreatesReversalsAndRestoresAccountBalance() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val database=Room.inMemoryDatabaseBuilder(context,LedgerXDatabase::class.java).build()
        val ledger=LedgerRepository(database,database.ledgerDao())
        ledger.initialize()
        val account=ledger.createAccount("Bank","SAVINGS",Money(0))
        val importer=ImportRepository(database,database.planningDao(),ledger)
        val csv="date,description,amount\n2026-01-01,Salary,1000.00\n2026-01-02,Shop,-250.00"
        val batch=importer.commit(account,"statement.csv",csv,StatementColumnMapping("date","description","amount"),"shopping","salary")
        assertEquals(750_00L,database.ledgerDao().accountBalance(account))
        importer.rollback(batch)
        assertEquals(0L,database.ledgerDao().accountBalance(account))
        assertEquals("ROLLED_BACK",database.planningDao().importBatch(batch)?.status)
        database.close()
    }
}
