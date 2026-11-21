package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PerformedByPill
import com.example.ui.components.ProofImageViewerDialog
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureLarge
import com.example.ui.theme.MonoFigureMedium
import com.example.util.DashboardPeriod
import com.example.util.Formatters
import com.example.util.ProfitLossCalculator
import com.example.viewmodel.InventoryFilter
import java.io.File

@Composable
fun DashboardScreen(
    activeBusiness: BusinessEntity,
    authSession: AuthSession,
    selectedPeriod: DashboardPeriod,
    onPeriodChange: (DashboardPeriod) -> Unit,
    products: List<ProductEntity>,
    invoices: List<InvoiceWithItems>,
    expenses: List<ExpenseEntity>,
    onQuickNewBill: () -> Unit,
    onQuickScanStock: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onQuickSync: () -> Unit,
    onOpenInventoryWithFilter: (InventoryFilter) -> Unit,
    onOpenInvoice: (InvoiceWithItems) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppThemeOption.current
    val semantic = LocalSemanticColors.current
    val currency = activeBusiness.resolvedCurrency()
    var selectedDetailInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val (periodStart, periodEnd) = remember(selectedPeriod) {
        selectedPeriod.boundsMillis()
    }

    val pnl = remember(products, invoices, expenses, periodStart, periodEnd) {
        ProfitLossCalculator.calculate(
            products = products,
            invoices = invoices,
            expenses = expenses,
            startMillis = periodStart,
            endMillis = periodEnd
        )
    }

    val periodInvoices = remember(invoices, periodStart, periodEnd) {
        invoices.filter { it.invoice.createdAt in periodStart until periodEnd }
    }

    val totalPendingCredit = remember(invoices) {
        invoices.filter { !it.invoice.isReturned }.sumOf { it.invoice.balanceDue }
    }

    val expiredCount = remember(products) {
        products.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRED }
    }
    val expiringSoonCount = remember(products) {
        products.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRING_SOON }
    }
    val lowStockCount = remember(products) {
        products.count { it.stockQty <= it.lowStockThreshold }
    }

    val heroStart = if (semantic.isDark) activeTheme.darkHeroStart else activeTheme.heroStart
    val heroEnd = if (semantic.isDark) activeTheme.darkHeroEnd else activeTheme.heroEnd

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Persistent Time Period Filter Tags (always clearly highlighted)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onPeriodChange(period) }
                                .testTag("dashboard_period_${period.name.lowercase()}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Hero Profit & Loss Card dynamically tinted to business theme (Light & Dark)
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.Transparent,
                    border = if (semantic.isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(colors = listOf(heroStart, heroEnd)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedPeriod.label} ${if (pnl.isLoss) "Net Loss" else "Net Profit"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (pnl.isLoss) Color(0xFFDC2626).copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (pnl.isLoss) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (pnl.totalSales > 0.0) {
                                                "${Formatters.formatDecimal(pnl.netMarginPercent)}% Margin"
                                            } else {
                                                "${pnl.invoiceCount} Bills"
                                            },
                                            style = MonoCodeSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatPkr(pnl.netProfit, currency),
                                style = MonoFigureLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${authSession.userName} • ${authSession.roleName.ifBlank { "Admin" }} • ${pnl.unitsSold} units sold",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HeroSubMetric(
                                    label = "Total Sales",
                                    value = Formatters.formatShortPkr(pnl.totalSales, currency),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroSubMetric(
                                    label = "Cost",
                                    value = Formatters.formatShortPkr(pnl.costOfGoodsSold, currency),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroSubMetric(
                                    label = "Gross Profit",
                                    value = Formatters.formatShortPkr(pnl.grossProfit, currency),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Full Accounting Breakdown Card (Operating Expenses vs Stock Purchases vs Inventory Capital)
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_pnl_breakdown_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profit, Loss & Stock Capital",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedPeriod.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PnlDetailCell(
                                label = "Operating Exp.",
                                value = Formatters.formatCompactPkr(pnl.operatingExpenses, currency),
                                subtext = "Deducted from profit",
                                accent = semantic.negativeFg,
                                bg = semantic.negativeBg,
                                modifier = Modifier.weight(1f)
                            )
                            PnlDetailCell(
                                label = "Stock Bought",
                                value = Formatters.formatStockCompactPkr(pnl.stockPurchaseSpend, currency),
                                subtext = "Inventory spend",
                                accent = MaterialTheme.colorScheme.tertiary,
                                bg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PnlDetailCell(
                                label = "Stock Asset Value",
                                value = Formatters.formatStockCompactPkr(pnl.inventoryCostValuation, currency),
                                subtext = "At purchase cost",
                                accent = MaterialTheme.colorScheme.primary,
                                bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f)
                            )
                            PnlDetailCell(
                                label = "Pending Credit",
                                value = Formatters.formatCompactPkr(totalPendingCredit, currency),
                                subtext = "Customer Udhaar",
                                accent = semantic.warningFg,
                                bg = semantic.warningBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick Actions Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.AddShoppingCart,
                        label = "POS Sale",
                        tint = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        testTag = "quick_action_new_bill",
                        onClick = onQuickNewBill,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.QrCodeScanner,
                        label = "Scan In",
                        tint = MaterialTheme.colorScheme.tertiary,
                        bg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                        testTag = "quick_action_scan_stock",
                        onClick = onQuickScanStock,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.ReceiptLong,
                        label = "Expense",
                        tint = MaterialTheme.colorScheme.secondary,
                        bg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        testTag = "quick_action_add_expense",
                        onClick = onQuickAddExpense,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.CloudSync,
                        label = "Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        testTag = "quick_action_sync",
                        onClick = onQuickSync,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stock Health Alerts (adapts cleanly to whether this business tracks Expiry Dates!)
            item {
                Text(
                    text = if (activeBusiness.trackExpiry) "Stock & Expiry Alerts" else "Inventory Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeBusiness.trackExpiry) {
                        AlertSummaryCard(
                            icon = Icons.Default.EventBusy,
                            count = expiredCount,
                            label = "Expired",
                            fg = semantic.negativeFg,
                            bg = semantic.negativeBg,
                            testTag = "alert_card_expired",
                            onClick = { onOpenInventoryWithFilter(InventoryFilter.EXPIRED) },
                            modifier = Modifier.weight(1f)
                        )
                        AlertSummaryCard(
                            icon = Icons.Default.WarningAmber,
                            count = expiringSoonCount,
                            label = "Expiring ≤30d",
                            fg = semantic.warningFg,
                            bg = semantic.warningBg,
                            testTag = "alert_card_expiring",
                            onClick = { onOpenInventoryWithFilter(InventoryFilter.EXPIRING_SOON) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        AlertSummaryCard(
                            icon = Icons.Default.Inventory2,
                            count = products.size,
                            label = "Total Products",
                            fg = MaterialTheme.colorScheme.primary,
                            bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            testTag = "alert_card_total_skus",
                            onClick = { onOpenInventoryWithFilter(InventoryFilter.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AlertSummaryCard(
                        icon = Icons.Default.Inventory2,
                        count = lowStockCount,
                        label = "Low Stock",
                        fg = MaterialTheme.colorScheme.tertiary,
                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                        testTag = "alert_card_low_stock",
                        onClick = { onOpenInventoryWithFilter(InventoryFilter.LOW_STOCK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Sales for Selected Period
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sales (${selectedPeriod.label})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${periodInvoices.size} bills • ${products.size} SKUs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (periodInvoices.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = "No sales in ${selectedPeriod.label.lowercase()}",
                        subtitle = if (invoices.isEmpty())
                            "Add or scan products into ${activeBusiness.name}'s inventory, then create invoices with multi-tier pricing & WhatsApp PDF sharing."
                        else
                            "Switch the time period tag above (e.g. This Month or All Time) or start a new POS sale.",
                        actionLabel = if (invoices.isEmpty()) "Scan First Product" else "New POS Sale",
                        onAction = if (invoices.isEmpty()) onQuickScanStock else onQuickNewBill,
                        actionTestTag = "dashboard_empty_scan_btn"
                    )
                }
            } else {
                items(periodInvoices.take(6), key = { it.invoice.id }) { entry ->
                    val statusEnum = InvoicePaymentStatus.fromName(entry.invoice.paymentStatus)
                    val performerName = entry.invoice.performedByName.trim().ifBlank { "System" }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDetailInvoice = entry }
                            .testTag("dashboard_sale_row_${entry.invoice.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.invoice.customerName.ifBlank { "Walk-in Customer" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = Formatters.formatPkrOneDecimal(entry.invoice.grandTotal, currency),
                                    style = MonoFigureMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${Formatters.formatDate(entry.invoice.createdAt)} • $performerName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                val (badgeBg, badgeFg, badgeText) = when {
                                    entry.invoice.isReturned -> Triple(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        "Returned"
                                    )
                                    statusEnum == InvoicePaymentStatus.PAID -> Triple(
                                        semantic.positiveBg,
                                        semantic.positiveFg,
                                        "Paid"
                                    )
                                    statusEnum == InvoicePaymentStatus.PARTIAL -> Triple(
                                        semantic.warningBg,
                                        semantic.warningFg,
                                        "Partial"
                                    )
                                    else -> Triple(
                                        semantic.negativeBg,
                                        semantic.negativeFg,
                                        "Credit"
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = badgeFg,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    selectedDetailInvoice?.let { entry ->
        SaleQuickDetailDialog(
            entry = entry,
            products = products,
            currency = currency,
            onOpenReceipt = {
                selectedDetailInvoice = null
                onOpenInvoice(entry)
            },
            onDismiss = { selectedDetailInvoice = null }
        )
    }
}

@Composable
private fun SaleQuickDetailDialog(
    entry: InvoiceWithItems,
    products: List<ProductEntity>,
    currency: String,
    onOpenReceipt: () -> Unit,
    onDismiss: () -> Unit
) {
    val inv = entry.invoice
    val semantic = LocalSemanticColors.current
    val statusEnum = InvoicePaymentStatus.fromName(inv.paymentStatus)
    val invoiceCost = remember(entry, products) {
        val map = products.associate { it.id to it.purchasePrice }
        entry.items.sumOf { item ->
            val c = if (item.costPrice > 0.0) item.costPrice else (map[item.productId] ?: 0.0)
            c * item.quantity
        }
    }
    val invoiceProfit = inv.grandTotal - invoiceCost
    val hasProof = !inv.proofImagePath.isNullOrBlank() && File(inv.proofImagePath).exists()
    var showProofFull by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .testTag("dashboard_sale_detail_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = inv.customerName.ifBlank { "Walk-in Customer" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${inv.invoiceNumber} • ${Formatters.formatDateTime(inv.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PerformedByPill(name = inv.performedByName)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            inv.isReturned -> MaterialTheme.colorScheme.surfaceVariant
                            statusEnum == InvoicePaymentStatus.PAID -> semantic.positiveBg
                            statusEnum == InvoicePaymentStatus.PARTIAL -> semantic.warningBg
                            else -> semantic.negativeBg
                        }
                    ) {
                        Text(
                            text = if (inv.isReturned) "Returned" else "${statusEnum.label} • ${inv.paymentMethod}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                inv.isReturned -> MaterialTheme.colorScheme.onSurfaceVariant
                                statusEnum == InvoicePaymentStatus.PAID -> semantic.positiveFg
                                statusEnum == InvoicePaymentStatus.PARTIAL -> semantic.warningFg
                                else -> semantic.negativeFg
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (inv.customerPhone.isNotBlank()) {
                    Text(
                        text = "Phone: ${inv.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text("Items (${entry.items.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.quantity} x ${Formatters.formatDecimal(item.unitPrice)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Formatters.formatPkrOneDecimal(item.lineTotal, currency),
                                style = MonoCodeSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SaleDetailSummaryLine("Subtotal", Formatters.formatPkrOneDecimal(inv.subtotal, currency))
                if (inv.discountAmount > 0.0) {
                    SaleDetailSummaryLine("Discount", "-${Formatters.formatPkrOneDecimal(inv.discountAmount, currency)}")
                }
                if (inv.taxEnabled && inv.taxRatePercent > 0.0) {
                    SaleDetailSummaryLine("Tax (${Formatters.formatDecimal(inv.taxRatePercent)}%)", Formatters.formatPkrOneDecimal(inv.taxAmount, currency))
                }
                SaleDetailSummaryLine(
                    label = "Grand Total",
                    value = Formatters.formatPkrOneDecimal(inv.grandTotal, currency),
                    bold = true,
                    valueColor = MaterialTheme.colorScheme.primary
                )
                SaleDetailSummaryLine("Paid", Formatters.formatPkrOneDecimal(inv.amountPaid, currency))
                if (inv.balanceDue > 0.01 && !inv.isReturned) {
                    SaleDetailSummaryLine(
                        label = "Balance Due",
                        value = Formatters.formatPkrOneDecimal(inv.balanceDue, currency),
                        bold = true,
                        valueColor = semantic.negativeFg
                    )
                }
                if (!inv.isReturned && invoiceCost > 0.0) {
                    SaleDetailSummaryLine(
                        label = "Sale Profit",
                        value = "${if (invoiceProfit >= 0.0) "+" else ""}${Formatters.formatPkrOneDecimal(invoiceProfit, currency)}",
                        bold = true,
                        valueColor = if (invoiceProfit >= 0.0) semantic.positiveFg else semantic.negativeFg
                    )
                }
                if (inv.notes.isNotBlank()) {
                    Text(
                        text = "Note: ${inv.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasProof) {
                    OutlinedButton(
                        onClick = { showProofFull = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View Payment Proof Photo", maxLines = 1, softWrap = false)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(46.dp)
                    ) {
                        Text("Close", maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = onOpenReceipt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(46.dp)
                            .testTag("dashboard_sale_open_receipt_btn")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Receipt & Share", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    if (showProofFull && hasProof) {
        ProofImageViewerDialog(
            title = "Payment Proof #${inv.invoiceNumber}",
            imagePath = inv.proofImagePath!!,
            onDismiss = { showProofFull = false }
        )
    }
}

@Composable
private fun SaleDetailSummaryLine(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MonoFigureMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold) else MonoCodeSmall,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PnlDetailCell(
    label: String,
    value: String,
    subtext: String,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MonoFigureMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroSubMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MonoCodeSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AlertSummaryCard(
    icon: ImageVector,
    count: Int,
    label: String,
    fg: Color,
    bg: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (count > 0) bg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (count > 0) fg.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = MonoFigureLarge,
                color = if (count > 0) fg else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (count > 0) fg else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
