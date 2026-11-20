@extends('admin.layout')
@section('title', 'POS Sales & Invoices')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.invoices') }}" style="display:flex; gap:8px; flex-wrap:wrap; flex:1;">
        <input class="input" name="q" value="{{ $search }}" placeholder="Search invoice #, customer, phone, admin..." style="max-width:300px;">
        <select name="status" onchange="this.form.submit()" style="width:auto;">
            <option value="ALL" {{ $status === 'ALL' ? 'selected' : '' }}>All Statuses</option>
            <option value="PAID" {{ $status === 'PAID' ? 'selected' : '' }}>Paid</option>
            <option value="PARTIAL" {{ $status === 'PARTIAL' ? 'selected' : '' }}>Partial</option>
            <option value="CREDIT" {{ $status === 'CREDIT' ? 'selected' : '' }}>Credit (Udhaar)</option>
            <option value="RETURNED" {{ $status === 'RETURNED' ? 'selected' : '' }}>Returned</option>
        </select>
        <button type="submit" class="btn">Filter</button>
    </form>
    <a href="{{ route('admin.invoices.export') }}" class="btn">⬇ Export CSV</a>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Invoice # &amp; Date</th>
                    <th>Customer</th>
                    <th>Performed By (Admin)</th>
                    <th>Tier / Method</th>
                    <th>Total / Due</th>
                    <th>Status</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($invoices as $inv)
                    <tr>
                        <td>
                            <a href="{{ route('admin.invoices.show', $inv->id) }}" style="color:var(--primary); font-weight:700;">{{ $inv->invoice_number }}</a>
                            <div style="font-size:12px; color:var(--muted);">
                                {{ $inv->created_at_ms > 0 ? \Carbon\Carbon::createFromTimestampMs($inv->created_at_ms)->format('d M Y, h:i A') : '—' }}
                                · {{ $bizMap[$inv->business_id]->name ?? '' }}
                            </div>
                        </td>
                        <td>
                            <strong>{{ $inv->customer_name }}</strong>
                            @if($inv->customer_phone)<div style="font-size:12px; color:var(--muted);">{{ $inv->customer_phone }}</div>@endif
                        </td>
                        <td>
                            @if(!empty($inv->performed_by_name))
                                <span class="badge badge-primary">{{ $inv->performed_by_name }}</span>
                            @else
                                <span style="color:var(--muted); font-size:12px;">—</span>
                            @endif
                        </td>
                        <td>
                            <span class="badge badge-blue">{{ $inv->pricing_tier }}</span>
                            <div style="font-size:12px; color:var(--muted); margin-top:2px;">{{ $inv->payment_method }}</div>
                        </td>
                        <td class="num">
                            <strong>Rs. {{ number_format($inv->grand_total, 0) }}</strong>
                            @if($inv->balance_due > 0 && !$inv->is_returned)
                                <div style="font-size:12px; color:var(--danger); font-weight:700;">Due: Rs. {{ number_format($inv->balance_due, 0) }}</div>
                            @endif
                        </td>
                        <td>
                            @if($inv->is_returned)<span class="badge badge-gray">RETURNED</span>
                            @elseif($inv->payment_status === 'PAID')<span class="badge badge-green">PAID</span>
                            @elseif($inv->payment_status === 'PARTIAL')<span class="badge badge-amber">PARTIAL</span>
                            @else<span class="badge badge-red">CREDIT</span>@endif
                        </td>
                        <td style="text-align:right; white-space:nowrap;">
                            <a href="{{ route('admin.invoices.show', $inv->id) }}" class="btn btn-sm">View / Print</a>
                        </td>
                    </tr>
                @empty
                    <tr><td colspan="7" style="text-align:center; color:var(--muted); padding:28px;">No POS invoices yet.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>
@endsection
