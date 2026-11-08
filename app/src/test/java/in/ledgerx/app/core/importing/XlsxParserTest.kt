package com.ledgerx.app.core.importing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxParserTest {
    @Test fun readsSharedStringsAndSparseCellsFromFirstWorksheet() {
        val shared = """<sst><si><t>Date</t></si><si><t>Description</t></si><si><t>Amount</t></si><si><t>Groceries</t></si></sst>"""
        val sheet = """<worksheet><sheetData><row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c></row><row r="2"><c r="A2" t="inlineStr"><is><t>2026-11-07</t></is></c><c r="B2" t="s"><v>3</v></c><c r="C2"><v>-123.45</v></c></row></sheetData></worksheet>"""
        val bytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("xl/sharedStrings.xml")); zip.write(shared.toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml")); zip.write(sheet.toByteArray()); zip.closeEntry()
            }
            output.toByteArray()
        }
        val table = XlsxParser.parse(bytes)
        assertEquals(listOf("Date", "Description", "Amount"), table.headers)
        assertEquals(listOf("2026-11-07", "Groceries", "-123.45"), table.rows.single())
    }
}
