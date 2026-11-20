package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.ExpiredBg
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.ExpiringSoonAmber
import com.example.ui.theme.ExpiringSoonBg
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureLarge
import com.example.ui.theme.MonoFigureMedium
import com.example.util.Formatters
import com.example.viewmodel.InventoryFilter
import java.util.Calendar

@Composable
fun DashboardScreen(
    activeBusiness: BusinessEntity,
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
    val nowCal = remember { Calendar.getInstance() }
    val (monthStart, monthEnd) = remember {
        Formatters.monthBoundsMillis(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
    }
    val todayStart = remember { Formatters.todayStartMillis() }
    val currentMonthLabel = remember {
        Formatters.formatMonthYear(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
    }

    val monthlySales = remember(invoices) {
        invoices.filter { !it.invoice.isReturned && it.invoice.createdAt in monthStart until monthEnd }
            .sumOf { it.invoice.grandTotal }
    }
    val todaySales = remember(invoices) {
        invoices.filter { !it.invoice.isReturned && it.invoice.createdAt >= todayStart }
            .sumOf { it.invoice.grandTotal }
    }
    val totalPendingCredit = remember(invoices) {
        invoices.filter { !it.invoice.isReturned }.sumOf { it.invoice.balanceDue }
    }
    val monthlyExpenses = remember(expenses) {
        expenses.filter { it.expenseDateMillis in monthStart until monthEnd }
            .sumOf { it.amount }
    }
    val netProfit = monthlySales - monthlyExpenses
    val stockValuation = remember(products) { products.sumOf { it.stockQty * it.purchasePrice } }

    val expiredCount = remember(products) {
        products.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRED }
    }
    val expiringSoonCount = remember(products) {
        products.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRING_SOON }
    }
    val lowStockCount = remember(products) {
        products.count { it.stockQty <= it.lowStockThreshold }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Ledger Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(HeroGradientStart, HeroGradientEnd)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$currentMonthLabel Net Balance",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "Today: ${Formatters.formatCompactPkr(todaySales)}",
                                        style = MonoCodeSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatPkr(netProfit),
                                style = MonoFigureLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroSubMetric(
                                    label = "Monthly Sales",
                                    value = Formatters.formatCompactPkr(monthlySales),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroSubMetric(
                                    label = "Expenses",
                                    value = Formatters.formatCompactPkr(monthlyExpenses),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroSubMetric(
                                    label = "Stock Value",
                                    value = Formatters.formatCompactPkr(stockValuation),
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                        bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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

            // Pesticide Expiry & Stock Health Alerts
            item {
                Text(
                    text = "Pesticide Stock & Expiry Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlertSummaryCard(
                        icon = Icons.Default.EventBusy,
                        count = expiredCount,
                        label = "Expired",
                        fg = ExpiredCrimson,
                        bg = ExpiredBg,
                        testTag = "alert_card_expired",
                        onClick = { onOpenInventoryWithFilter(InventoryFilter.EXPIRED) },
                        modifier = Modifier.weight(1f)
                    )
                    AlertSummaryCard(
                        icon = Icons.Default.WarningAmber,
                        count = expiringSoonCount,
                        label = "Expiring ≤30d",
                        fg = ExpiringSoonAmber,
                        bg = ExpiringSoonBg,
                        testTag = "alert_card_expiring",
                        onClick = { onOpenInventoryWithFilter(InventoryFilter.EXPIRING_SOON) },
                        modifier = Modifier.weight(1f)
                    )
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

            // Recent Invoices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent POS Sales — ${activeBusiness.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (totalPendingCredit > 0.01)
                            "Credit Due: ${Formatters.formatCompactPkr(totalPendingCredit)}"
                        else
                            "${products.size} SKUs in stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (totalPendingCredit > 0.01) ExpiringSoonAmber
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (invoices.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = "Ready for your first sale",
                        subtitle = "Scan pesticide products into ${activeBusiness.name}'s inventory, then create 4-tier invoices with auto-tax & WhatsApp PDF sharing.",
                        actionLabel = "Scan First Product",
                        onAction = onQuickScanStock,
                        actionTestTag = "dashboard_empty_scan_btn"
                    )
                }
            } else {
                items(invoices.take(5), key = { it.invoice.id }) { entry ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInvoice(entry) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.invoice.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.invoice.invoiceNumber} • ${entry.items.size} items • ${Formatters.formatDate(entry.invoice.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatPkr(entry.invoice.grandTotal),
                                    style = MonoFigureMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PDF / Share",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun HeroSubMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MonoCodeSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
