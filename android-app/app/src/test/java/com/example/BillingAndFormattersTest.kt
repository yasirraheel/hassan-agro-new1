package com.example

import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.ProductEntity
import com.example.util.Formatters
import com.example.viewmodel.BillingCartLine
import com.example.viewmodel.BillingDraftState
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingAndFormattersTest {

    private fun sampleDraft(amountReceivedText: String, isCreditSale: Boolean = false): BillingDraftState {
        val product = ProductEntity(
            id = "p1",
            businessId = "b1",
            name = "Sample Item",
            brand = "Agro",
            category = "General",
            packSize = "1L",
            barcode = "",
            batchNumber = "",
            expiryDateMillis = 0L,
            stockQty = 10,
            lowStockThreshold = 2,
            purchasePrice = 800.0,
            mrpPrice = 1200.0,
            distributorPrice = 1000.0,
            wholesalerPrice = 1050.0,
            retailPrice = 1100.0
        )
        return BillingDraftState(
            cartLines = listOf(BillingCartLine(product = product, quantity = 2, unitPrice = 1000.0)),
            amountReceivedText = amountReceivedText,
            isCreditSale = isCreditSale
        )
    }

    @Test
    fun blankCashReceivedDefaultsToCreditNotPaid() {
        val draft = sampleDraft(amountReceivedText = "")
        assertEquals(2000.0, draft.grandTotal, 0.01)
        assertEquals(0.0, draft.tenderedAmount, 0.01)
        assertEquals(0.0, draft.appliedPaidAmount, 0.01)
        assertEquals(2000.0, draft.balanceDue, 0.01)
        assertEquals(InvoicePaymentStatus.CREDIT, draft.computedPaymentStatus)
    }

    @Test
    fun zeroCashReceivedIsCredit() {
        val draft = sampleDraft(amountReceivedText = "0")
        assertEquals(0.0, draft.appliedPaidAmount, 0.01)
        assertEquals(2000.0, draft.balanceDue, 0.01)
        assertEquals(InvoicePaymentStatus.CREDIT, draft.computedPaymentStatus)
    }

    @Test
    fun partialCashReceivedIsPartial() {
        val draft = sampleDraft(amountReceivedText = "750")
        assertEquals(750.0, draft.appliedPaidAmount, 0.01)
        assertEquals(1250.0, draft.balanceDue, 0.01)
        assertEquals(InvoicePaymentStatus.PARTIAL, draft.computedPaymentStatus)
    }

    @Test
    fun fullCashReceivedIsPaid() {
        val draft = sampleDraft(amountReceivedText = "2000")
        assertEquals(2000.0, draft.appliedPaidAmount, 0.01)
        assertEquals(0.0, draft.balanceDue, 0.01)
        assertEquals(InvoicePaymentStatus.PAID, draft.computedPaymentStatus)
    }

    @Test
    fun formatPkrOneDecimalRoundsToOnePlace() {
        assertEquals("Rs. 1,250.5", Formatters.formatPkrOneDecimal(1250.46))
        assertEquals("Rs. 500.0", Formatters.formatPkrOneDecimal(500.0))
    }

    @Test
    fun formatStockCompactPkrShowsTwoDecimalsForMillions() {
        assertEquals("Rs. 1.25M", Formatters.formatStockCompactPkr(1_250_000.0))
        assertEquals("Rs. 142.50K", Formatters.formatStockCompactPkr(142_500.0))
    }

    @Test
    fun formatShortPkrUsesReadableKAndMNotation() {
        assertEquals("Rs. 2.45M", Formatters.formatShortPkr(2_450_000.0))
        assertEquals("Rs. 15.5K", Formatters.formatShortPkr(15_500.0))
        assertEquals("Rs. 12K", Formatters.formatShortPkr(12_000.0))
    }
}
