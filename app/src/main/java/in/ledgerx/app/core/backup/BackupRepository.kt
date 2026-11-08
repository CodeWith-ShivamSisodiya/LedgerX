package com.ledgerx.app.core.backup

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ledgerx.app.core.database.BackupMetadataEntity
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.PlanningDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class BackupResult(val recordCount: Long, val checksumSha256: String)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LedgerXDatabase,
    private val planningDao: PlanningDao,
) {
    private val clock = Clock.systemUTC()

    suspend fun create(uri: Uri, password: CharArray): BackupResult = withContext(Dispatchers.IO) {
        validatePassword(password)
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAtEpochMillis", clock.millis())
        val tablesJson = JSONObject()
        var records = 0L
        database.openHelper.readableDatabase.let { db ->
            TABLES.forEach { table ->
                val rows = JSONArray()
                db.query("SELECT * FROM `$table`").use { cursor ->
                    while (cursor.moveToNext()) {
                        rows.put(cursor.toJson())
                        records++
                    }
                }
                tablesJson.put(table, rows)
            }
        }
        root.put("tables", tablesJson).put("recordCount", records)
        val plain = root.toString().toByteArray(Charsets.UTF_8)
        val checksum = sha256(plain)
        val encrypted = PortableBackupCrypto.encrypt(plain, password)
        requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "Cannot open backup destination" }.use {
            it.write(encrypted)
            it.flush()
        }
        planningDao.saveBackupMetadata(
            BackupMetadataEntity(UUID.randomUUID().toString(), VERSION, uri.lastPathSegment ?: "ledgerx-backup.lxb", checksum, clock.millis(), clock.millis(), records)
        )
        BackupResult(records, checksum)
    }

    suspend fun restore(uri: Uri, password: CharArray): BackupResult = withContext(Dispatchers.IO) {
        validatePassword(password)
        val encrypted = requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open backup" }.use { it.readBytes() }
        val plain = PortableBackupCrypto.decrypt(encrypted, password)
        val root = runCatching { JSONObject(plain.toString(Charsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Backup content is invalid", it) }
        val backupVersion = root.optInt("version")
        require(root.optString("format") == FORMAT && backupVersion in 1..VERSION) { "Unsupported LedgerX backup version" }
        val tablesJson = root.getJSONObject("tables")
        val includedTables = when (backupVersion) { 1 -> BASE_TABLES; 2 -> V2_TABLES; else -> TABLES }
        var records = 0L
        includedTables.forEach { table ->
            require(tablesJson.has(table)) { "Backup is incomplete: $table is missing" }
            records += tablesJson.getJSONArray(table).length()
        }
        require(records == root.getLong("recordCount")) { "Backup record count failed verification" }

        database.withTransaction {
            val db = database.openHelper.writableDatabase
            TABLES.asReversed().forEach { db.execSQL("DELETE FROM `$it`") }
            includedTables.forEach { table ->
                val rows = tablesJson.getJSONArray(table)
                repeat(rows.length()) { index ->
                    val row = rows.getJSONObject(index)
                    val names = row.keys().asSequence().toList()
                    if (names.isNotEmpty()) {
                        val sql = "INSERT INTO `$table` (${names.joinToString { "`$it`" }}) VALUES (${names.joinToString { "?" }})"
                        val args = names.map { name -> row.toSqlArg(name) }.toTypedArray()
                        db.execSQL(sql, args)
                    }
                }
            }
        }
        val checksum = sha256(plain)
        planningDao.saveBackupMetadata(BackupMetadataEntity(UUID.randomUUID().toString(), VERSION, uri.lastPathSegment ?: "restored-backup.lxb", checksum, root.getLong("createdAtEpochMillis"), clock.millis(), records))
        BackupResult(records, checksum)
    }

    private fun Cursor.toJson(): JSONObject = JSONObject().also { row ->
        columnNames.forEachIndexed { index, name ->
            when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(name, getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> row.put(name, getDouble(index))
                Cursor.FIELD_TYPE_BLOB -> row.put(name, BLOB_PREFIX + Base64.encodeToString(getBlob(index), Base64.NO_WRAP))
                else -> row.put(name, getString(index))
            }
        }
    }

    private fun JSONObject.toSqlArg(name: String): Any? {
        if (isNull(name)) return null
        val value = get(name)
        return if (value is String && value.startsWith(BLOB_PREFIX)) Base64.decode(value.removePrefix(BLOB_PREFIX), Base64.NO_WRAP) else value
    }

    private fun validatePassword(password: CharArray) = require(password.size >= 12) { "Backup password must contain at least 12 characters" }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val FORMAT = "LedgerX portable encrypted backup"
        private const val VERSION = 3
        private const val BLOB_PREFIX = "base64:"
        private val BASE_TABLES = listOf(
            "accounts", "categories", "transactions", "ledger_postings", "audit_log",
            "recurring_templates", "scheduled_occurrences", "bills", "import_batches", "imported_records", "reconciliations",
            "investment_instruments", "investment_transactions", "valuation_snapshots", "deposits", "retirement_contributions",
            "physical_assets", "asset_valuations", "loans", "loan_events", "insurance_policies", "goals", "goal_allocations",
            "tax_profiles", "tax_rules", "tax_payments", "health_snapshots", "financial_insights", "forecast_results",
            "notification_schedules", "backup_metadata",
        )
        private val V2_TABLES = BASE_TABLES + listOf(
            "local_profiles", "merchants", "tags", "transaction_tags", "receipt_attachments",
            "investment_lots", "dividend_records", "sip_schedules", "deposit_contributions",
            "retirement_events", "asset_maintenance", "loan_schedule", "credit_card_statements",
            "dashboard_widgets",
        )
        private val TABLES = V2_TABLES + listOf("bill_price_history", "policy_documents", "planning_assumptions", "income_breakdowns")
    }
}
