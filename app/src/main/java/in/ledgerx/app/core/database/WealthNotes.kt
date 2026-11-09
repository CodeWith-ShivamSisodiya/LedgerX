package com.ledgerx.app.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Notes belong to an existing holding. SQLCipher encrypts the Room database at rest.
 * holdingType is one of ACCOUNT, INVESTMENT, DEPOSIT, ASSET, LOAN, POLICY.
 */
@Entity(tableName = "wealth_notes")
data class WealthNoteEntity(
    @PrimaryKey val id: String,
    val holdingType: String,
    val holdingId: String,
    val title: String,
    val body: String,
    val updatedAtEpochMillis: Long,
)

@Dao
interface WealthNotesDao {
    @Query("SELECT * FROM wealth_notes ORDER BY updatedAtEpochMillis DESC")
    fun observeNotes(): Flow<List<WealthNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: WealthNoteEntity)

    @Query("DELETE FROM wealth_notes WHERE id = :id")
    suspend fun delete(id: String)
}
