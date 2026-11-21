package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.MonthlyTotalRow
import com.example.data.local.ProductEntity
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureMedium
import com.example.util.Formatters
import com.example.util.ProfitLossCalculator
import com.example.util.ShareHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private data class MonthlyChartBucket(
    val yearMonth: String,
    val shortLabel: String,
    val income: Double,
    val expense: Double
) {
    val netProfit: Double get() = income - expense
}

private data class AdminActivitySummary(
    val adminName: String,
    val invoiceCount: Int,
    val salesTotal: Double,
    val expenseCount: Int,
    val expenseTotal: Double
)

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
    activeMonthlySalesRows: List<MonthlyTotalRow>,
    activeMonthlyExpenseRows: List<MonthlyTotalRow>,
    allMonthlySalesRows: List<MonthlyTotalRow>,
    allMonthlyExpenseRows: List<MonthlyTotalRow>,
    canExportData: Boolean,
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
    var focusedMonthIndex by remember(selectedYear, selectedMonth) { mutableIntStateOf(5) }

    val semantic = LocalSemanticColors.current
    val currency = activeBusiness.resolvedCurrency()
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

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

    // 6-Month SQL-backed Monthly Income vs. Expense totals directly from Room DAO aggregates
    val monthlyBuckets = remember(
        selectedYear,
        selectedMonth,
        allBusinessesScope,
        activeMonthlySalesRows,
        activeMonthlyExpenseRows,
        allMonthlySalesRows,
        allMonthlyExpenseRows
    ) {
        val salesMap = (if (allBusinessesScope) allMonthlySalesRows else activeMonthlySalesRows)
            .associate { it.yearMonth to it.total }
        val expenseMap = (if (allBusinessesScope) allMonthlyExpenseRows else activeMonthlyExpenseRows)
            .associate { it.yearMonth to it.total }

        Formatters.lastSixYearMonths(selectedYear, selectedMonth).map { ym ->
            MonthlyChartBucket(
                yearMonth = ym,
                shortLabel = Formatters.formatShortMonth(ym),
                income = salesMap[ym] ?: 0.0,
                expense = expenseMap[ym] ?: 0.0
            )
        }
    }

    val sixMonthIncomeTotal = remember(monthlyBuckets) { monthlyBuckets.sumOf { it.income } }
    val sixMonthExpenseTotal = remember(monthlyBuckets) { monthlyBuckets.sumOf { it.expense } }
    val sixMonthNetTotal = sixMonthIncomeTotal - sixMonthExpenseTotal

    // Month-over-Month Growth calculation (selected month vs previous month)
    val momGrowthPercent = remember(monthlyBuckets) {
        val prev = monthlyBuckets.getOrNull(4)?.income ?: 0.0
        val curr = monthlyBuckets.getOrNull(5)?.income ?: 0.0
        when {
            prev > 0.0 -> ((curr - prev) / prev) * 100.0
            curr > 0.0 -> 100.0
            else -> 0.0
        }
    }

    val scopedAllInvoices = if (allBusinessesScope) allInvoices else invoices
    val scopedAllExpenses = if (allBusinessesScope) allExpenses else expenses
    val monthPl = remember(scopedAllInvoices, scopedAllExpenses, scopedProducts, startMs, endMs) {
        ProfitLossCalculator.calculate(
            invoices = scopedAllInvoices,
            expenses = scopedAllExpenses,
            products = scopedProducts,
            startMillis = startMs,
            endMillis = endMs
        )
    }
    val grossSales = monthPl.totalSales
    val totalExpenses = remember(scopedExpenses) { scopedExpenses.sumOf { it.amount } }
    val taxCollected = remember(scopedInvoices) { scopedInvoices.sumOf { it.invoice.taxAmount } }
    val stockValuation = monthPl.inventoryCostValuation

    // Weekly buckets (W1..W4)
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

    // Per-Admin Audit Activity for the selected month
    val adminSummaries = remember(scopedInvoices, scopedExpenses) {
        val names = (
            scopedInvoices.map { it.invoice.performedByName.trim().ifBlank { "System" } } +
                scopedExpenses.map { it.performedByName.trim().ifBlank { "System" } }
            ).distinct()
        names.map { adminName ->
            val adminInvs = scopedInvoices.filter {
                it.invoice.performedByName.trim().ifBlank { "System" } == adminName
            }
            val adminExps = scopedExpenses.filter {
                it.performedByName.trim().ifBlank { "System" } == adminName
            }
            AdminActivitySummary(
                adminName = adminName,
                invoiceCount = adminInvs.size,
                salesTotal = adminInvs.sumOf { it.invoice.grandTotal },
                expenseCount = adminExps.size,
                expenseTotal = adminExps.sumOf { it.amount }
            )
        }.sortedByDescending { it.salesTotal + it.expenseTotal }
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
                    border = BorderStroke(1.dp, outlineColor)
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
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.cd_previous_month)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Formatters.formatMonthYear(selectedYear, selectedMonth),
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (allBusinessesScope) "All Assigned Businesses" else activeBusiness.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.cd_next_month)
                                )
                            }
                        }

                        if (businessCount > 1) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !allBusinessesScope,
                                    onClick = { allBusinessesScope = false },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                    modifier = Modifier.testTag("reports_scope_active")
                                ) {
                                    Text(
                                        activeBusiness.name,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                SegmentedButton(
                                    selected = allBusinessesScope,
                                    onClick = { allBusinessesScope = true },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                    modifier = Modifier.testTag("reports_scope_all")
                                ) {
                                    Text(
                                        "All ($businessCount)",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial P&L KPI Grid for Selected Month
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = "Total Sales (${monthPl.invoiceCount})",
                            value = Formatters.formatShortPkr(grossSales, currency),
                            accent = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Cost of Goods",
                            value = Formatters.formatShortPkr(monthPl.costOfGoodsSold, currency),
                            accent = semantic.warningFg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = String.format(Locale.US, "Gross Profit (%.0f%%)", monthPl.grossMarginPercent),
                            value = Formatters.formatShortPkr(monthPl.grossProfit, currency),
                            accent = if (monthPl.grossProfit >= 0) semantic.positiveFg else semantic.negativeFg,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Operating Expenses",
                            value = Formatters.formatShortPkr(monthPl.operatingExpenses, currency),
                            accent = semantic.negativeFg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = if (monthPl.netProfit >= 0) "Net Profit" else "Net Loss",
                            value = Formatters.formatShortPkr(monthPl.netProfit, currency),
                            accent = if (monthPl.netProfit >= 0) semantic.positiveFg else semantic.negativeFg,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Stock Purchased",
                            value = Formatters.formatStockCompactPkr(monthPl.stockPurchaseSpend, currency),
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            label = "Stock Asset Value",
                            value = Formatters.formatStockCompactPkr(stockValuation, currency),
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Tax Collected",
                            value = Formatters.formatShortPkr(taxCollected, currency),
                            accent = semantic.warningFg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6-Month Monthly Income vs Expense Grouped Bar Chart (Room SQL Aggregates)
            item {
                val activeBucket = monthlyBuckets.getOrElse(focusedMonthIndex) { monthlyBuckets.last() }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineColor),
                    modifier = Modifier.testTag("reports_monthly_income_expense_chart")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "6-Month Income vs Expenses",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap any month to inspect exact Room DB totals",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(primaryColor, "Income")
                                LegendDot(semantic.negativeFg, "Expense")
                            }
                        }

                        // Focused Month Callout Strip
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activeBucket.yearMonth,
                                        style = MonoCodeSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Cashflow: ${Formatters.formatCompactPkr(activeBucket.netProfit, currency)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeBucket.netProfit >= 0) semantic.positiveFg else semantic.negativeFg
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = Formatters.formatCompactPkr(activeBucket.income, currency),
                                            style = MonoCodeSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryColor
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = Formatters.formatCompactPkr(activeBucket.expense, currency),
                                            style = MonoCodeSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = semantic.negativeFg
                                        )
                                    }
                                }
                            }
                        }

                        MonthlyGroupedBarChart(
                            buckets = monthlyBuckets,
                            selectedIndex = focusedMonthIndex,
                            onSelectIndex = { focusedMonthIndex = it },
                            incomeColor = primaryColor,
                            expenseColor = semantic.negativeFg,
                            gridColor = outlineColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                        )

                        // 6-Month Cumulative Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "6M Income: ${Formatters.formatCompactPkr(sixMonthIncomeTotal, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "6M Net: ${Formatters.formatCompactPkr(sixMonthNetTotal, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sixMonthNetTotal >= 0) semantic.positiveFg else semantic.negativeFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Business Growth Trend (6-Month Net Profit Line & Area Chart + MoM Growth Badge)
            item {
                val isPositiveGrowth = momGrowthPercent >= 0.0
                val growthBadgeBg = if (isPositiveGrowth) semantic.positiveBg else semantic.negativeBg
                val growthBadgeFg = if (isPositiveGrowth) semantic.positiveFg else semantic.negativeFg

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineColor),
                    modifier = Modifier.testTag("reports_growth_trend_chart")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Business Growth Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "6-month net profit trajectory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = growthBadgeBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPositiveGrowth) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = growthBadgeFg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s%.1f%% MoM",
                                            if (isPositiveGrowth) "+" else "",
                                            momGrowthPercent
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = growthBadgeFg,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        BusinessGrowthAreaChart(
                            buckets = monthlyBuckets,
                            lineColor = primaryColor,
                            negativeColor = semantic.negativeFg,
                            gridColor = outlineColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }

            // Native Canvas Weekly Sales vs Expenses Bar Chart
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weekly Sales vs Expenses", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(primaryColor, "Sales")
                                LegendDot(semantic.negativeFg, "Expenses")
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        WeeklyBarChart(
                            buckets = weeklyBuckets,
                            salesColor = primaryColor,
                            expenseColor = semantic.negativeFg,
                            gridColor = outlineColor,
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
                    border = BorderStroke(1.dp, outlineColor)
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
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${Formatters.formatShortPkr(sum, currency)} (${(fraction * 100).toInt()}%)",
                                            style = MonoCodeSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = primaryColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Admin / Staff Audit Activity Summary
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineColor),
                    modifier = Modifier.testTag("reports_admin_activity_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Transactions by Admin / Staff", style = MaterialTheme.typography.titleMedium)
                        if (adminSummaries.isEmpty()) {
                            Text(
                                text = "No transactions recorded for this month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            adminSummaries.forEach { summary ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = summary.adminName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${summary.invoiceCount} sales • ${summary.expenseCount} expenses",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+${Formatters.formatCompactPkr(summary.salesTotal, currency)}",
                                                style = MonoCodeSmall,
                                                color = semantic.positiveFg,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (summary.expenseTotal > 0) {
                                                Text(
                                                    text = "-${Formatters.formatCompactPkr(summary.expenseTotal, currency)}",
                                                    style = MonoCodeSmall,
                                                    color = semantic.negativeFg
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export Center (RBAC Gated)
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Export Monthly Report & Data", style = MaterialTheme.typography.titleMedium)
                        if (!canExportData) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Your assigned role does not include PDF/CSV export permissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Generate branded PDF summaries or Excel-compatible CSV spreadsheets including admin audit columns.",
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
                                    .height(48.dp)
                                    .testTag("export_monthly_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share Monthly Financial Report (PDF)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }

                            ExportCsvRow(
                                title = "Inventory Stock Ledger (CSV)",
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
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun MonthlyGroupedBarChart(
    buckets: List<MonthlyChartBucket>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    incomeColor: Color,
    expenseColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = max(
        buckets.maxOfOrNull { max(it.income, it.expense) } ?: 1.0,
        1.0
    )
    val selectedHighlight = incomeColor.copy(alpha = 0.08f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val chartHeight = size.height - 8f
            val count = buckets.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceIn(8f, 28f)
            val gap = 5f

            // Subtle horizontal gridlines (25%, 50%, 75%, baseline)
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { ratio ->
                val y = chartHeight * ratio
                drawLine(
                    color = gridColor.copy(alpha = if (ratio == 1.0f) 0.9f else 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (ratio == 1.0f) 2f else 1f
                )
            }

            buckets.forEachIndexed { idx, bucket ->
                val groupLeft = groupWidth * idx
                val centerX = groupLeft + groupWidth / 2f

                if (idx == selectedIndex) {
                    drawRoundRect(
                        color = selectedHighlight,
                        topLeft = Offset(groupLeft + 4f, 0f),
                        size = Size((groupWidth - 8f).coerceAtLeast(4f), chartHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                val incomeH = ((bucket.income / maxVal) * (chartHeight - 12f))
                    .toFloat()
                    .coerceAtLeast(if (bucket.income > 0) 6f else 2f)
                val expenseH = ((bucket.expense / maxVal) * (chartHeight - 12f))
                    .toFloat()
                    .coerceAtLeast(if (bucket.expense > 0) 6f else 2f)

                val alphaFactor = if (idx == selectedIndex) 1f else 0.72f
                drawRoundRect(
                    color = incomeColor.copy(alpha = alphaFactor),
                    topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - incomeH),
                    size = Size(barWidth, incomeH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = expenseColor.copy(alpha = alphaFactor * 0.9f),
                    topLeft = Offset(centerX + gap / 2f, chartHeight - expenseH),
                    size = Size(barWidth, expenseH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { idx, bucket ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            else Color.Transparent
                        )
                        .clickable { onSelectIndex(idx) }
                        .padding(vertical = 4.dp)
                        .testTag("reports_month_bar_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bucket.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessGrowthAreaChart(
    buckets: List<MonthlyChartBucket>,
    lineColor: Color,
    negativeColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    val values = buckets.map { it.netProfit }
    val rawMax = values.maxOrNull() ?: 0.0
    val rawMin = min(values.minOrNull() ?: 0.0, 0.0)
    val span = max(rawMax - rawMin, 1.0)
    val dotFill = MaterialTheme.colorScheme.surface

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val topPad = 14f
            val bottomPad = 12f
            val usableH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
            val count = buckets.size.coerceAtLeast(1)
            val stepX = if (count > 1) size.width / count else size.width

            fun yFor(value: Double): Float {
                val normalized = ((value - rawMin) / span).toFloat().coerceIn(0f, 1f)
                return topPad + usableH * (1f - normalized)
            }

            // Horizontal reference lines + zero line
            listOf(0f, 0.5f, 1f).forEach { r ->
                val y = topPad + usableH * r
                drawLine(
                    color = gridColor.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            val zeroY = yFor(0.0)
            drawLine(
                color = gridColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 2f
            )

            val points = buckets.mapIndexed { idx, bucket ->
                Offset(stepX * idx + stepX / 2f, yFor(bucket.netProfit))
            }

            if (points.size >= 2) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, zeroY)
                    lineTo(points.first().x, zeroY)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                        startY = topPad,
                        endY = topPad + usableH
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            points.forEachIndexed { idx, pt ->
                val isNeg = buckets[idx].netProfit < 0
                val ptColor = if (isNeg) negativeColor else lineColor
                drawCircle(color = ptColor, radius = 8f, center = pt)
                drawCircle(color = dotFill, radius = 4f, center = pt)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
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
    salesColor: Color,
    expenseColor: Color,
    gridColor: Color,
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

            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2f
            )

            buckets.forEachIndexed { idx, (_, sales, exp) ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val salesH = ((sales / maxVal) * (chartHeight - 10f)).toFloat().coerceAtLeast(if (sales > 0) 6f else 2f)
                val expH = ((exp / maxVal) * (chartHeight - 10f)).toFloat().coerceAtLeast(if (exp > 0) 6f else 2f)

                drawRoundRect(
                    color = salesColor,
                    topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - salesH),
                    size = Size(barWidth, salesH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = expenseColor.copy(alpha = 0.85f),
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
                    textAlign = TextAlign.Center
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onSave,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("${testTagPrefix}_save")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Save CSV", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("${testTagPrefix}_share")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share CSV", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
