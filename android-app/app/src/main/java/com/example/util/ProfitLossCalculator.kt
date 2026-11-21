package com.example.util

import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import java.util.Calendar

enum class DashboardPeriod(val label: String, val shortLabel: String) {
    TODAY("Today", "Today"),
    THIS_WEEK("This Week", "Week"),
    THIS_MONTH("This Month", "Month"),
    THIS_YEAR("This Year", "Year"),
    ALL_TIME("All Time", "All");

    fun boundsMillis(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val end = Long.MAX_VALUE
        return when (this) {
            TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to end
            }
            THIS_WEEK -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis to end
            }
            THIS_MONTH -> {
                val (mStart, mEnd) = Formatters.monthBoundsMillis(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH)
                )
                mStart to mEnd
            }
            THIS_YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to end
            }
            ALL_TIME -> 0L to end
        }
    }
}

data class ProfitLossSummary(
    val totalSales: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val grossMarginPercent: Double,
    val operatingExpenses: Double,
    val stockPurchaseSpend: Double,
    val totalCashExpenses: Double,
    val netProfit: Double,
    val netMarginPercent: Double,
    val inventoryCostValuation: Double,
    val inventoryRetailValuation: Double,
    val invoiceCount: Int,
    val unitsSold: Int
) {
    val isLoss: Boolean get() = netProfit < -0.005
}

object ProfitLossCalculator {

    private val stockPurchaseKeywords = listOf("stock", "inventory", "purchase", "restock", "wholesale buy")

    fun isStockPurchaseExpense(category: String): Boolean {
        val lower = category.trim().lowercase()
        return stockPurchaseKeywords.any { lower.contains(it) }
    }

    /**
     * Computes true business Profit & Loss:
     * - Revenue (Total Sales): sum of non-returned invoice grandTotals in the period.
     * - COGS (Cost of Goods Sold): sum of item.costPrice * qty (falls back to product.purchasePrice if legacy item has costPrice == 0).
     * - Gross Profit = Total Sales - COGS.
     * - Operating Expenses = all period expenses NOT categorized as Stock Purchase (rent, utilities, salaries, freight, etc.).
     * - Stock Purchase Spend = expenses categorized as Stock Purchase (capital invested into inventory, tracked separately so unsold inventory does not distort P&L as a fake loss).
     * - Net Profit / Loss = Gross Profit - Operating Expenses.
     */
    fun calculate(
        products: List<ProductEntity>,
        invoices: List<InvoiceWithItems>,
        expenses: List<ExpenseEntity>,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE
    ): ProfitLossSummary {
        val productCostById = products.associate { it.id to it.purchasePrice }

        val periodInvoices = invoices.filter {
            !it.invoice.isReturned && it.invoice.createdAt in startMillis until endMillis
        }
        val periodExpenses = expenses.filter {
            it.expenseDateMillis in startMillis until endMillis
        }

        val totalSales = periodInvoices.sumOf { it.invoice.grandTotal }
        var cogs = 0.0
        var unitsSold = 0
        periodInvoices.forEach { entry ->
            entry.items.forEach { item ->
                val unitCost = if (item.costPrice > 0.0) {
                    item.costPrice
                } else {
                    productCostById[item.productId] ?: 0.0
                }
                cogs += unitCost * item.quantity
                unitsSold += item.quantity
            }
        }

        val grossProfit = totalSales - cogs
        val grossMarginPercent = if (totalSales > 0.0) (grossProfit / totalSales) * 100.0 else 0.0

        val stockPurchaseSpend = periodExpenses
            .filter { isStockPurchaseExpense(it.category) }
            .sumOf { it.amount }
        val operatingExpenses = periodExpenses
            .filterNot { isStockPurchaseExpense(it.category) }
            .sumOf { it.amount }
        val totalCashExpenses = stockPurchaseSpend + operatingExpenses

        val netProfit = grossProfit - operatingExpenses
        val netMarginPercent = if (totalSales > 0.0) (netProfit / totalSales) * 100.0 else 0.0

        val inventoryCostValuation = products.sumOf { it.stockQty.coerceAtLeast(0) * it.purchasePrice.coerceAtLeast(0.0) }
        val inventoryRetailValuation = products.sumOf {
            val sellPrice = it.retailPrice.takeIf { p -> p > 0.0 } ?: it.mrpPrice
            it.stockQty.coerceAtLeast(0) * sellPrice.coerceAtLeast(0.0)
        }

        return ProfitLossSummary(
            totalSales = totalSales,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            grossMarginPercent = grossMarginPercent,
            operatingExpenses = operatingExpenses,
            stockPurchaseSpend = stockPurchaseSpend,
            totalCashExpenses = totalCashExpenses,
            netProfit = netProfit,
            netMarginPercent = netMarginPercent,
            inventoryCostValuation = inventoryCostValuation,
            inventoryRetailValuation = inventoryRetailValuation,
            invoiceCount = periodInvoices.size,
            unitsSold = unitsSold
        )
    }
}
