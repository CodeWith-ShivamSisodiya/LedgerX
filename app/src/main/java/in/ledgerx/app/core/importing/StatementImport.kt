package com.ledgerx.app.core.importing

import java.security.MessageDigest
import java.util.Locale

data class CsvTable(val headers: List<String>, val rows: List<List<String>>)

object CsvParser {
    fun parse(text: String): CsvTable {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> { cell.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> { row += cell.toString(); cell.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += cell.toString(); cell.clear()
                    if (row.any(String::isNotEmpty)) rows += row
                    row = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }
        require(!quoted) { "Unclosed quoted field" }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        require(rows.isNotEmpty()) { "CSV is empty" }
        val headers = rows.first().map(String::trim)
        require(headers.none(String::isEmpty) && headers.distinct().size == headers.size) { "Headers must be non-empty and unique" }
        val body = rows.drop(1).map { values -> headers.indices.map { values.getOrElse(it) { "" }.trim() } }
        return CsvTable(headers, body)
    }

    fun encode(table: CsvTable): String = (listOf(table.headers) + table.rows).joinToString("\n") { row ->
        row.joinToString(",") { value -> "\"${value.replace("\"", "\"\"")}\"" }
    }
}

object ImportFingerprint {
    fun create(accountId: String, dateIso: String, amountMinor: Long, description: String): String {
        val canonical = listOf(accountId.trim(), dateIso.trim(), amountMinor.toString(), description.trim().lowercase(Locale.ROOT)).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
