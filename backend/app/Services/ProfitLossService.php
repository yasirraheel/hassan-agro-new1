<?php

namespace App\Services;

use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class ProfitLossService
{
    public const PERIOD_LABELS = [
        'today' => 'Today',
        'week'  => 'This Week',
        'month' => 'This Month',
        'year'  => 'This Year',
        'all'   => 'All Time',
    ];

    /**
     * Matches Android ProfitLossCalculator.isStockPurchaseExpense() so Stock Purchase
     * ledger outflows are tracked separately from Operating Expenses and not double-counted
     * against COGS in Net Profit / Loss.
     */
    public function isStockPurchaseExpense(?string $category): bool
    {
        $norm = strtolower(trim((string) $category));
        if ($norm === '') {
            return false;
        }
        foreach (['stock', 'inventory', 'purchase', 'restock', 'wholesale buy'] as $kw) {
            if (str_contains($norm, $kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns [startMs, endMs] for a named period, or [null, null] for 'all'.
     */
    public function periodBounds(string $period): array
    {
        $now = Carbon::now();
        return match ($period) {
            'today' => [$now->copy()->startOfDay()->getTimestampMs(), $now->copy()->endOfDay()->getTimestampMs()],
            'week'  => [$now->copy()->startOfWeek()->getTimestampMs(), $now->copy()->endOfDay()->getTimestampMs()],
            'month' => [$now->copy()->startOfMonth()->getTimestampMs(), $now->copy()->endOfMonth()->getTimestampMs()],
            'year'  => [$now->copy()->startOfYear()->getTimestampMs(), $now->copy()->endOfYear()->getTimestampMs()],
            default => [null, null],
        };
    }

    /**
     * Full Profit & Loss + Inventory Asset Valuation summary for a business (or all businesses)
     * over an optional [startMs, endMs] time window.
     */
    public function summarize(string $selectedBizId, ?int $startMs = null, ?int $endMs = null, ?int $nowMs = null): array
    {
        $nowMs = $nowMs ?? (int) round(microtime(true) * 1000);
        $thirtyDaysMs = $nowMs + (30 * 86400 * 1000);

        $invQ = DB::table('invoices')
            ->where('is_deleted', false)
            ->where('is_returned', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId));
        if ($startMs !== null && $endMs !== null) {
            $invQ->whereBetween('created_at_ms', [$startMs, $endMs]);
        }
        $invoices = $invQ->get();

        $expQ = DB::table('expenses')
            ->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId));
        if ($startMs !== null && $endMs !== null) {
            $expQ->whereBetween('expense_date_millis', [$startMs, $endMs]);
        }
        $expenses = $expQ->get();

        $prodQ = DB::table('products')
            ->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId));
        $products = $prodQ->get();

        $productCostById = [];
        foreach ($products as $p) {
            $productCostById[(string) $p->id] = (float) ($p->purchase_price ?? 0);
        }

        // Compute COGS & units sold from invoice_items (using snapshot cost_price when > 0, fallback to product purchase_price)
        $invoiceIds = $invoices->pluck('id')->all();
        $hasCostCol = Schema::hasColumn('invoice_items', 'cost_price');
        $cogs = 0.0;
        $unitsSold = 0;

        if (!empty($invoiceIds)) {
            $items = DB::table('invoice_items')->whereIn('invoice_id', $invoiceIds)->get();
            foreach ($items as $item) {
                $qty = max(0, (int) ($item->quantity ?? 0));
                $unitsSold += $qty;
                $unitCost = $hasCostCol ? (float) ($item->cost_price ?? 0) : 0.0;
                if ($unitCost <= 0.0) {
                    $unitCost = (float) ($productCostById[(string) $item->product_id] ?? 0.0);
                }
                $cogs += ($unitCost * $qty);
            }
        }

        $totalSales = (float) $invoices->sum('grand_total');
        $totalCollected = (float) $invoices->sum('amount_paid');
        $totalCreditDue = (float) $invoices->sum('balance_due');
        $taxTotal = (float) $invoices->sum('tax_amount');
        $discountTotal = (float) $invoices->sum('discount_amount');
        $invoiceCount = $invoices->count();

        $grossProfit = $totalSales - $cogs;
        $grossMarginPercent = $totalSales > 0.0 ? ($grossProfit / $totalSales) * 100.0 : 0.0;

        $operatingExpensesList = $expenses->filter(fn ($e) => !$this->isStockPurchaseExpense($e->category ?? ''));
        $stockPurchaseList = $expenses->filter(fn ($e) => $this->isStockPurchaseExpense($e->category ?? ''));

        $operatingExpenses = (float) $operatingExpensesList->sum('amount');
        $stockPurchaseSpend = (float) $stockPurchaseList->sum('amount');
        $totalCashOutflow = $operatingExpenses + $stockPurchaseSpend;

        // True Net Profit = Gross Profit (Sales - COGS) - Operating Expenses
        $netProfit = $grossProfit - $operatingExpenses;
        $netMarginPercent = $totalSales > 0.0 ? ($netProfit / $totalSales) * 100.0 : 0.0;

        $inventoryCostValuation = (float) $products->sum(fn ($p) => max(0, (int) $p->stock_qty) * (float) ($p->purchase_price ?? 0));
        $inventoryRetailValuation = (float) $products->sum(fn ($p) => max(0, (int) $p->stock_qty) * (float) ($p->retail_price ?? 0));
        $totalProducts = $products->count();
        $totalUnits = (int) $products->sum('stock_qty');
        $lowStockCount = $products->filter(fn ($p) => (int) $p->stock_qty <= (int) $p->low_stock_threshold)->count();
        $expiredCount = $products->filter(fn ($p) => (int) $p->expiry_date_millis > 0 && (int) $p->expiry_date_millis < $nowMs)->count();
        $expiringSoonCount = $products->filter(
            fn ($p) => (int) $p->expiry_date_millis >= $nowMs && (int) $p->expiry_date_millis <= $thirtyDaysMs
        )->count();

        $expensesByCategory = $expenses->groupBy('category')->map(fn ($g) => (float) $g->sum('amount'))->sortDesc();
        $operatingByCategory = $operatingExpensesList->groupBy('category')->map(fn ($g) => (float) $g->sum('amount'))->sortDesc();

        return [
            'invoices' => $invoices,
            'expenses' => $expenses,
            'products' => $products,
            'totalSales' => $totalSales,
            'totalCollected' => $totalCollected,
            'totalCreditDue' => $totalCreditDue,
            'taxTotal' => $taxTotal,
            'discountTotal' => $discountTotal,
            'invoiceCount' => $invoiceCount,
            'unitsSold' => $unitsSold,
            'costOfGoodsSold' => $cogs,
            'grossProfit' => $grossProfit,
            'grossMarginPercent' => $grossMarginPercent,
            'operatingExpenses' => $operatingExpenses,
            'operatingExpenseCount' => $operatingExpensesList->count(),
            'stockPurchaseSpend' => $stockPurchaseSpend,
            'stockPurchaseCount' => $stockPurchaseList->count(),
            'totalCashOutflow' => $totalCashOutflow,
            'expenseCount' => $expenses->count(),
            'netProfit' => $netProfit,
            'netMarginPercent' => $netMarginPercent,
            'inventoryCostValuation' => $inventoryCostValuation,
            'inventoryRetailValuation' => $inventoryRetailValuation,
            'totalProducts' => $totalProducts,
            'totalUnits' => $totalUnits,
            'lowStockCount' => $lowStockCount,
            'expiredCount' => $expiredCount,
            'expiringSoonCount' => $expiringSoonCount,
            'expensesByCategory' => $expensesByCategory,
            'operatingByCategory' => $operatingByCategory,
        ];
    }
}
