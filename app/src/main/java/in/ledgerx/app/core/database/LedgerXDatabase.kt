package in.ledgerx.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        LedgerPostingEntity::class,
        AuditLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LedgerXDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
}

