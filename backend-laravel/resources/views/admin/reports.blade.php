@extends('admin.layout')
@section('title', 'Monthly Reports')

@section('content')
<div class="toolbar no-print" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.reports') }}" style="display:flex; gap:8px; align-items:center;">
        <input class="input" type="month" name="month" value="{{ $month }}" onchange="this.form.submit()" style="width:auto;">
        <button type="submit" class="btn">Load Month</button>
    </form>
    <div style="display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="btn btn-primary" onclick="window.print()">🖨 Print Report</button>
        <a href="{{ route('admin.invoices.export') }}" class="btn">⬇ Sales CSV</a>
        <a href="{{ route('admin.expenses.export') }}" class="btn">⬇ Expenses CSV</a>
        <a href="{{ route('admin.products.export') }}" class="btn">⬇ Stock CSV</a>
    </div>
</div>

<div class="grid-4">
    <div class="card">
        <div class="kpi-label">Sales ({{ \Carbon\Carbon::createFromFormat('Y-m', $month)->format('M Y') }})</div>
        <div class="kpi-val" style="color:var(--primary);">Rs. {{ number_format($salesTotal, 0) }}</div>
        <div class="kpi-sub">{{ $invoices->count() }} invoices · Tax Rs. {{ number_format($taxTotal, 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Expenses</div>
        <div class="kpi-val" style="color:var(--danger);">Rs. {{ number_format($expensesTotal, 0) }}</div>
        <div class="kpi-sub">{{ $expenses->count() }} expense entries</div>
    </div>
    <div class="card">
        <div class="kpi-label">Net Profit / Loss</div>
        <div class="kpi-val" style="color:{{ $netProfit >= 0 ? 'var(--primary)' : 'var(--danger)' }};">
            Rs. {{ number_format($netProfit, 0) }}
        </div>
        <div class="kpi-sub">Discounts given: Rs. {{ number_format($discountTotal, 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Cash vs. Credit</div>
        <div class="kpi-val">Rs. {{ number_format($cashCollected, 0) }}</div>
        <div class="kpi-sub">Pending Credit: Rs. {{ number_format($creditOutstanding, 0) }}</div>
    </div>
</div>

<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:14px;">Expenses by Category</h2>
        @forelse($expensesByCategory as $cat => $amt)
            @php $pct = $expensesTotal > 0 ? round(($amt / $expensesTotal) * 100) : 0; @endphp
            <div style="margin-bottom:12px;">
                <div style="display:flex; justify-content:space-between; font-size:13.5px; margin-bottom:4px;">
                    <strong>{{ $cat }}</strong>
                    <span class="num">Rs. {{ number_format($amt, 0) }} ({{ $pct }}%)</span>
                </div>
                <div style="height:8px; background:#edf2ee; border-radius:999px; overflow:hidden;">
                    <div style="height:100%; width:{{ $pct }}%; background:#b91c1c; border-radius:999px;"></div>
                </div>
            </div>
        @empty
            <div style="color:var(--muted); padding:16px 0;">No expenses in this month.</div>
        @endforelse
    </div>

    <div class="card">
        <h2 class="card-title" style="margin-bottom:14px;">Sales by Pricing Tier &amp; Payment Mode</h2>
        <div style="margin-bottom:16px;">
            <div style="font-size:12px; font-weight:700; color:var(--muted); text-transform:uppercase; margin-bottom:8px;">By Pricing Tier</div>
            @forelse($salesByTier as $tier => $amt)
                <div style="display:flex; justify-content:space-between; padding:6px 0; border-bottom:1px solid #edf2ee; font-size:13.5px;">
                    <span>{{ $tier }}</span>
                    <strong class="num">Rs. {{ number_format($amt, 0) }}</strong>
                </div>
            @empty
                <div style="color:var(--muted); font-size:13px;">No sales in this month.</div>
            @endforelse
        </div>
        <div>
            <div style="font-size:12px; font-weight:700; color:var(--muted); text-transform:uppercase; margin-bottom:8px;">By Payment Method</div>
            @forelse($salesByPayment as $pm => $amt)
                <div style="display:flex; justify-content:space-between; padding:6px 0; border-bottom:1px solid #edf2ee; font-size:13.5px;">
                    <span>{{ $pm }}</span>
                    <strong class="num">Rs. {{ number_format($amt, 0) }}</strong>
                </div>
            @empty
                <div style="color:var(--muted); font-size:13px;">No sales in this month.</div>
            @endforelse
        </div>
    </div>
</div>
@endsection
