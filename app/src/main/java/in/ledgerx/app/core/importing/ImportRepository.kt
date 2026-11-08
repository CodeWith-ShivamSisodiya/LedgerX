package com.ledgerx.app.core.importing

import androidx.room.withTransaction
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.database.ImportBatchEntity
import com.ledgerx.app.core.database.ImportedRecordEntity
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.PlanningDao
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class StatementColumnMapping(val date: String, val description: String, val amount: String)
data class ImportPreviewRow(val rowNumber: Int, val date: LocalDate?, val description: String, val signedAmountMinor: Long?, val error: String?)
data class ImportPreview(val headers: List<String>, val rows: List<ImportPreviewRow>) {
    val isValid: Boolean get() = rows.isNotEmpty() && rows.none { it.error != null }
}

object StatementPreviewer {
    fun preview(csv: String, mapping: StatementColumnMapping): ImportPreview {
        val table = CsvParser.parse(csv)
        val dateIndex = table.headers.indexOf(mapping.date)
        val descriptionIndex = table.headers.indexOf(mapping.description)
        val amountIndex = table.headers.indexOf(mapping.amount)
        require(dateIndex >= 0 && descriptionIndex >= 0 && amountIndex >= 0) { "Mapped columns were not found" }
        val rows = table.rows.mapIndexed { index, values ->
            val date = runCatching { LocalDate.parse(values[dateIndex]) }.getOrElse {
                runCatching { LocalDate.of(1899, 12, 30).plusDays(values[dateIndex].toBigDecimal().toLong()) }.getOrNull()
            }
            val amount = runCatching { values[amountIndex].replace(",", "").toBigDecimal().movePointRight(2).longValueExact() }.getOrNull()
            val error = when {
                date == null -> "Date must use YYYY-MM-DD or an Excel date serial"
                amount == null -> "Amount is invalid or has more than two decimals"
                amount == 0L -> "Amount cannot be zero"
                amount == Long.MIN_VALUE -> "Amount is outside the supported range"
                else -> null
            }
            ImportPreviewRow(index + 2, date, values[descriptionIndex].trim(), amount, error)
        }
        return ImportPreview(table.headers, rows)
    }
}

@Singleton
class ImportRepository @Inject constructor(
    private val database: LedgerXDatabase,
    private val dao: PlanningDao,
    private val ledger: LedgerRepository,
) {
    fun preview(csv: String, mapping: StatementColumnMapping): ImportPreview {
        return StatementPreviewer.preview(csv, mapping)
    }

    suspend fun commit(
        accountId: String,
        fileName: String,
        csv: String,
        mapping: StatementColumnMapping,
        expenseCategoryId: String,
        incomeCategoryId: String,
    ): String {
        val preview = preview(csv, mapping)
        require(preview.isValid) { "Resolve all preview errors before importing" }
        val sourceHash = sha256(csv.toByteArray())
        val fingerprints = preview.rows.map { ImportFingerprint.create(accountId, requireNotNull(it.date).toString(), requireNotNull(it.signedAmountMinor), it.description) }
        val existing = if (fingerprints.isEmpty()) emptySet() else dao.existingFingerprints(fingerprints).toSet()
        val batchId = UUID.randomUUID().toString()
        return database.withTransaction {
            val imported = mutableListOf<ImportedRecordEntity>()
            val seen = existing.toMutableSet()
            preview.rows.forEachIndexed { index, row ->
                val fingerprint = fingerprints[index]
                if (seen.add(fingerprint)) {
                    val epochMillis = requireNotNull(row.date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val amount = requireNotNull(row.signedAmountMinor)
                    val transactionId = ledger.recordImported(accountId, amount, if (amount < 0) expenseCategoryId else incomeCategoryId, row.description, epochMillis)
                    imported += ImportedRecordEntity(UUID.randomUUID().toString(), batchId, fingerprint, row.rowNumber, transactionId, "IMPORTED", null)
                }
            }
            dao.saveImportBatch(ImportBatchEntity(batchId, accountId, fileName, sourceHash, preview.rows.size, imported.size, preview.rows.size - imported.size, "COMPLETED", System.currentTimeMillis()))
            if (imported.isNotEmpty()) dao.saveImportedRecords(imported)
            batchId
        }
    }

    suspend fun rollback(batchId: String) = database.withTransaction {
        val batch = requireNotNull(dao.importBatch(batchId)) { "Import batch not found" }
        require(batch.status == "COMPLETED") { "Only a completed import can be rolled back" }
        val records = dao.importedRecords(batchId)
        records.mapNotNull { it.transactionId }.forEach { ledger.reverseTransaction(it) }
        require(dao.updateImportBatchStatus(batchId, "ROLLED_BACK") == 1) { "Import batch changed during rollback" }
        dao.updateImportedRecordStatus(batchId, "ROLLED_BACK")
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
