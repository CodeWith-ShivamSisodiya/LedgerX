package com.ledgerx.app.core.importing

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object XlsxParser {
    fun parse(bytes: ByteArray): CsvTable {
        val entries = mutableMapOf<String, ByteArray>()
        var expandedTotal = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name.startsWith("xl/worksheets/sheet"))) {
                    val content = readLimited(zip, 25 * 1024 * 1024)
                    expandedTotal += content.size
                    require(expandedTotal <= 30 * 1024 * 1024) { "XLSX expands beyond the 30 MB safety limit" }
                    entries[entry.name] = content
                }
                entry = zip.nextEntry
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let(::sharedStrings).orEmpty()
        val sheetBytes = entries.entries.filter { it.key.startsWith("xl/worksheets/sheet") }.minByOrNull { it.key }?.value
            ?: error("XLSX contains no worksheet")
        val document = document(sheetBytes)
        val rowNodes = document.getElementsByTagName("row")
        val rows = buildList {
            for (rowIndex in 0 until rowNodes.length) {
                val cells = (rowNodes.item(rowIndex) as Element).getElementsByTagName("c")
                val values = sortedMapOf<Int, String>()
                for (cellIndex in 0 until cells.length) {
                    val cell = cells.item(cellIndex) as Element
                    val column = columnIndex(cell.getAttribute("r"))
                    val type = cell.getAttribute("t")
                    val raw = if (type == "inlineStr") cell.getElementsByTagName("t").item(0)?.textContent.orEmpty()
                        else cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
                    values[column] = if (type == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw
                }
                if (values.isNotEmpty()) add((0..values.lastKey()).map { values[it].orEmpty().trim() })
            }
        }
        require(rows.isNotEmpty()) { "XLSX worksheet is empty" }
        val headers = rows.first().map(String::trim)
        require(headers.none(String::isEmpty) && headers.distinct().size == headers.size) { "XLSX headers must be non-empty and unique" }
        return CsvTable(headers, rows.drop(1).map { row -> headers.indices.map { row.getOrElse(it) { "" } } })
    }

    private fun sharedStrings(bytes: ByteArray): List<String> {
        val nodes = document(bytes).getElementsByTagName("si")
        return buildList {
            for (index in 0 until nodes.length) {
                val textNodes = (nodes.item(index) as Element).getElementsByTagName("t")
                add(buildString { for (textIndex in 0 until textNodes.length) append(textNodes.item(textIndex).textContent) })
            }
        }
    }

    private fun document(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun columnIndex(reference: String): Int {
        val letters = reference.takeWhile(Char::isLetter).uppercase()
        require(letters.isNotEmpty()) { "XLSX cell reference is invalid" }
        return letters.fold(0) { value, char -> value * 26 + (char - 'A' + 1) } - 1
    }

    private fun readLimited(zip: ZipInputStream, limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "XLSX worksheet expands beyond the 25 MB safety limit" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
