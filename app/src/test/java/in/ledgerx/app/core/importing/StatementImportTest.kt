package com.ledgerx.app.core.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.Assert.assertTrue
import java.time.LocalDate

class StatementImportTest {
    @Test
    fun quotedCommaAndEscapedQuoteAreParsed() {
        val table = CsvParser.parse("date,description,amount\n2026-01-01,\"Cafe, Mumbai\",120.50\n2026-01-02,\"A \"\"quoted\"\" item\",10")
        assertEquals("Cafe, Mumbai", table.rows[0][1])
        assertEquals("A \"quoted\" item", table.rows[1][1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedQuoteIsRejected() {
        CsvParser.parse("date,description\n2026-01-01,\"unfinished")
    }

    @Test
    fun duplicateFingerprintIsStableButAccountSpecific() {
        val first = ImportFingerprint.create("a", "2026-01-01", -100, "Cafe")
        assertEquals(first, ImportFingerprint.create("a", "2026-01-01", -100, " cafe "))
        assertNotEquals(first, ImportFingerprint.create("b", "2026-01-01", -100, "Cafe"))
    }

    @Test fun previewAcceptsIsoAndExcelDatesButRejectsSubPaiseValues() {
        val mapping = StatementColumnMapping("date", "description", "amount")
        val valid = StatementPreviewer.preview("date,description,amount\n2026-11-07,Salary,100.00\n46333,Cafe,-20.50", mapping)
        assertTrue(valid.isValid)
        assertEquals(LocalDate.of(2026, 11, 7), valid.rows[0].date)
        val invalid = StatementPreviewer.preview("date,description,amount\n2026-11-07,Cafe,1.001", mapping)
        assertTrue(!invalid.isValid)
    }
}
