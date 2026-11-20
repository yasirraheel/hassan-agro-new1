package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import com.example.ui.theme.AgriLeafPrimary
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.HarvestAmberSecondary
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureMedium
import com.example.ui.theme.SafeStockGreen
import com.example.util.Formatters
import com.example.util.ShareHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    activeBusiness: BusinessEntity,
    businessCount: Int,
    products: List<ProductEntity>,
    invoices: List<InvoiceWithItems>,
    expenses: List<ExpenseEntity>,
    allProducts: List<ProductEntity>,
    allInvoices: List<InvoiceWithItems>,
    allExpenses: List<ExpenseEntity>,
    onGenerateReportPdf: suspend (Int, Int, Boolean) -> File,
    onExportCsv: suspend (String, Int, Int, Boolean) -> Pair<File, String>,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var allBusinessesScope by remember { mutableStateOf(false) }

    // Pending CSV content for Storage Access Framework Save
    var pendingSaveCsvContent by remember { mutableStateOf<String?>(null) }
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingSaveCsvContent
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                onNotify("CSV exported successfully to device")
            } catch (_: Exception) {
                onNotify("Failed to save CSV file")
            }
        }
        pendingSaveCsvContent = null
    }

    val (startMs, endMs) = remember(selectedYear, selectedMonth) {
        Formatters.monthBoundsMillis(selectedYear, selectedMonth)
    }

    val scopedProducts = if (allBusinessesScope) allProducts else products
    val scopedInvoices = remember(allBusinessesScope, invoices, allInvoices, startMs, endMs) {
        (if (allBusinessesScope) allInvoices else invoices)
            .filter { !it.invoice.isReturned && it.invoice.createdAt in startMs until endMs }
    }
    val scopedExpenses = remember(allBusinessesScope, expenses, allExpenses, startMs, endMs) {
        (if (allBusinessesScope) allExpenses else expenses)
            .filter { it.expenseDateMillis in startMs until endMs }
    }

    val grossSales = remember(scopedInvoices) { scopedInvoices.sumOf { it.invoice.grandTotal } }
    val totalExpenses = remember(scopedExpenses) { scopedExpenses.sumOf { it.amount } }
    val netProfit = grossSales - totalExpenses
    val taxCollected = remember(scopedInvoices) { scopedInvoices.sumOf { it.invoice.taxAmount } }
    val stockValuation = remember(scopedProducts) { scopedProducts.sumOf { it.stockQty * it.purchasePrice } }

    // Weekly buckets (W1..W4/W5)
    val weeklyBuckets = remember(scopedInvoices, scopedExpenses) {
        val salesByWeek = DoubleArray(4)
        val expByWeek = DoubleArray(4)
        val cal = Calendar.getInstance()
        scopedInvoices.forEach {
            cal.timeInMillis = it.invoice.createdAt
            val idx = ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 8).coerceIn(0, 3)
            salesByWeek[idx] += it.invoice.grandTotal
        }
        scopedExpenses.forEach {
            cal.timeInMillis = it.expenseDateMillis
            val idx = ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 8).coerceIn(0, 3)
            expByWeek[idx] += it.amount
        }
        (0..3).map { w -> Triple("W${w + 1}", salesByWeek[w], expByWeek[w]) }
    }

    val categoryBreakdown = remember(scopedExpenses) {
        scopedExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month + Multi-Business Scope Header
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 0) {
                                        selectedMonth = 11
                                        selectedYear -= 1
                                    } else {
                                        selectedMonth -= 1
                                    }
                                },
                                modifier = Modifier.testTag("reports_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_month))
                            }
                            Text(
                                text = Formatters.formatMonthYear(selectedYear, selectedMonth),
                                style = MaterialTheme.typography.titleLarge
                            )
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 11) {
                                        selectedMonth = 0
                                        selectedYear += 1
                                    } else {
                                        selectedMonth += 1
                                    }
                                },
                                modifier = Modifier.testTag("reports_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
                            }
                        }

                        if (businessCount > 1) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !allBusinessesScope,
                                    onClick = { allBusinessesScope = false },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                ) { Text(activeBusiness.name, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                                SegmentedButton(
                                    selected = allBusinessesScope,
                                    onClick = { allBusinessesScope = true },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("All ($businessCount)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }

            // Financial KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = "Gross Sales (${scopedInvoices.size})",
                            value = Formatters.formatPkr(grossSales),
                            accent = SafeStockGreen,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Expenses (${scopedExpenses.size})",
                            value = Formatters.formatPkr(totalExpenses),
                            accent = ExpiredCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = "Net Profit / Loss",
                            value = Formatters.formatPkr(netProfit),
                            accent = if (netProfit >= 0) SafeStockGreen else ExpiredCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Tax Collected",
                            value = Formatters.formatPkr(taxCollected),
                            accent = HarvestAmberSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    KpiCard(
                        label = "Current Inventory Valuation (Purchase Cost)",
                        value = Formatters.formatPkr(stockValuation),
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Native Canvas Weekly Sales vs Expenses Bar Chart
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weekly Sales vs Expenses", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(AgriLeafPrimary, "Sales")
                                LegendDot(ExpiredCrimson, "Expenses")
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        WeeklyBarChart(
                            buckets = weeklyBuckets,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                }
            }

            // Expense Breakdown
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Expense Breakdown", style = MaterialTheme.typography.titleMedium)
                        if (categoryBreakdown.isEmpty()) {
                            Text(
                                text = "No expenses logged for this period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val total = max(totalExpenses, 1.0)
                            categoryBreakdown.forEach { (cat, sum) ->
                                val fraction = (sum / total).toFloat().coerceIn(0f, 1f)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${Formatters.formatPkr(sum)} (${(fraction * 100).toInt()}%)",
                                            style = MonoCodeSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export Center
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Export Monthly Report & Data", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Generate branded PDF summaries or Excel-compatible CSV spreadsheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    val pdf = onGenerateReportPdf(selectedYear, selectedMonth, allBusinessesScope)
                                    ShareHelper.shareFileGeneric(
                                        context = context,
                                        file = pdf,
                                        mimeType = "application/pdf",
                                        chooserTitle = "Share Monthly Report PDF"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_monthly_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Monthly Financial Report (PDF)")
                        }

                        ExportCsvRow(
                            title = "Inventory & Expiry Stock (CSV)",
                            testTagPrefix = "export_inventory_csv",
                            onShare = {
                                scope.launch {
                                    val (file, _) = onExportCsv("inventory", selectedYear, selectedMonth, allBusinessesScope)
                                    ShareHelper.shareFileGeneric(context, file, "text/csv", "Share Inventory CSV")
                                }
                            },
                            onSave = {
                                scope.launch {
                                    val (file, csv) = onExportCsv("inventory", selectedYear, selectedMonth, allBusinessesScope)
                                    pendingSaveCsvContent = csv
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                        )

                        ExportCsvRow(
                            title = "Monthly Expenses Ledger (CSV)",
                            testTagPrefix = "export_expenses_csv",
                            onShare = {
                                scope.launch {
                                    val (file, _) = onExportCsv("expenses", selectedYear, selectedMonth, allBusinessesScope)
                                    ShareHelper.shareFileGeneric(context, file, "text/csv", "Share Expenses CSV")
                                }
                            },
                            onSave = {
                                scope.launch {
                                    val (file, csv) = onExportCsv("expenses", selectedYear, selectedMonth, allBusinessesScope)
                                    pendingSaveCsvContent = csv
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                        )

                        ExportCsvRow(
                            title = "Monthly Sales Invoices (CSV)",
                            testTagPrefix = "export_sales_csv",
                            onShare = {
                                scope.launch {
                                    val (file, _) = onExportCsv("sales", selectedYear, selectedMonth, allBusinessesScope)
                                    ShareHelper.shareFileGeneric(context, file, "text/csv", "Share Sales CSV")
                                }
                            },
                            onSave = {
                                scope.launch {
                                    val (file, csv) = onExportCsv("sales", selectedYear, selectedMonth, allBusinessesScope)
                                    pendingSaveCsvContent = csv
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MonoFigureMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(3.dp),
            color = color
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WeeklyBarChart(
    buckets: List<Triple<String, Double, Double>>,
    modifier: Modifier = Modifier
) {
    val maxVal = max(
        buckets.maxOfOrNull { max(it.second, it.third) } ?: 1.0,
        1.0
    )
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val chartHeight = size.height - 8f
            val groupWidth = size.width / buckets.size
            val barWidth = (groupWidth * 0.28f).coerceAtMost(36f)
            val gap = 6f

            // Baseline
            drawLine(
                color = Color(0xFFD4E1D8),
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2f
            )

            buckets.forEachIndexed { idx, (_, sales, exp) ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val salesH = ((sales / maxVal) * (chartHeight - 10f)).toFloat().coerceAtLeast(if (sales > 0) 6f else 2f)
                val expH = ((exp / maxVal) * (chartHeight - 10f)).toFloat().coerceAtLeast(if (exp > 0) 6f else 2f)

                drawRoundRect(
                    color = AgriLeafPrimary,
                    topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - salesH),
                    size = Size(barWidth, salesH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = ExpiredCrimson.copy(alpha = 0.85f),
                    topLeft = Offset(centerX + gap / 2f, chartHeight - expH),
                    size = Size(barWidth, expH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEach { (label, _, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ExportCsvRow(
    title: String,
    testTagPrefix: String,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onSave,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("${testTagPrefix}_save")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Save CSV", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTagPrefix}_share")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share CSV", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
