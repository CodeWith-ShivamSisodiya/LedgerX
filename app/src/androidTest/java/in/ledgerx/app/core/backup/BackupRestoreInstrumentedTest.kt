package com.ledgerx.app.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ledgerx.app.core.database.AccountEntity
import com.ledgerx.app.core.database.LedgerXDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentedTest {
    @Test fun portableBackupRestoresLogicalDatabaseAcrossFreshState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val dao = database.ledgerDao()
        val repository = BackupRepository(context, database, database.planningDao())
        val now = 1_800_000_000_000L
        dao.insertAccount(AccountEntity("preserved", "Preserved account", "SAVINGS", "INR", false, now))
        val file = File(context.cacheDir, "portable-backup-test.lxb")
        val password = "correct horse battery staple".toCharArray()
        repository.create(Uri.fromFile(file), password)
        dao.insertAccount(AccountEntity("after-backup", "Must disappear", "CASH", "INR", false, now + 1))

        val result = repository.restore(Uri.fromFile(file), password)

        assertEquals("Preserved account", dao.account("preserved")?.name)
        assertNull(dao.account("after-backup"))
        assertTrue(result.recordCount >= 1)
        password.fill('\u0000')
        database.close()
        file.delete()
    }
}
