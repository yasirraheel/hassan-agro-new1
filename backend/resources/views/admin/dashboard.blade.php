@extends('admin.layout')
@section('title', 'Dashboard & Profit / Loss')

@section('content')
@php
    $periodLabel = $periodLabels[$selectedPeriod] ?? 'This Month';
    $isProfit = ($pl['netProfit'] ?? 0) >= 0;
@endphp

<div class="toolbar" style="justify-content:space-between; margin-bottom:18px;">
    <div class="period-bar" role="tablist" aria-label="Time period filter">
        @foreach($periodLabels as $pKey => $pTitle)
            <a href="{{ route('admin.dashboard', ['period' => $pKey]) }}"
               class="period-pill {{ $selectedPeriod === $pKey ? 'active' : '' }}">
                {{ $pTitle }}
            </a>
        @endforeach
    </div>
    <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
        <a href="{{ route('admin.products') }}" class="btn btn-sm">📦 Manage Stock</a>
        <a href="{{ route('admin.reports') }}" class="btn btn-sm btn-primary">📈 Full P&amp;L Report</a>
    </div>
</div>

{{-- Complete Profit & Loss Summary Panel --}}
<div class="pl-hero">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:16px; flex-wrap:wrap;">
        <div>
            <div class="kpi-label">True Net Profit / Loss · {{ $periodLabel }}</div>
            <div style="display:flex; align-items:baseline; gap:12px; flex-wrap:wrap; margin-top:4px;">
                <div style="font-size:30px; font-weight:900; font-variant-numeric:tabular-nums; color:{{ $isProfit ? 'var(--success)' : 'var(--danger)' }};">
                    {{ $isProfit ? '' : '−' }}Rs. {{ number_format(abs($pl['netProfit']), 0) }}
                </div>
                <span class="badge {{ $isProfit ? 'badge-green' : 'badge-red' }}">
                    {{ $isProfit ? 'PROFIT' : 'LOSS' }} · {{ number_format($pl['netMarginPercent'], 1) }}% Net Margin
                </span>
            </div>
        </div>
        <div style="display:flex; gap:14px; flex-wrap:wrap; text-align:right;">
            <div>
                <div class="kpi-label">Gross Profit</div>
                <div class="num" style="font-size:18px; font-weight:800; color:var(--primary);">
                    Rs. {{ number_format($pl['grossProfit'], 0) }}
                </div>
                <div style="font-size:11.5px; color:var(--muted);">{{ number_format($pl['grossMarginPercent'], 1) }}% margin</div>
            </div>
            <div style="border-left:1px solid var(--border); padding-left:14px;">
                <div class="kpi-label">Stock Asset (Cost)</div>
                <div class="num" style="font-size:18px; font-weight:800; color:var(--text);">
                    Rs. {{ number_format($pl['inventoryCostValuation'], 0) }}
                </div>
                <div style="font-size:11.5px; color:var(--muted);">Retail: Rs. {{ number_format($pl['inventoryRetailValuation'], 0) }}</div>
            </div>
        </div>
    </div>

    <div class="pl-formula">
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Sales</span> <span class="num" style="color:var(--primary);">Rs. {{ number_format($pl['totalSales'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">−</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">COGS ({{ number_format($pl['unitsSold']) }} units)</span> <span class="num" style="color:var(--warn);">Rs. {{ number_format($pl['costOfGoodsSold'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">=</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Gross Profit</span> <span class="num" style="color:var(--success);">Rs. {{ number_format($pl['grossProfit'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">−</span>
        <span class="pl-chip"><span style="color:var(--muted); font-weight:600;">Operating Expenses</span> <span class="num" style="color:var(--danger);">Rs. {{ number_format($pl['operatingExpenses'], 0) }}</span></span>
        <span style="color:var(--muted); font-weight:800;">=</span>
        <span class="pl-chip" style="border-color:{{ $isProfit ? 'var(--success-border)' : 'var(--danger-border)' }}; background:{{ $isProfit ? 'var(--success-bg)' : 'var(--danger-bg)' }};">
            <span style="color:{{ $isProfit ? 'var(--success)' : 'var(--danger)' }};">Net {{ $isProfit ? 'Profit' : 'Loss' }}: Rs. {{ number_format($pl['netProfit'], 0) }}</span>
        </span>
    </div>
</div>

{{-- Detailed Accounting KPI Grid --}}
<div class="grid-4">
    <div class="card">
        <div class="kpi-label">Sales Revenue ({{ $periodLabel }})</div>
        <div class="kpi-val" style="color:var(--primary);">Rs. {{ number_format($pl['totalSales'], 0) }}</div>
        <div class="kpi-sub">{{ $pl['invoiceCount'] }} invoices · Collected Rs. {{ number_format($pl['totalCollected'], 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Cost of Goods Sold (COGS)</div>
        <div class="kpi-val" style="color:var(--warn);">Rs. {{ number_format($pl['costOfGoodsSold'], 0) }}</div>
        <div class="kpi-sub">{{ number_format($pl['unitsSold']) }} units sold · Gross Margin {{ number_format($pl['grossMarginPercent'], 1) }}%</div>
    </div>
    <div class="card">
        <div class="kpi-label">Operating Expenses</div>
        <div class="kpi-val" style="color:var(--danger);">Rs. {{ number_format($pl['operatingExpenses'], 0) }}</div>
        <div class="kpi-sub">{{ $pl['operatingExpenseCount'] }} entries (excludes stock purchases)</div>
    </div>
    <div class="card">
        <div class="kpi-label">Stock Purchased ({{ $periodLabel }})</div>
        <div class="kpi-val" style="color:var(--info);">Rs. {{ number_format($pl['stockPurchaseSpend'], 0) }}</div>
        <div class="kpi-sub">{{ $pl['stockPurchaseCount'] }} stock purchase logs · Tracked as asset</div>
    </div>
</div>

<div class="grid-3">
    <div class="card">
        <div class="kpi-label">Credit Receivable (Udhaar)</div>
        <div class="kpi-val" style="color:var(--warn);">Rs. {{ number_format($totalCreditDue, 0) }}</div>
        <div class="kpi-sub">{{ $activeStaffCount }} active admins/staff accounts</div>
    </div>
    <div class="card">
        <div class="kpi-label">Inventory Valuation (Cost → Retail)</div>
        <div class="kpi-val">Rs. {{ number_format($pl['inventoryCostValuation'], 0) }}</div>
        <div class="kpi-sub">Retail Value: <strong>Rs. {{ number_format($pl['inventoryRetailValuation'], 0) }}</strong> · Potential Gain Rs. {{ number_format(max(0, $pl['inventoryRetailValuation'] - $pl['inventoryCostValuation']), 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Inventory Health</div>
        <div class="kpi-val">{{ number_format($totalProducts) }} SKUs</div>
        <div class="kpi-sub">{{ number_format($totalUnits) }} units · {{ $lowStockCount }} low · {{ $expiredCount }} expired · {{ $expiringSoonCount }} ≤30d</div>
    </div>
</div>

<div class="grid-2">
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Recent POS Invoices ({{ $periodLabel }})</h2>
            <a href="{{ route('admin.invoices') }}" class="btn btn-sm">View All</a>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Invoice #</th>
                        <th>Customer / By Admin</th>
                        <th>Total</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($recentInvoices as $inv)
                        <tr>
                            <td>
                                <a href="{{ route('admin.invoices.show', $inv->id) }}" style="color:var(--primary); font-weight:700;">
                                    {{ $inv->invoice_number }}
                                </a>
                                <div style="font-size:11.5px; color:var(--muted);">{{ $bizMap[$inv->business_id]->name ?? 'Business' }}</div>
                            </td>
                            <td>
                                <div>{{ $inv->customer_name }}</div>
                                @if(!empty($inv->performed_by_name))
                                    <div style="font-size:11.5px; color:var(--primary); font-weight:600;">By: {{ $inv->performed_by_name }}</div>
                                @endif
                            </td>
                            <td class="num"><strong>Rs. {{ number_format($inv->grand_total, 0) }}</strong></td>
                            <td>
                                @if($inv->is_returned)
                                    <span class="badge badge-gray">RETURNED</span>
                                @elseif($inv->payment_status === 'PAID')
                                    <span class="badge badge-green">PAID</span>
                                @elseif($inv->payment_status === 'PARTIAL')
                                    <span class="badge badge-amber">PARTIAL</span>
                                @else
                                    <span class="badge badge-red">CREDIT</span>
                                @endif
                            </td>
                        </tr>
                    @empty
                        <tr><td colspan="4" style="color:var(--muted); text-align:center; padding:24px;">No invoices in this period.</td></tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>

    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Stock &amp; Expiry Alerts</h2>
            <a href="{{ route('admin.products', ['filter' => 'low']) }}" class="btn btn-sm">Manage Stock</a>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Product</th>
                        <th>Stock</th>
                        <th>Expiry</th>
                        <th>Alert</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($alertProducts as $p)
                        @php
                            $isExpired = $p->expiry_date_millis > 0 && $p->expiry_date_millis < $nowMs;
                            $isSoon = !$isExpired && $p->expiry_date_millis > 0 && $p->expiry_date_millis <= $thirtyDaysMs;
                            $isLow = $p->stock_qty <= $p->low_stock_threshold;
                        @endphp
                        <tr>
                            <td>
                                <strong>{{ $p->name }}</strong>
                                <div style="font-size:11.5px; color:var(--muted);">{{ $p->batch_number ?: ($p->brand ?: '—') }}</div>
                            </td>
                            <td class="num">{{ $p->stock_qty }}</td>
                            <td class="num">
                                {{ $p->expiry_date_millis > 0 ? \Carbon\Carbon::createFromTimestampMs($p->expiry_date_millis)->format('d M Y') : '—' }}
                            </td>
                            <td>
                                @if($isExpired)<span class="badge badge-red">EXPIRED</span>@elseif($isSoon)<span class="badge badge-amber">EXPIRING ≤30d</span>@endif
                                @if($isLow)<span class="badge badge-amber">LOW STOCK</span>@endif
                            </td>
                        </tr>
                    @empty
                        <tr><td colspan="4" style="color:var(--muted); text-align:center; padding:24px;">All stock and expiry dates look healthy.</td></tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection
