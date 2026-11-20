package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import com.example.ui.theme.AppThemeOption
import java.io.File
import java.io.FileOutputStream

enum class InvoicePdfStyle(val label: String) {
    THERMAL_COUPON("Thermal Receipt"),
    MINIMAL_A4("Minimal PDF")
}

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 points at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val THERMAL_WIDTH = 226 // 80mm thermal receipt roll (~226pt)

    private fun loadBusinessLogo(business: BusinessEntity): Bitmap? {
        val customPath = business.logoFilePath
        if (!customPath.isNullOrBlank()) {
            val file = File(customPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
            }
        }
        return null
    }

    fun generateInvoicePdf(
        context: Context,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems,
        style: InvoicePdfStyle = InvoicePdfStyle.THERMAL_COUPON
    ): File = when (style) {
        InvoicePdfStyle.THERMAL_COUPON -> generateThermalReceiptPdf(context, business, invoiceWithItems)
        InvoicePdfStyle.MINIMAL_A4 -> generateMinimalA4Pdf(context, business, invoiceWithItems)
    }

    /**
     * 80mm Thermal Printer Coupon Receipt with crisp monospace styling, dashed dividers,
     * and a serrated / zig-zag torn-coupon bottom edge.
     */
    fun generateThermalReceiptPdf(
        context: Context,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val currency = business.resolvedCurrency()
        val pad = 12f
        val contentWidth = THERMAL_WIDTH - (pad * 2f)

        // Compute dynamic thermal roll height based on content
        var estimatedHeight = 160f + (items.size * 24f)
        if (business.tagline.isNotBlank()) estimatedHeight += 11f
        if (business.address.isNotBlank()) estimatedHeight += 11f
        if (business.phone.isNotBlank()) estimatedHeight += 11f
        if (business.taxId.isNotBlank()) estimatedHeight += 11f
        if (invoice.customerPhone.isNotBlank()) estimatedHeight += 11f
        if (invoice.performedByName.isNotBlank()) estimatedHeight += 11f
        if (invoice.discountAmount > 0.0) estimatedHeight += 12f
        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) estimatedHeight += 12f
        if (invoice.balanceDue > 0.01 && !invoice.isReturned) estimatedHeight += 14f
        if (invoice.notes.isNotBlank()) estimatedHeight += 16f
        val pageHeight = estimatedHeight.coerceAtLeast(280f).toInt()

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_WIDTH, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Soft paper background around the bottom serrated teeth so teeth visibly stand out
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(0f, 0f, THERMAL_WIDTH.toFloat(), pageHeight.toFloat(), bgPaint)

        // Draw white coupon paper with serrated zig-zag bottom edge
        val toothW = 8f
        val toothH = 6f
        val paperBottom = pageHeight - 10f
        val paperPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(THERMAL_WIDTH.toFloat(), 0f)
            lineTo(THERMAL_WIDTH.toFloat(), paperBottom - toothH)
            var x = THERMAL_WIDTH.toFloat()
            while (x > 0f) {
                val midX = (x - toothW / 2f).coerceAtLeast(0f)
                val nextX = (x - toothW).coerceAtLeast(0f)
                lineTo(midX, paperBottom)
                lineTo(nextX, paperBottom - toothH)
                x = nextX
            }
            close()
        }
        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawPath(paperPath, paperPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        canvas.drawPath(paperPath, borderPaint)

        val ink = Color.rgb(15, 23, 42)
        val muted = Color.rgb(71, 85, 105)
        val monoTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 11.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val monoCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        val monoBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val monoBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 7.8f
            typeface = Typeface.MONOSPACE
        }
        val monoMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 7.2f
            typeface = Typeface.MONOSPACE
        }
        val monoRight = Paint(monoBody).apply { textAlign = Paint.Align.RIGHT }
        val monoBoldRight = Paint(monoBold).apply { textAlign = Paint.Align.RIGHT }
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(3f, 2.5f), 0f)
        }

        fun drawDash(yPos: Float) {
            canvas.drawLine(pad, yPos, THERMAL_WIDTH - pad, yPos, dashPaint)
        }

        val cx = THERMAL_WIDTH / 2f
        val rightX = THERMAL_WIDTH - pad
        var y = 18f

        // Header
        canvas.drawText(business.name.uppercase().take(26), cx, y, monoTitle)
        y += 11f
        if (business.tagline.isNotBlank()) {
            canvas.drawText(business.tagline.take(34), cx, y, monoCenter)
            y += 10f
        }
        if (business.address.isNotBlank()) {
            canvas.drawText(business.address.take(36), cx, y, monoCenter)
            y += 10f
        }
        if (business.phone.isNotBlank()) {
            canvas.drawText("Tel: ${business.phone.take(26)}", cx, y, monoCenter)
            y += 10f
        }
        if (business.taxId.isNotBlank()) {
            canvas.drawText("Tax ID: ${business.taxId.take(24)}", cx, y, monoCenter)
            y += 10f
        }

        y += 2f
        drawDash(y)
        y += 11f

        // Meta rows
        canvas.drawText("Receipt #:", pad, y, monoMuted)
        canvas.drawText(invoice.invoiceNumber, rightX, y, monoBoldRight)
        y += 10f

        canvas.drawText("Date:", pad, y, monoMuted)
        canvas.drawText(Formatters.formatDateTime(invoice.createdAt), rightX, y, monoRight)
        y += 10f

        canvas.drawText("Customer:", pad, y, monoMuted)
        canvas.drawText(invoice.customerName.ifBlank { "Walk-in" }.take(20), rightX, y, monoRight)
        y += 10f

        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText("Phone:", pad, y, monoMuted)
            canvas.drawText(invoice.customerPhone.take(20), rightX, y, monoRight)
            y += 10f
        }

        if (invoice.performedByName.isNotBlank()) {
            canvas.drawText("Served By:", pad, y, monoMuted)
            canvas.drawText(invoice.performedByName.take(20), rightX, y, monoBoldRight)
            y += 10f
        }

        y += 2f
        drawDash(y)
        y += 10f

        // Column header
        canvas.drawText("ITEM / QTY x RATE", pad, y, monoBold)
        canvas.drawText("AMT", rightX, y, monoBoldRight)
        y += 5f
        drawDash(y)
        y += 11f

        // Line items
        items.forEach { item ->
            val label = buildString {
                append(item.productName)
                if (item.packSize.isNotBlank()) append(" (${item.packSize})")
            }.take(30)
            canvas.drawText(label, pad, y, monoBold)
            y += 10f
            val qtyRate = "  ${item.quantity} x ${Formatters.formatDecimal(item.unitPrice)}"
            canvas.drawText(qtyRate, pad, y, monoMuted)
            canvas.drawText(Formatters.formatDecimal(item.lineTotal), rightX, y, monoRight)
            y += 12f
        }

        drawDash(y - 3f)
        y += 9f

        // Totals
        canvas.drawText("Subtotal", pad, y, monoBody)
        canvas.drawText(Formatters.formatDecimal(invoice.subtotal), rightX, y, monoRight)
        y += 11f

        if (invoice.discountAmount > 0.0) {
            canvas.drawText("Discount", pad, y, monoBody)
            canvas.drawText("-${Formatters.formatDecimal(invoice.discountAmount)}", rightX, y, monoRight)
            y += 11f
        }

        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) {
            canvas.drawText("Tax (${invoice.taxRatePercent}%)", pad, y, monoBody)
            canvas.drawText(Formatters.formatDecimal(invoice.taxAmount), rightX, y, monoRight)
            y += 11f
        }

        drawDash(y - 3f)
        y += 11f

        val grandPaint = Paint(monoBold).apply { textSize = 10f }
        val grandRight = Paint(grandPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL", pad, y, grandPaint)
        canvas.drawText(Formatters.formatPkr(invoice.grandTotal, currency), rightX, y, grandRight)
        y += 12f

        canvas.drawText("Paid (${invoice.paymentMethod.take(12)})", pad, y, monoMuted)
        canvas.drawText(Formatters.formatPkr(invoice.amountPaid, currency), rightX, y, monoRight)
        y += 11f

        if (invoice.balanceDue > 0.01 && !invoice.isReturned) {
            val duePaint = Paint(monoBoldRight).apply { color = Color.rgb(185, 28, 28) }
            canvas.drawText("BALANCE DUE", pad, y, monoBold)
            canvas.drawText(Formatters.formatPkr(invoice.balanceDue, currency), rightX, y, duePaint)
            y += 12f
        }

        if (invoice.notes.isNotBlank()) {
            canvas.drawText("Note: ${invoice.notes.take(30)}", pad, y, monoMuted)
            y += 11f
        }

        drawDash(y - 2f)
        y += 11f

        val statusTag = InvoicePaymentStatus.fromName(invoice.paymentStatus).label.uppercase()
        canvas.drawText(
            business.invoiceFooter.ifBlank { "Thank you for your business!" }.take(34),
            cx,
            y,
            monoCenter
        )
        y += 11f
        canvas.drawText("*** $statusTag ***", cx, y, Paint(monoCenter).apply {
            color = ink
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })

        doc.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNumber = invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "Receipt_${safeNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /**
     * Ultra-minimal, compact colorful A4 PDF invoice:
     * slender 4pt top accent bar, crisp refined typography, hairline 0.5pt dividers, zero bulky blocks.
     */
    fun generateMinimalA4Pdf(
        context: Context,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val currency = business.resolvedCurrency()
        val theme = AppThemeOption.fromKey(business.themeKey)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val accentColor = theme.primaryAndroidColorInt()
        val softBg = theme.softAndroidColorInt()
        val darkInk = Color.rgb(30, 41, 59)
        val mutedInk = Color.rgb(100, 116, 139)
        val hairlineColor = Color.rgb(226, 232, 240)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 14.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 8.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 7.4f
        }
        val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightMutedPaint = Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT }
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hairlineColor
            strokeWidth = 0.5f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sleek 4pt top theme accent strip
        fillPaint.color = accentColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 4f, fillPaint)

        var y = 26f
        val logoBitmap = loadBusinessLogo(business)
        val textStartX = if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(MARGIN, y, MARGIN + 38f, y + 38f), null)
            MARGIN + 48f
        } else {
            MARGIN
        }

        // Compact Left Business Header
        canvas.drawText(business.name, textStartX, y + 12f, titlePaint)
        var leftY = y + 23f
        if (business.tagline.isNotBlank()) {
            canvas.drawText(business.tagline, textStartX, leftY, mutedPaint)
            leftY += 10f
        }
        val contactBits = listOf(business.address.take(42), business.phone, business.taxId.takeIf { it.isNotBlank() }?.let { "Tax: $it" })
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        if (contactBits.isNotBlank()) {
            canvas.drawText(contactBits, textStartX, leftY, mutedPaint)
        }

        // Compact Right Invoice Meta
        canvas.drawText(invoice.invoiceNumber, PAGE_WIDTH - MARGIN, y + 12f, Paint(titlePaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 12f
            color = darkInk
        })
        canvas.drawText(Formatters.formatDateTime(invoice.createdAt), PAGE_WIDTH - MARGIN, y + 23f, rightMutedPaint)
        if (invoice.performedByName.isNotBlank()) {
            canvas.drawText(
                "By: ${invoice.performedByName}",
                PAGE_WIDTH - MARGIN,
                y + 34f,
                Paint(rightMutedPaint).apply { color = accentColor }
            )
        }

        y += 48f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, hairlinePaint)
        y += 14f

        // Compact Billed To & Payment Meta Row (no heavy tinted box)
        val tier = PricingTier.fromName(invoice.pricingTier)
        val statusLabel = InvoicePaymentStatus.fromName(invoice.paymentStatus).label
        canvas.drawText("BILLED TO", MARGIN, y, Paint(mutedPaint).apply {
            color = accentColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        })
        canvas.drawText("PAYMENT", PAGE_WIDTH - MARGIN, y, Paint(rightMutedPaint).apply {
            color = accentColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        })
        y += 12f
        val custTitle = buildString {
            append(invoice.customerName.ifBlank { "Walk-in Customer" })
            if (invoice.customerPhone.isNotBlank()) append("  •  ${invoice.customerPhone}")
        }
        canvas.drawText(custTitle, MARGIN, y, boldPaint)
        canvas.drawText(
            "${business.tierLabel(tier)}  •  ${invoice.paymentMethod}  •  $statusLabel",
            PAGE_WIDTH - MARGIN,
            y,
            rightBodyPaint
        )

        y += 16f

        // Clean Minimal Table Header (soft tint, thin bottom rule — no heavy dark block)
        fillPaint.color = softBg
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 19f), 3f, 3f, fillPaint)
        val thPaint = Paint(mutedPaint).apply {
            color = accentColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.6f
        }
        val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("#", MARGIN + 6f, y + 12.5f, thPaint)
        canvas.drawText("ITEM", MARGIN + 24f, y + 12.5f, thPaint)
        canvas.drawText("QTY", PAGE_WIDTH - MARGIN - 140f, y + 12.5f, thRight)
        canvas.drawText("RATE", PAGE_WIDTH - MARGIN - 72f, y + 12.5f, thRight)
        canvas.drawText("AMOUNT", PAGE_WIDTH - MARGIN - 6f, y + 12.5f, thRight)
        y += 24f

        // Compact Table Rows with hairline separators
        items.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 140f) return@forEachIndexed
            canvas.drawText("${idx + 1}", MARGIN + 6f, y + 4f, mutedPaint)
            val title = buildString {
                append(item.productName)
                if (item.packSize.isNotBlank()) append(" (${item.packSize})")
                if (business.trackBatch && item.batchNumber.isNotBlank()) append(" • ${item.batchNumber}")
            }.take(48)
            canvas.drawText(title, MARGIN + 24f, y + 4f, bodyPaint)
            canvas.drawText("${item.quantity}", PAGE_WIDTH - MARGIN - 140f, y + 4f, rightBodyPaint)
            canvas.drawText(Formatters.formatDecimal(item.unitPrice), PAGE_WIDTH - MARGIN - 72f, y + 4f, rightBodyPaint)
            canvas.drawText(Formatters.formatDecimal(item.lineTotal), PAGE_WIDTH - MARGIN - 6f, y + 4f, rightBoldPaint)
            y += 18f
            canvas.drawLine(MARGIN, y - 5f, PAGE_WIDTH - MARGIN, y - 5f, hairlinePaint)
        }

        y += 10f

        // Compact Totals Block (Right aligned, clean hairline accent)
        val boxLeft = PAGE_WIDTH - MARGIN - 190f
        canvas.drawText("Subtotal", boxLeft, y, mutedPaint)
        canvas.drawText(Formatters.formatPkr(invoice.subtotal, currency), PAGE_WIDTH - MARGIN - 6f, y, rightBodyPaint)
        y += 13f

        if (invoice.discountAmount > 0.0) {
            canvas.drawText("Discount", boxLeft, y, mutedPaint)
            canvas.drawText("-${Formatters.formatPkr(invoice.discountAmount, currency)}", PAGE_WIDTH - MARGIN - 6f, y, rightBodyPaint)
            y += 13f
        }

        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) {
            canvas.drawText("Tax (${invoice.taxRatePercent}%)", boxLeft, y, mutedPaint)
            canvas.drawText(Formatters.formatPkr(invoice.taxAmount, currency), PAGE_WIDTH - MARGIN - 6f, y, rightBodyPaint)
            y += 13f
        }

        y += 2f
        canvas.drawLine(boxLeft, y - 6f, PAGE_WIDTH - MARGIN, y - 6f, Paint(hairlinePaint).apply { color = accentColor; strokeWidth = 0.8f })
        val grandLabelPaint = Paint(boldPaint).apply { color = accentColor; textSize = 10.5f }
        val grandValPaint = Paint(grandLabelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Grand Total", boxLeft, y + 7f, grandLabelPaint)
        canvas.drawText(Formatters.formatPkr(invoice.grandTotal, currency), PAGE_WIDTH - MARGIN - 6f, y + 7f, grandValPaint)

        y += 20f
        canvas.drawText("Paid", boxLeft, y, mutedPaint)
        canvas.drawText(Formatters.formatPkr(invoice.amountPaid, currency), PAGE_WIDTH - MARGIN - 6f, y, rightBodyPaint)
        if (invoice.balanceDue > 0.01 && !invoice.isReturned) {
            y += 13f
            val duePaint = Paint(rightBoldPaint).apply { color = Color.rgb(185, 28, 28) }
            canvas.drawText("Balance Due", boxLeft, y, mutedPaint)
            canvas.drawText(Formatters.formatPkr(invoice.balanceDue, currency), PAGE_WIDTH - MARGIN - 6f, y, duePaint)
        }

        if (invoice.notes.isNotBlank()) {
            y += 20f
            canvas.drawText("Notes: ${invoice.notes.take(90)}", MARGIN, y, mutedPaint)
        }

        val footerY = PAGE_HEIGHT - 32f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, hairlinePaint)
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
        val file = File(dir, "Invoice_Minimal_${safeNumber}.pdf")
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
        val currency = business.resolvedCurrency()
        val theme = AppThemeOption.fromKey(business.themeKey)
        val accentColor = theme.primaryAndroidColorInt()
        val softBg = theme.softAndroidColorInt()

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val darkInk = Color.rgb(30, 41, 59)
        val mutedInk = Color.rgb(100, 116, 139)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 8.6f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 7.6f
        }
        val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

        fillPaint.color = accentColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, fillPaint)

        var y = 30f
        val logoBitmap = loadBusinessLogo(business)
        val titleX = if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, Rect(MARGIN.toInt(), y.toInt(), (MARGIN + 40).toInt(), (y + 40).toInt()), null)
            MARGIN + 52f
        } else {
            MARGIN
        }

        canvas.drawText(scopeTitle, titleX, y + 14f, titlePaint)
        canvas.drawText("Monthly Financial & Inventory Report — $monthLabel", titleX, y + 28f, boldPaint)
        canvas.drawText("Generated: ${Formatters.formatDateTime(System.currentTimeMillis())}", titleX, y + 40f, mutedPaint)

        y += 60f
        val rows = mutableListOf(
            "Gross Sales ($invoiceCount invoices)" to Formatters.formatPkr(grossSales, currency),
            "Total Business Expenses" to Formatters.formatPkr(totalExpenses, currency),
            "Net Profit / Loss" to Formatters.formatPkr(netProfit, currency),
            "Tax Collected" to Formatters.formatPkr(taxCollected, currency),
            "Current Stock Cost Value" to Formatters.formatPkr(stockValuation, currency)
        )
        if (business.trackExpiry) {
            rows.add("Expiry Alerts" to "$expiringCount expiring soon, $expiredCount expired")
        }

        fillPaint.color = softBg
        val boxHeight = rows.size * 16f + 16f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight), 6f, 6f, fillPaint)

        var ky = y + 18f
        rows.forEach { (k, v) ->
            canvas.drawText(k, MARGIN + 12f, ky, bodyPaint)
            canvas.drawText(v, PAGE_WIDTH - MARGIN - 12f, ky, rightBold)
            ky += 16f
        }

        y += boxHeight + 22f
        canvas.drawText("EXPENSE BREAKDOWN BY CATEGORY", MARGIN, y, Paint(boldPaint).apply { color = accentColor })
        y += 16f

        val grouped = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        if (grouped.isEmpty()) {
            canvas.drawText("No expenses recorded in $monthLabel.", MARGIN, y, mutedPaint)
            y += 18f
        } else {
            grouped.forEach { (cat, sum) ->
                canvas.drawText(cat, MARGIN + 6f, y, bodyPaint)
                canvas.drawText(Formatters.formatPkr(sum, currency), PAGE_WIDTH - MARGIN - 6f, y, rightBold)
                y += 16f
            }
        }

        y += 14f
        canvas.drawText("RECENT EXPENSES ($monthLabel)", MARGIN, y, Paint(boldPaint).apply { color = accentColor })
        y += 16f
        expenses.take(20).forEach { exp ->
            if (y > PAGE_HEIGHT - 45f) return@forEach
            val bySuffix = if (exp.performedByName.isNotBlank()) " • By ${exp.performedByName}" else ""
            canvas.drawText(
                "${Formatters.formatDate(exp.expenseDateMillis)}  •  ${exp.title.take(32)} (${exp.category})$bySuffix",
                MARGIN + 6f,
                y,
                bodyPaint
            )
            canvas.drawText(Formatters.formatPkr(exp.amount, currency), PAGE_WIDTH - MARGIN - 6f, y, rightBold)
            y += 15f
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
