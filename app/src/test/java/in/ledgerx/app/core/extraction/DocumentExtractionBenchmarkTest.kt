package com.ledgerx.app.core.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Synthetic layouts only, not real institution-issued statements.
 * A passing test establishes regression behavior, not real-world extraction accuracy.
 */
class DocumentExtractionBenchmarkTest {
    private val engine = SemanticLabelEngine()

    @Test fun standardFdReceipt() {
        val result=engine.extract("""
            HDFC BANK
            Fixed Deposit Receipt
            Deposit Amount: INR 50,000.00
            Interest Rate: 7.25 %
            Opening Date: 01/11/2026
            Maturity Date: 01/11/2027
            Maturity Amount: INR 53,625.00
        """.trimIndent())
        assertEquals("Fixed Deposit",result.category)
        assertEquals("50000.00",result.principal?.value)
        assertEquals("53625.00",result.maturityAmount?.value)
        assertEquals("7.25",result.rate?.value)
        assertNotNull(result.principal?.evidence)
    }

    @Test fun alternateTermDepositLabels() {
        val result=engine.extract("""
            STATE BANK OF INDIA
            Term Deposit Advice
            Amount Invested - Rs. 100000
            ROI: 6.80%
            Date of Deposit: 12-10-2026
            Due Date: 12-10-2027
            Maturity Proceeds: ₹ 106950
        """.trimIndent())
        assertEquals("Fixed Deposit",result.category)
        assertEquals("100000",result.principal?.value)
        assertEquals("106950",result.maturityAmount?.value)
        assertEquals("12-10-2027",result.maturityDate?.value)
    }

    @Test fun missingFieldsAreNeverInvented() {
        val result=engine.extract("ICICI BANK\\nTerm Deposit\\nDeposit Amount: INR 25000")
        assertEquals("25000",result.principal?.value)
        assertEquals(null,result.maturityAmount)
        assertEquals(null,result.rate)
    }

    @Test fun recurringDepositClassification() {
        val result=engine.extract("SBI\\nRecurring Deposit\\nMonthly installment: 5000\\nMaturity Date: 01/01/2029")
        assertEquals("Recurring Deposit",result.category)
    }

    @Test fun rejectsUnrelatedAmounts() {
        val result=engine.extract("HDFC BANK\\nStatement generated on 09/11/2026\\nCustomer ID: 123456")
        assertEquals(null,result.principal)
        assertEquals(null,result.maturityAmount)
    }
}
