@extends('admin.layout')
@section('title', 'Dashboard')

@section('content')
<div class="grid-4">
    <div class="card">
        <div class="kpi-label">Total Sales Revenue</div>
        <div class="kpi-val" style="color:var(--primary);">Rs. {{ number_format($totalRevenue, 0) }}</div>
        <div class="kpi-sub">{{ $invoiceCount }} active invoices · Collected Rs. {{ number_format($totalCollected, 0) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Total Expenses</div>
        <div class="kpi-val" style="color:var(--danger);">Rs. {{ number_format($totalExpenses, 0) }}</div>
        <div class="kpi-sub">Net Profit: <strong style="color:{{ $netProfit >= 0 ? '#1b5e20' : 'var(--danger)' }};">Rs. {{ number_format($netProfit, 0) }}</strong></div>
    </div>
    <div class="card">
        <div class="kpi-label">Credit Receivable</div>
        <div class="kpi-val" style="color:var(--warn);">Rs. {{ number_format($totalCreditDue, 0) }}</div>
        <div class="kpi-sub">{{ $activeStaffCount }} active admins/staff accounts</div>
    </div>
    <div class="card">
        <div class="kpi-label">Inventory Summary</div>
        <div class="kpi-val">{{ number_format($totalProducts) }} SKUs</div>
        <div class="kpi-sub">{{ number_format($totalUnits) }} units · {{ $lowStockCount }} low · {{ $expiredCount }} expired · {{ $expiringSoonCount }} ≤30d</div>
    </div>
</div>

<div class="grid-2">
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Recent POS Invoices</h2>
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
                        <tr><td colspan="4" style="color:var(--muted); text-align:center; padding:24px;">No invoices synced yet.</td></tr>
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
