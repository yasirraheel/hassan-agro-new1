package com.example

import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import com.example.util.ProfitLossCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfitLossCalculatorTest {

    @Test
    fun calculatesTrueGrossProfitNetProfitAndSeparatesStockPurchases() {
        val now = 1_750_000_000_000L
        val prodA = ProductEntity(
            id = "p1",
            businessId = "b1",
            name = "Widget A",
            purchasePrice = 100.0,
            retailPrice = 160.0,
            stockQty = 10,
            expiryDateMillis = 0L
        )

        val invoice1 = InvoiceWithItems(
            invoice = InvoiceEntity(
                id = "inv1",
                businessId = "b1",
                invoiceNumber = "INV-1",
                customerName = "Ali",
                subtotal = 800.0,
                grandTotal = 800.0,
                amountPaid = 800.0,
                balanceDue = 0.0,
                isReturned = false,
                createdAt = now
            ),
            items = listOf(
                InvoiceItemEntity(
                    invoiceId = "inv1",
                    productId = "p1",
                    productName = "Widget A",
                    quantity = 5,
                    unitPrice = 160.0,
                    costPrice = 100.0,
                    lineTotal = 800.0
                )
            )
        )

        // Returned invoice must be excluded from sales & COGS
        val returnedInvoice = InvoiceWithItems(
            invoice = InvoiceEntity(
                id = "inv2",
                businessId = "b1",
                invoiceNumber = "INV-2",
                customerName = "Bilal",
                subtotal = 320.0,
                grandTotal = 320.0,
                isReturned = true,
                createdAt = now
            ),
            items = listOf(
                InvoiceItemEntity(
                    invoiceId = "inv2",
                    productId = "p1",
                    productName = "Widget A",
                    quantity = 2,
                    unitPrice = 160.0,
                    costPrice = 100.0,
                    lineTotal = 320.0
                )
            )
        )

        // Operating expense (Rent = 120) vs Stock Purchase (1000) — Stock Purchase must NOT double-count against COGS!
        val expenses = listOf(
            ExpenseEntity(
                id = "e1",
                businessId = "b1",
                title = "Monthly Shop Rent",
                category = "Shop Rent",
                amount = 120.0,
                expenseDateMillis = now
            ),
            ExpenseEntity(
                id = "e2",
                businessId = "b1",
                title = "Initial Stock Purchase",
                category = "Stock Purchase",
                amount = 1000.0,
                expenseDateMillis = now
            )
        )

        val summary = ProfitLossCalculator.calculate(
            invoices = listOf(invoice1, returnedInvoice),
            expenses = expenses,
            products = listOf(prodA),
            startMillis = now - 10_000L,
            endMillis = now + 10_000L
        )

        assertEquals(800.0, summary.totalSales, 0.01)
        assertEquals(500.0, summary.costOfGoodsSold, 0.01)
        assertEquals(300.0, summary.grossProfit, 0.01)
        assertEquals(37.5, summary.grossMarginPercent, 0.01)
        assertEquals(120.0, summary.operatingExpenses, 0.01)
        assertEquals(1000.0, summary.stockPurchaseSpend, 0.01)
        assertEquals(180.0, summary.netProfit, 0.01)
        assertEquals(1000.0, summary.inventoryCostValuation, 0.01)
        assertEquals(1600.0, summary.inventoryRetailValuation, 0.01)
        assertEquals(1, summary.invoiceCount)
    }

    @Test
    fun fallsBackToProductPurchasePriceForLegacyInvoiceItemsWithZeroCostPrice() {
        val now = 1_750_000_000_000L
        val prodA = ProductEntity(
            id = "p1",
            businessId = "b1",
            name = "Legacy Item",
            purchasePrice = 50.0,
            retailPrice = 80.0,
            stockQty = 4,
            expiryDateMillis = 0L
        )
        val legacyInv = InvoiceWithItems(
            invoice = InvoiceEntity(
                id = "inv1",
                businessId = "b1",
                invoiceNumber = "INV-OLD",
                customerName = "Walk-in",
                subtotal = 240.0,
                grandTotal = 240.0,
                createdAt = now
            ),
            items = listOf(
                InvoiceItemEntity(
                    invoiceId = "inv1",
                    productId = "p1",
                    productName = "Legacy Item",
                    quantity = 3,
                    unitPrice = 80.0,
                    costPrice = 0.0, // Legacy row before DB v5
                    lineTotal = 240.0
                )
            )
        )

        val summary = ProfitLossCalculator.calculate(
            invoices = listOf(legacyInv),
            expenses = emptyList(),
            products = listOf(prodA),
            startMillis = 0L,
            endMillis = Long.MAX_VALUE
        )

        assertEquals(240.0, summary.totalSales, 0.01)
        assertEquals(150.0, summary.costOfGoodsSold, 0.01)
        assertEquals(90.0, summary.grossProfit, 0.01)
        assertEquals(90.0, summary.netProfit, 0.01)
    }
}
