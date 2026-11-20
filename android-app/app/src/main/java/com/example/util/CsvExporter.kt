package com.example.util

import android.content.Context
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import java.io.File

object CsvExporter {
    fun buildInventoryCsv(products: List<ProductEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Brand,Category,PackSize,Barcode,BatchNumber,ExpiryDate,ExpiryStatus,StockQty,LowStockThreshold,PurchasePrice,MRP,DistributorPrice,WholesalerPrice,RetailPrice,PerformedBy")
        products.forEach { p ->
            sb.appendLine(
                listOf(
                    p.name,
                    p.brand,
                    p.category,
                    p.packSize,
                    p.barcode,
                    p.batchNumber,
                    Formatters.formatIsoDate(p.expiryDateMillis),
                    Formatters.expiryStatus(p.expiryDateMillis).label,
                    p.stockQty.toString(),
                    p.lowStockThreshold.toString(),
                    p.purchasePrice.toString(),
                    p.mrpPrice.toString(),
                    p.distributorPrice.toString(),
                    p.wholesalerPrice.toString(),
                    p.retailPrice.toString(),
                    p.performedByName
                ).joinToString(",") { Formatters.csvEscape(it) }
            )
        }
        return sb.toString()
    }

    fun buildExpensesCsv(expenses: List<ExpenseEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Category,Amount,PaymentMethod,Payee,PerformedBy,Notes")
        expenses.forEach { e ->
            sb.appendLine(
                listOf(
                    Formatters.formatIsoDate(e.expenseDateMillis),
                    e.title,
                    e.category,
                    e.amount.toString(),
                    e.paymentMethod,
                    e.payee,
                    e.performedByName,
                    e.notes
                ).joinToString(",") { Formatters.csvEscape(it) }
            )
        }
        return sb.toString()
    }

    fun buildSalesCsv(invoices: List<InvoiceWithItems>): String {
        val sb = StringBuilder()
        sb.appendLine("InvoiceNumber,Date,CustomerName,CustomerPhone,PricingTier,ItemCount,Subtotal,Discount,TaxRatePercent,TaxAmount,GrandTotal,PaymentMethod,PaymentStatus,AmountPaid,BalanceDue,PerformedBy")
        invoices.forEach { entry ->
            val inv = entry.invoice
            sb.appendLine(
                listOf(
                    inv.invoiceNumber,
                    Formatters.formatIsoDate(inv.createdAt),
                    inv.customerName,
                    inv.customerPhone,
                    inv.pricingTier,
                    entry.items.sumOf { it.quantity }.toString(),
                    inv.subtotal.toString(),
                    inv.discountAmount.toString(),
                    if (inv.taxEnabled) inv.taxRatePercent.toString() else "0",
                    inv.taxAmount.toString(),
                    inv.grandTotal.toString(),
                    inv.paymentMethod,
                    inv.paymentStatus,
                    inv.amountPaid.toString(),
                    inv.balanceDue.toString(),
                    inv.performedByName
                ).joinToString(",") { Formatters.csvEscape(it) }
            )
        }
        return sb.toString()
    }

    fun writeCsvToCache(context: Context, fileName: String, content: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }
}
