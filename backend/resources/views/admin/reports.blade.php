@extends('admin.layout')
@section('title', 'Monthly Profit & Loss Reports')

@section('content')
@php
    $monthLabel = \Carbon\Carbon::createFromFormat('Y-m', $month)->format('F Y');
    $isProfit = ($pl['netProfit'] ?? 0) >= 0;
    $totalAllExp = (float) $pl['totalCashOutflow'];
@endphp

<div class="toolbar no-print" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.reports') }}" style="display:flex; gap:8px; align-items:center;">
        <input class="input" type="month" name="month" value="{{ $month }}" onchange="this.form.submit()" style="width:auto;">
        <button type="submit" class="btn">Load Month</button>
    </form>
    <div style="display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="btn btn-primary" onclick="window.print()">🖨 Print P&amp;L Report</button>
        <a href="{{ route('admin.invoices.export') }}" class="btn">⬇ Sales CSV</a>
        <a href="{{ route('admin.expenses.export') }}" class="btn">⬇ Expenses CSV</a>
        <a href="{{ route('admin.products.export') }}" class="btn">⬇ Stock CSV</a>
    </div>
</div>

{{-- P&L Statement Hero --}}
<div class="pl-hero">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:16px; flex-wrap:wrap;">
        <div>
            <div class="kpi-label">Profit &amp; Loss Statement · {{ $monthLabel }}</div>
            <div style="display:flex; align-items:baseline; gap:12px; flex-wrap:wrap; margin-top:4px;">
                <div style="font-size:28px; font-weight:900; font-variant-numeric:tabular-nums; color:{{ $isProfit ? 'var(--success)' : 'var(--danger)' }};">
                    {{ $isProfit ? '' : '−' }}Rs. {{ number_format(abs($pl['netProfit']), 0) }}
                </div>
                <span class="badge {{ $isProfit ? 'badge-green' : 'badge-red' }}">
                    Net {{ $isProfit ? 'Profit' : 'Loss' }} · {{ number_format($pl['netMarginPercent'], 1) }}% Margin
                </span>
            </div>
        </div>
        <div style="text-align:right;">
            <div class="kpi-label">Current Inventory Asset Value</div>
            <div class="num" style="font-size:18px; font-weight:800; color:var(--text);">Rs. {{ number_format($pl['inventoryCostValuation'], 0) }}</div>
            <div style="font-size:12px; color:var(--muted);">Retail Value: Rs. {{ number_format($pl['inventoryRetailValuation'], 0) }}</div>
        </div>
    </div>

    <div class="pl-formula">
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Sales</span> <span class="num" style="color:var(--primary);">Rs. {{ number_format($pl['totalSales'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">−</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">COGS ({{ number_format($pl['unitsSold']) }} units)</span> <span class="num" style="color:var(--warn);">Rs. {{ number_format($pl['costOfGoodsSold'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">=</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Gross Profit ({{ number_format($pl['grossMarginPercent'], 1) }}%)</span> <span class="num" style="color:var(--success);">Rs. {{ number_format($pl['grossProfit'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">−</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Operating Expenses</span> <span class="num" style="color:var(--danger);">Rs. {{ number_format($pl['operatingExpenses'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">=</span>
        <span class="pl-chip" style="border-color:{{ $isProfit ? 'var(--success-border)' : 'var(--danger-border)' }}; background:{{ $isProfit ? 'var(--success-bg)' : 'var(--danger-bg)' }};">
            <span style="color:{{ $isProfit ? 'var(--success)' : 'var(--danger)' }};">Net: Rs. {{ number_format($pl['netProfit'], 0) }}</span>
        </span>
    </div>
</div>

<div class="grid-4">
    <div class="card">
        <div class="kpi-label">Sales Revenue</div>
        <div class="kpi-val" style="color:var(--primary);">Rs. {{ number_format($salesTotal, 0) }}</div>
        <div class="kpi-sub">{{ $invoices->count() }} invoices · Tax Rs. {{ number_format($taxTotal, 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Cost of Goods Sold (COGS)</div>
        <div class="kpi-val" style="color:var(--warn);">Rs. {{ number_format($pl['costOfGoodsSold'], 0) }}</div>
        <div class="kpi-sub">Gross Profit: <strong style="color:var(--success);">Rs. {{ number_format($pl['grossProfit'], 0) }}</strong> ({{ number_format($pl['grossMarginPercent'], 1) }}%)</div>
    </div>
    <div class="card">
        <div class="kpi-label">Operating Expenses</div>
        <div class="kpi-val" style="color:var(--danger);">Rs. {{ number_format($pl['operatingExpenses'], 0) }}</div>
        <div class="kpi-sub">{{ $pl['operatingExpenseCount'] }} operating entries · Discounts Rs. {{ number_format($discountTotal, 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Stock Purchase Spend</div>
        <div class="kpi-val" style="color:var(--info);">Rs. {{ number_format($pl['stockPurchaseSpend'], 0) }}</div>
        <div class="kpi-sub">{{ $pl['stockPurchaseCount'] }} entries · Cash Collected Rs. {{ number_format($cashCollected, 0) }} · Due Rs. {{ number_format($creditOutstanding, 0) }}</div>
    </div>
</div>

<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:14px;">Expenses &amp; Stock Purchases by Category</h2>
        @forelse($expensesByCategory as $cat => $amt)
            @php
                $pct = $totalAllExp > 0 ? round(($amt / $totalAllExp) * 100) : 0;
                $isStockCat = str_contains(strtolower($cat), 'stock') || str_contains(strtolower($cat), 'inventory') || str_contains(strtolower($cat), 'medicine purchase');
            @endphp
            <div style="margin-bottom:12px;">
                <div style="display:flex; justify-content:space-between; align-items:center; font-size:13.5px; margin-bottom:4px; gap:8px;">
                    <span>
                        <strong>{{ $cat }}</strong>
                        @if($isStockCat)
                            <span class="badge badge-blue" style="margin-left:4px; font-size:10.5px;">Stock Asset</span>
                        @endif
                    </span>
                    <span class="num">Rs. {{ number_format($amt, 0) }} ({{ $pct }}%)</span>
                </div>
                <div style="height:8px; background:var(--bar-track); border-radius:999px; overflow:hidden;">
                    <div style="height:100%; width:{{ $pct }}%; background:{{ $isStockCat ? 'var(--info)' : 'var(--danger)' }}; border-radius:999px;"></div>
                </div>
            </div>
        @empty
            <div style="color:var(--muted); padding:16px 0;">No expenses or stock purchases recorded in {{ $monthLabel }}.</div>
        @endforelse
    </div>

    <div class="card">
        <h2 class="card-title" style="margin-bottom:14px;">Sales by Admin / Staff &amp; Tier</h2>
        <div style="margin-bottom:16px;">
            <div style="font-size:12px; font-weight:700; color:var(--muted); text-transform:uppercase; margin-bottom:8px;">By Admin / Cashier</div>
            @forelse($salesByAdmin as $adminName => $amt)
                <div style="display:flex; justify-content:space-between; padding:7px 0; border-bottom:1px solid var(--row-border); font-size:13.5px;">
                    <span>{{ $adminName }}</span>
                    <strong class="num">Rs. {{ number_format($amt, 0) }}</strong>
                </div>
            @empty
                <div style="color:var(--muted); font-size:13px;">No sales in this month.</div>
            @endforelse
        </div>
        <div>
            <div style="font-size:12px; font-weight:700; color:var(--muted); text-transform:uppercase; margin-bottom:8px;">By Pricing Tier</div>
            @forelse($salesByTier as $tier => $amt)
                <div style="display:flex; justify-content:space-between; padding:7px 0; border-bottom:1px solid var(--row-border); font-size:13.5px;">
                    <span>{{ $tier }}</span>
                    <strong class="num">Rs. {{ number_format($amt, 0) }}</strong>
                </div>
            @empty
                <div style="color:var(--muted); font-size:13px;">No sales in this month.</div>
            @endforelse
        </div>
    </div>
</div>
@endsection
