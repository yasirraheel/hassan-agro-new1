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

    private fun computeThermalPageHeight(
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ): Int {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        var estimatedHeight = 166f + (items.size * 24f)
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
        return estimatedHeight.coerceAtLeast(280f).toInt()
    }

    /**
     * Generates a crisp high-resolution PNG image of the serrated thermal receipt
     * so it renders directly inline on WhatsApp without needing a PDF reader.
     */
    fun generateThermalReceiptImage(
        context: Context,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ): File {
        val pageHeight = computeThermalPageHeight(business, invoiceWithItems)
        val scale = 3.5f
        val bitmapWidth = (THERMAL_WIDTH * scale).toInt()
        val bitmapHeight = (pageHeight * scale).toInt()
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        drawThermalReceiptOnCanvas(canvas, pageHeight, business, invoiceWithItems)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNumber = invoiceWithItems.invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "Receipt_${safeNumber}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
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
        val pageHeight = computeThermalPageHeight(business, invoiceWithItems)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_WIDTH, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)

        drawThermalReceiptOnCanvas(page.canvas, pageHeight, business, invoiceWithItems)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNumber = invoiceWithItems.invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "Receipt_${safeNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawThermalReceiptOnCanvas(
        canvas: Canvas,
        pageHeight: Int,
        business: BusinessEntity,
        invoiceWithItems: InvoiceWithItems
    ) {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val currency = business.resolvedCurrency()
        val pad = 12f

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
    }

    /**
     * Minimal, Compact, Modern Invoice PDF:
     * Content-fitted A5-width sheet (420pt wide, dynamic height, zero wasted blank half-page),
     * crisp readable 9.5 to 15.5pt typography, rounded status pill, subtle alternating rows,
     * and clean highlight box for Grand Total.
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

        val compactWidth = 420
        val pad = 22f
        val contentRight = compactWidth - pad

        val accentColor = theme.primaryAndroidColorInt()
        val softAccentBg = theme.softAndroidColorInt()
        val darkInk = Color.rgb(15, 23, 42)
        val bodyInk = Color.rgb(30, 41, 59)
        val mutedInk = Color.rgb(100, 116, 139)
        val surfaceTint = Color.rgb(248, 250, 252)
        val borderColor = Color.rgb(226, 232, 240)
        val strongBorder = Color.rgb(203, 213, 225)

        // Pre-calculate exact item row heights so the page fits tightly around its content
        val rowHeights = items.map { item ->
            val hasSub = item.packSize.isNotBlank() || (business.trackBatch && item.batchNumber.isNotBlank())
            if (hasSub) 29f else 22f
        }
        val itemsBlockHeight = rowHeights.sum()

        var dynamicHeight = 148f + itemsBlockHeight + 92f
        if (business.tagline.isNotBlank()) dynamicHeight += 11f
        if (business.address.isNotBlank() || business.phone.isNotBlank() || business.taxId.isNotBlank()) dynamicHeight += 11f
        if (invoice.discountAmount > 0.0) dynamicHeight += 15f
        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) dynamicHeight += 15f
        if (invoice.balanceDue > 0.01 && !invoice.isReturned) dynamicHeight += 16f
        if (invoice.notes.isNotBlank()) dynamicHeight += 20f
        val pageHeight = dynamicHeight.coerceIn(300f, 1400f).toInt()

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(compactWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 0.65f
        }

        // White clean page canvas
        fillPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, compactWidth.toFloat(), pageHeight.toFloat(), fillPaint)

        // Slender 3.5pt top brand accent bar
        fillPaint.color = accentColor
        canvas.drawRect(0f, 0f, compactWidth.toFloat(), 3.5f, fillPaint)

        val bizTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 15.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val invNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 12.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bodyInk
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 7.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightMutedPaint = Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT }

        // --- 1. Header ---
        var y = 22f
        val logoBitmap = loadBusinessLogo(business)
        val leftX = if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(pad, y - 2f, pad + 32f, y + 30f), null)
            pad + 40f
        } else {
            pad
        }

        canvas.drawText(business.name.take(28), leftX, y + 11f, bizTitlePaint)
        var leftBottom = y + 22f
        if (business.tagline.isNotBlank()) {
            canvas.drawText(business.tagline.take(40), leftX, leftBottom, mutedPaint)
            leftBottom += 10.5f
        }
        val contactLine = listOf(
            business.phone,
            business.address.take(32),
            business.taxId.takeIf { it.isNotBlank() }?.let { "Tax: $it" }
        ).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine.take(54), leftX, leftBottom, mutedPaint)
            leftBottom += 10.5f
        }

        // Right header: Invoice # + Date + Status Pill
        canvas.drawText(invoice.invoiceNumber, contentRight, y + 10f, invNumPaint)
        canvas.drawText(Formatters.formatDateTime(invoice.createdAt), contentRight, y + 21f, rightMutedPaint)

        val statusEnum = InvoicePaymentStatus.fromName(invoice.paymentStatus)
        val statusText = if (invoice.isReturned) "RETURNED" else statusEnum.label.uppercase()
        val (pillBg, pillFg) = when {
            invoice.isReturned -> Color.rgb(226, 232, 240) to Color.rgb(71, 85, 105)
            statusEnum == InvoicePaymentStatus.PAID -> Color.rgb(220, 252, 231) to Color.rgb(21, 128, 61)
            statusEnum == InvoicePaymentStatus.PARTIAL -> Color.rgb(254, 243, 199) to Color.rgb(180, 83, 9)
            else -> Color.rgb(254, 226, 226) to Color.rgb(185, 28, 28)
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillFg
            textSize = 7.6f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillW = (pillTextPaint.measureText(statusText) + 16f).coerceAtLeast(48f)
        val pillRect = RectF(contentRight - pillW, y + 26f, contentRight, y + 40f)
        fillPaint.color = pillBg
        canvas.drawRoundRect(pillRect, 7f, 7f, fillPaint)
        canvas.drawText(statusText, pillRect.centerX(), pillRect.centerY() + 2.7f, pillTextPaint)

        y = maxOf(leftBottom + 6f, y + 48f)

        // --- 2. Compact Customer & Payment Card ---
        val metaRect = RectF(pad, y, contentRight, y + 36f)
        fillPaint.color = surfaceTint
        canvas.drawRoundRect(metaRect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, strokePaint)

        canvas.drawText("CUSTOMER", pad + 10f, y + 13f, labelPaint)
        val custLine = buildString {
            append(invoice.customerName.ifBlank { "Walk-in Customer" }.take(24))
            if (invoice.customerPhone.isNotBlank()) append(" · ${invoice.customerPhone.take(16)}")
        }
        canvas.drawText(custLine, pad + 10f, y + 26.5f, boldPaint)

        val rightLabelPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("PAYMENT & STAFF", contentRight - 10f, y + 13f, rightLabelPaint)
        val tier = PricingTier.fromName(invoice.pricingTier)
        val payStaffLine = buildString {
            append(invoice.paymentMethod.take(14))
            append(" · ")
            append(business.tierLabel(tier).take(10))
            if (invoice.performedByName.isNotBlank()) append(" · ${invoice.performedByName.take(14)}")
        }
        canvas.drawText(payStaffLine, contentRight - 10f, y + 26.5f, rightBodyPaint)

        y += 46f

        // --- 3. Clean Item Table ---
        val colQtyX = contentRight - 118f
        val colRateX = contentRight - 64f
        val colAmtX = contentRight - 6f

        val thRect = RectF(pad, y, contentRight, y + 20f)
        fillPaint.color = softAccentBg
        canvas.drawRoundRect(thRect, 4f, 4f, fillPaint)

        val thPaint = Paint(labelPaint).apply {
            color = accentColor
            textSize = 7.8f
        }
        val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("#", pad + 7f, y + 13f, thPaint)
        canvas.drawText("ITEM", pad + 22f, y + 13f, thPaint)
        canvas.drawText("QTY", colQtyX, y + 13f, thRight)
        canvas.drawText("RATE", colRateX, y + 13f, thRight)
        canvas.drawText("AMOUNT", colAmtX, y + 13f, thRight)

        y += 22f

        items.forEachIndexed { idx, item ->
            val rH = rowHeights[idx]
            if (idx % 2 == 1) {
                fillPaint.color = surfaceTint
                canvas.drawRect(pad, y, contentRight, y + rH, fillPaint)
            }

            val baseline = y + 13.5f
            canvas.drawText("${idx + 1}", pad + 7f, baseline, mutedPaint)
            canvas.drawText(item.productName.take(30), pad + 22f, baseline, boldPaint)

            val metaSub = buildString {
                if (item.packSize.isNotBlank()) append(item.packSize)
                if (business.trackBatch && item.batchNumber.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(item.batchNumber)
                }
            }
            if (metaSub.isNotBlank()) {
                canvas.drawText(metaSub.take(34), pad + 22f, baseline + 10.5f, mutedPaint)
            }

            canvas.drawText("${item.quantity}", colQtyX, baseline, rightBodyPaint)
            canvas.drawText(Formatters.formatDecimal(item.unitPrice), colRateX, baseline, rightBodyPaint)
            canvas.drawText(Formatters.formatDecimal(item.lineTotal), colAmtX, baseline, rightBoldPaint)

            y += rH
            canvas.drawLine(pad, y, contentRight, y, strokePaint)
        }

        y += 12f

        // --- 4. Clean Right-Aligned Totals Card ---
        val sumLeft = contentRight - 178f
        canvas.drawText("Subtotal", sumLeft, y, mutedPaint)
        canvas.drawText(Formatters.formatPkr(invoice.subtotal, currency), colAmtX, y, rightBodyPaint)
        y += 14.5f

        if (invoice.discountAmount > 0.0) {
            canvas.drawText("Discount", sumLeft, y, mutedPaint)
            canvas.drawText("-${Formatters.formatPkr(invoice.discountAmount, currency)}", colAmtX, y, rightBodyPaint)
            y += 14.5f
        }

        if (invoice.taxEnabled && invoice.taxRatePercent > 0.0) {
            canvas.drawText("Tax (${invoice.taxRatePercent}%)", sumLeft, y, mutedPaint)
            canvas.drawText(Formatters.formatPkr(invoice.taxAmount, currency), colAmtX, y, rightBodyPaint)
            y += 14.5f
        }

        // Highlighted Grand Total Pill Box
        val totalRect = RectF(sumLeft - 8f, y - 4f, contentRight, y + 20f)
        fillPaint.color = surfaceTint
        canvas.drawRoundRect(totalRect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(
            totalRect,
            6f,
            6f,
            Paint(strokePaint).apply { color = strongBorder; strokeWidth = 0.8f }
        )
        val totalLabelPaint = Paint(boldPaint).apply { color = accentColor; textSize = 11f }
        val totalValPaint = Paint(boldPaint).apply {
            color = darkInk
            textSize = 11.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Total", sumLeft, y + 12f, totalLabelPaint)
        canvas.drawText(Formatters.formatPkr(invoice.grandTotal, currency), colAmtX, y + 12f, totalValPaint)
        y += 32f

        canvas.drawText("Paid (${invoice.paymentMethod.take(12)})", sumLeft, y, mutedPaint)
        canvas.drawText(Formatters.formatPkr(invoice.amountPaid, currency), colAmtX, y, rightBodyPaint)

        if (invoice.balanceDue > 0.01 && !invoice.isReturned) {
            y += 14.5f
            val duePaint = Paint(rightBoldPaint).apply { color = Color.rgb(185, 28, 28) }
            canvas.drawText("Balance Due", sumLeft, y, Paint(boldPaint).apply { color = Color.rgb(185, 28, 28) })
            canvas.drawText(Formatters.formatPkr(invoice.balanceDue, currency), colAmtX, y, duePaint)
        }

        if (invoice.notes.isNotBlank()) {
            y += 18f
            canvas.drawText("Note: ${invoice.notes.take(66)}", pad, y, mutedPaint)
        }

        // --- 5. Snug Footer ---
        y += 16f
        canvas.drawLine(pad, y, contentRight, y, strokePaint)
        y += 13f
        val footerPaint = Paint(mutedPaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(
            business.invoiceFooter.ifBlank { "Thank you for choosing ${business.name}." }.take(64),
            compactWidth / 2f,
            y,
            footerPaint
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
        expenses: List<ExpenseEntity>,
        costOfGoodsSold: Double = 0.0,
        grossProfit: Double = grossSales - costOfGoodsSold,
        stockPurchaseSpend: Double = 0.0,
        inventoryRetailValue: Double = 0.0
    ): File {
        val currency = business.resolvedCurrency()
        val theme = AppThemeOption.fromKey(business.themeKey)
        val accentColor = theme.primaryAndroidColorInt()
        val softBg = theme.softAndroidColorInt()

        val darkInk = Color.rgb(15, 23, 42)
        val bodyInk = Color.rgb(30, 41, 59)
        val mutedInk = Color.rgb(100, 116, 139)
        val borderColor = Color.rgb(226, 232, 240)
        val surfaceTint = Color.rgb(248, 250, 252)

        val grouped = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
        val recentCount = expenses.take(15).size

        val estHeight = (275f + (grouped.size.coerceAtLeast(1) * 16f) + (recentCount * 15f) + 70f)
            .coerceIn(420f, PAGE_HEIGHT.toFloat()).toInt()

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, estHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bodyInk
            textSize = 9f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 8f
        }
        val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

        fillPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), estHeight.toFloat(), fillPaint)

        fillPaint.color = accentColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 4f, fillPaint)

        var y = 26f
        val logoBitmap = loadBusinessLogo(business)
        val titleX = if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, Rect(MARGIN.toInt(), y.toInt(), (MARGIN + 36).toInt(), (y + 36).toInt()), null)
            MARGIN + 46f
        } else {
            MARGIN
        }

        canvas.drawText(scopeTitle, titleX, y + 13f, titlePaint)
        canvas.drawText("Monthly Profit & Loss Report ($monthLabel)", titleX, y + 27f, boldPaint)
        canvas.drawText("Generated: ${Formatters.formatDateTime(System.currentTimeMillis())}", titleX, y + 39f, mutedPaint)

        y += 54f
        val rows = mutableListOf(
            "Total Sales Revenue ($invoiceCount invoices)" to Formatters.formatPkr(grossSales, currency),
            "Cost of Goods Sold (COGS)" to Formatters.formatPkr(costOfGoodsSold, currency),
            "Gross Profit" to Formatters.formatPkr(grossProfit, currency),
            "Operating Expenses" to Formatters.formatPkr(totalExpenses, currency),
            "Stock Purchases Spend (Asset Outflow)" to Formatters.formatPkr(stockPurchaseSpend, currency),
            "Net Profit / Loss" to Formatters.formatPkr(netProfit, currency),
            "Tax Collected" to Formatters.formatPkr(taxCollected, currency),
            "Inventory Asset Value (Cost)" to Formatters.formatPkr(stockValuation, currency)
        )
        if (inventoryRetailValue > 0.0) {
            rows.add("Inventory Retail Value" to Formatters.formatPkr(inventoryRetailValue, currency))
        }
        if (business.trackExpiry) {
            rows.add("Expiry Alerts" to "$expiringCount expiring soon, $expiredCount expired")
        }

        val boxHeight = rows.size * 17f + 16f
        val boxRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight)
        fillPaint.color = surfaceTint
        canvas.drawRoundRect(boxRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(boxRect, 8f, 8f, strokePaint)

        var ky = y + 19f
        rows.forEach { (k, v) ->
            val isNet = k == "Net Profit / Loss"
            val valPaint = if (isNet) {
                Paint(rightBold).apply {
                    color = if (netProfit >= 0.0) Color.rgb(21, 128, 61) else Color.rgb(185, 28, 28)
                }
            } else {
                rightBold
            }
            canvas.drawText(k, MARGIN + 12f, ky, if (isNet) boldPaint else bodyPaint)
            canvas.drawText(v, PAGE_WIDTH - MARGIN - 12f, ky, valPaint)
            ky += 17f
        }

        y += boxHeight + 20f
        canvas.drawText("EXPENSE & STOCK PURCHASE BREAKDOWN", MARGIN, y, sectionPaint)
        y += 15f

        if (grouped.isEmpty()) {
            canvas.drawText("No expenses recorded in $monthLabel.", MARGIN, y, mutedPaint)
            y += 16f
        } else {
            grouped.forEach { (cat, sum) ->
                canvas.drawText(cat, MARGIN + 4f, y, bodyPaint)
                canvas.drawText(Formatters.formatPkr(sum, currency), PAGE_WIDTH - MARGIN - 4f, y, rightBold)
                y += 16f
            }
        }

        if (expenses.isNotEmpty() && y < estHeight - 40f) {
            y += 12f
            canvas.drawText("RECENT ENTRIES ($monthLabel)", MARGIN, y, sectionPaint)
            y += 15f
            expenses.take(15).forEach { exp ->
                if (y > estHeight - 24f) return@forEach
                val bySuffix = if (exp.performedByName.isNotBlank()) " · By ${exp.performedByName}" else ""
                canvas.drawText(
                    "${Formatters.formatDate(exp.expenseDateMillis)}  ·  ${exp.title.take(34)} (${exp.category})$bySuffix",
                    MARGIN + 4f,
                    y,
                    bodyPaint
                )
                canvas.drawText(Formatters.formatPkr(exp.amount, currency), PAGE_WIDTH - MARGIN - 4f, y, rightBold)
                y += 15f
            }
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
