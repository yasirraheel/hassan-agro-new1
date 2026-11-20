package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import java.io.File
import java.io.FileOutputStream

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 points at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    private fun loadBusinessLogo(context: Context, business: BusinessEntity): Bitmap? {
        val customPath = business.logoFilePath
        if (!customPath.isNullOrBlank()) {
            val file = File(customPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
            }
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.img_hassan_agro_crest_1795112944140)
    }

    fun generateInvoicePdf(
        context: Context,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val greenColor = Color.rgb(27, 107, 58)
        val darkInk = Color.rgb(19, 32, 24)
        val mutedInk = Color.rgb(85, 102, 91)
        val lightGreenBg = Color.rgb(235, 246, 238)
        val lineGray = Color.rgb(212, 225, 216)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 8.5f
        }
        val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineGray
            strokeWidth = 1f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top accent bar
        fillPaint.color = greenColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, fillPaint)

        var y = 34f

        // Brand Logo + Business Header
        val logoBitmap = loadBusinessLogo(context, business)
        val textStartX = if (logoBitmap != null) {
            val dest = RectF(MARGIN, y, MARGIN + 56f, y + 56f)
            canvas.drawBitmap(logoBitmap, null, dest, null)
            MARGIN + 68f
        } else {
            MARGIN
        }

        canvas.drawText(business.name, textStartX, y + 18f, titlePaint)
        if (business.tagline.isNotBlank()) {
            canvas.drawText(business.tagline, textStartX, y + 32f, mutedPaint)
        }
        val contactLine = listOf(business.phone, business.email).filter { it.isNotBlank() }.joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, textStartX, y + 44f, mutedPaint)
        }
        if (business.address.isNotBlank()) {
            canvas.drawText(business.address.take(54), textStartX, y + 56f, mutedPaint)
        }
        if (business.taxId.isNotBlank()) {
            canvas.drawText("NTN / STRN: ${business.taxId}", textStartX, y + 68f, mutedPaint)
        }

        // Invoice Badge on Right
        val invLabelPaint = Paint(titlePaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 16f
        }
        canvas.drawText("SALES INVOICE", PAGE_WIDTH - MARGIN, y + 18f, invLabelPaint)
        canvas.drawText("#${invoice.invoiceNumber}", PAGE_WIDTH - MARGIN, y + 34f, rightBoldPaint)
        canvas.drawText(Formatters.formatDateTime(invoice.createdAt), PAGE_WIDTH - MARGIN, y + 48f, Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT })
        val tier = PricingTier.fromName(invoice.pricingTier)
        canvas.drawText("Tier: ${tier.label}", PAGE_WIDTH - MARGIN, y + 61f, rightBoldPaint)

        y += 86f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 18f

        // Bill To Box
        fillPaint.color = lightGreenBg
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 54f), 8f, 8f, fillPaint)
        canvas.drawText("BILL TO", MARGIN + 12f, y + 16f, Paint(mutedPaint).apply {
            color = greenColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        })
        canvas.drawText(invoice.customerName.ifBlank { "Walk-in Customer" }, MARGIN + 12f, y + 32f, boldPaint)
        val custContact = listOf(invoice.customerPhone, invoice.customerEmail).filter { it.isNotBlank() }.joinToString("  |  ")
        if (custContact.isNotBlank()) {
            canvas.drawText(custContact, MARGIN + 12f, y + 46f, mutedPaint)
        }
        y += 72f

        // Table Header
        fillPaint.color = greenColor
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f), 4f, 4f, fillPaint)
        val headerWhite = Paint(boldPaint).apply { color = Color.WHITE; textSize = 9.5f }
        val headerWhiteRight = Paint(headerWhite).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("#", MARGIN + 8f, y + 16f, headerWhite)
        canvas.drawText("Product & Batch", MARGIN + 28f, y + 16f, headerWhite)
        canvas.drawText("Qty", PAGE_WIDTH - MARGIN - 165f, y + 16f, headerWhiteRight)
        canvas.drawText("Unit Rate", PAGE_WIDTH - MARGIN - 85f, y + 16f, headerWhiteRight)
        canvas.drawText("Total", PAGE_WIDTH - MARGIN - 8f, y + 16f, headerWhiteRight)
        y += 30f

        // Table Rows
        items.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 175f) return@forEachIndexed
            if (idx % 2 == 1) {
                fillPaint.color = Color.rgb(248, 251, 249)
                canvas.drawRect(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 20f, fillPaint)
            }
            canvas.drawText("${idx + 1}", MARGIN + 8f, y + 4f, bodyPaint)
            val title = buildString {
                append(item.productName)
                if (item.packSize.isNotBlank()) append(" (${item.packSize})")
            }.take(38)
            canvas.drawText(title, MARGIN + 28f, y + 4f, boldPaint)
            if (item.batchNumber.isNotBlank()) {
                canvas.drawText("Batch: ${item.batchNumber}", MARGIN + 28f, y + 15f, mutedPaint)
            }
            canvas.drawText("${item.quantity}", PAGE_WIDTH - MARGIN - 165f, y + 4f, rightBodyPaint)
            canvas.drawText(Formatters.formatPkr(item.unitPrice), PAGE_WIDTH - MARGIN - 85f, y + 4f, rightBodyPaint)
            canvas.drawText(Formatters.formatPkr(item.lineTotal), PAGE_WIDTH - MARGIN - 8f, y + 4f, rightBoldPaint)
            y += 28f
            canvas.drawLine(MARGIN, y - 6f, PAGE_WIDTH - MARGIN, y - 6f, dividerPaint)
        }

        y += 14f

        // Summary Totals Box (Right aligned)
        val boxLeft = PAGE_WIDTH - MARGIN - 220f
        canvas.drawText("Subtotal", boxLeft, y, bodyPaint)
        canvas.drawText(Formatters.formatPkr(invoice.subtotal), PAGE_WIDTH - MARGIN - 8f, y, rightBodyPaint)
        y += 16f

        if (invoice.discountAmount > 0.0) {
            canvas.drawText("Discount", boxLeft, y, bodyPaint)
            canvas.drawText("-${Formatters.formatPkr(invoice.discountAmount)}", PAGE_WIDTH - MARGIN - 8f, y, rightBodyPaint)
            y += 16f
        }

        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) {
            canvas.drawText("Tax (${invoice.taxRatePercent}%)", boxLeft, y, bodyPaint)
            canvas.drawText(Formatters.formatPkr(invoice.taxAmount), PAGE_WIDTH - MARGIN - 8f, y, rightBodyPaint)
            y += 16f
        }

        y += 4f
        fillPaint.color = greenColor
        canvas.drawRoundRect(RectF(boxLeft - 10f, y - 14f, PAGE_WIDTH - MARGIN, y + 14f), 6f, 6f, fillPaint)
        val totalWhite = Paint(boldPaint).apply { color = Color.WHITE; textSize = 12f }
        val totalWhiteRight = Paint(totalWhite).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("GRAND TOTAL", boxLeft, y + 4f, totalWhite)
        canvas.drawText(Formatters.formatPkr(invoice.grandTotal), PAGE_WIDTH - MARGIN - 8f, y + 4f, totalWhiteRight)

        y += 28f
        val statusLabel = com.example.data.local.InvoicePaymentStatus.fromName(invoice.paymentStatus).label
        canvas.drawText("Payment: ${invoice.paymentMethod} ($statusLabel)", boxLeft, y, bodyPaint)
        canvas.drawText("Paid: ${Formatters.formatPkr(invoice.amountPaid)}", PAGE_WIDTH - MARGIN - 8f, y, rightBodyPaint)
        if (invoice.balanceDue > 0.01 && !invoice.isReturned) {
            y += 15f
            val duePaint = Paint(rightBoldPaint).apply { color = Color.rgb(186, 26, 26) }
            canvas.drawText("Balance Due (Credit)", boxLeft, y, bodyPaint)
            canvas.drawText(Formatters.formatPkr(invoice.balanceDue), PAGE_WIDTH - MARGIN - 8f, y, duePaint)
        }

        // Notes & Footer
        if (invoice.notes.isNotBlank()) {
            y += 26f
            canvas.drawText("Notes: ${invoice.notes.take(80)}", MARGIN, y, mutedPaint)
        }

        val footerY = PAGE_HEIGHT - 44f
        canvas.drawLine(MARGIN, footerY - 14f, PAGE_WIDTH - MARGIN, footerY - 14f, dividerPaint)
        val centerMuted = Paint(mutedPaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(
            business.invoiceFooter.ifBlank { "Thank you for choosing ${business.name}." },
            PAGE_WIDTH / 2f,
            footerY,
            centerMuted
        )

        doc.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNumber = invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "Invoice_${safeNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun generateMonthlyReportPdf(
        context: Context,
        business: BusinessEntity,
        scopeTitle: String,
        monthLabel: String,
        grossSales: Double,
        totalExpenses: Double,
        netProfit: Double,
        taxCollected: Double,
        stockValuation: Double,
        invoiceCount: Int,
        expiringCount: Int,
        expiredCount: Int,
        expenses: List<ExpenseEntity>
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val greenColor = Color.rgb(27, 107, 58)
        val darkInk = Color.rgb(19, 32, 24)
        val mutedInk = Color.rgb(85, 102, 91)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 9.5f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 8.5f
        }
        val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

        fillPaint.color = greenColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, fillPaint)

        var y = 40f
        val logoBitmap = loadBusinessLogo(context, business)
        val titleX = if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, Rect(MARGIN.toInt(), y.toInt(), (MARGIN + 46).toInt(), (y + 46).toInt()), null)
            MARGIN + 58f
        } else {
            MARGIN
        }

        canvas.drawText(scopeTitle, titleX, y + 18f, titlePaint)
        canvas.drawText("Monthly Financial & Inventory Report — $monthLabel", titleX, y + 34f, boldPaint)
        canvas.drawText("Generated: ${Formatters.formatDateTime(System.currentTimeMillis())}", titleX, y + 48f, mutedPaint)

        y += 74f
        fillPaint.color = Color.rgb(235, 246, 238)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 116f), 8f, 8f, fillPaint)

        val rows = listOf(
            "Gross Sales ($invoiceCount invoices)" to Formatters.formatPkr(grossSales),
            "Total Business Expenses" to Formatters.formatPkr(totalExpenses),
            "Net Profit / Loss" to Formatters.formatPkr(netProfit),
            "Tax Collected (GST)" to Formatters.formatPkr(taxCollected),
            "Current Stock Cost Value" to Formatters.formatPkr(stockValuation),
            "Expiry Alerts" to "$expiringCount expiring soon, $expiredCount expired"
        )
        var ky = y + 20f
        rows.forEach { (k, v) ->
            canvas.drawText(k, MARGIN + 14f, ky, bodyPaint)
            canvas.drawText(v, PAGE_WIDTH - MARGIN - 14f, ky, rightBold)
            ky += 17f
        }

        y += 140f
        canvas.drawText("EXPENSE BREAKDOWN BY CATEGORY", MARGIN, y, Paint(boldPaint).apply { color = greenColor })
        y += 18f

        val grouped = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        if (grouped.isEmpty()) {
            canvas.drawText("No expenses recorded in $monthLabel.", MARGIN, y, mutedPaint)
            y += 22f
        } else {
            grouped.forEach { (cat, sum) ->
                canvas.drawText(cat, MARGIN + 8f, y, bodyPaint)
                canvas.drawText(Formatters.formatPkr(sum), PAGE_WIDTH - MARGIN - 8f, y, rightBold)
                y += 18f
            }
        }

        y += 16f
        canvas.drawText("RECENT EXPENSES ($monthLabel)", MARGIN, y, Paint(boldPaint).apply { color = greenColor })
        y += 18f
        expenses.take(18).forEach { exp ->
            if (y > PAGE_HEIGHT - 50f) return@forEach
            canvas.drawText(
                "${Formatters.formatDate(exp.expenseDateMillis)}  •  ${exp.title.take(36)} (${exp.category})",
                MARGIN + 8f,
                y,
                bodyPaint
            )
            canvas.drawText(Formatters.formatPkr(exp.amount), PAGE_WIDTH - MARGIN - 8f, y, rightBold)
            y += 16f
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeMonth = monthLabel.replace(" ", "_")
        val file = File(dir, "Monthly_Report_${safeMonth}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
