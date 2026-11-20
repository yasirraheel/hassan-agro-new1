@extends('admin.layout')
@section('title', 'Invoice ' . $invoice->invoice_number)

@section('content')
<div class="toolbar no-print" style="justify-content:space-between;">
    <a href="{{ route('admin.invoices') }}" class="btn">← Back to Invoices</a>
    <div style="display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="btn btn-primary" onclick="window.print()">🖨 Print / Save PDF</button>
        @if(!$invoice->is_returned && $invoice->balance_due > 0)
            <button type="button" class="btn" onclick="openModal('collectModal')">💵 Collect Credit</button>
        @endif
        @if(!$invoice->is_returned)
            <form method="POST" action="{{ route('admin.invoices.return', $invoice->id) }}" onsubmit="return confirm('Return this sale and restore product stock?')">
                @csrf
                <button type="submit" class="btn btn-danger">↩ Return &amp; Restock</button>
            </form>
        @endif
    </div>
</div>

<div class="card" style="max-width:820px; margin:0 auto;">
    <div style="display:flex; justify-content:space-between; gap:16px; flex-wrap:wrap; border-bottom:2px solid var(--primary); padding-bottom:16px; margin-bottom:18px;">
        <div>
            <h2 style="margin:0; font-size:22px; color:var(--primary);">{{ $business->name ?? 'Business Manager' }}</h2>
            @if(!empty($business->tagline))<div style="font-size:13px; color:var(--muted);">{{ $business->tagline }}</div>@endif
            @if(!empty($business->address))<div style="font-size:12.5px; color:var(--muted); margin-top:4px;">{{ $business->address }}</div>@endif
            <div style="font-size:12.5px; color:var(--muted);">{{ $business->phone ?? '' }} {{ !empty($business->tax_id) ? ' · NTN/GST: ' . $business->tax_id : '' }}</div>
        </div>
        <div style="text-align:right;">
            <div style="font-size:18px; font-weight:800;">INVOICE {{ $invoice->invoice_number }}</div>
            <div style="font-size:13px; color:var(--muted);">
                {{ $invoice->created_at_ms > 0 ? \Carbon\Carbon::createFromTimestampMs($invoice->created_at_ms)->format('d M Y, h:i A') : '' }}
            </div>
            @if(!empty($invoice->performed_by_name))
                <div style="font-size:12.5px; font-weight:700; color:var(--primary); margin-top:4px;">
                    Performed By: {{ $invoice->performed_by_name }}
                </div>
            @endif
        </div>
    </div>

    <div style="display:flex; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:18px; font-size:13.5px;">
        <div>
            <div style="font-size:11.5px; color:var(--muted); text-transform:uppercase; font-weight:700;">Billed To</div>
            <strong>{{ $invoice->customer_name }}</strong>
            @if($invoice->customer_phone)<div>{{ $invoice->customer_phone }}</div>@endif
            @if($invoice->customer_email)<div>{{ $invoice->customer_email }}</div>@endif
        </div>
        <div style="text-align:right;">
            <div style="font-size:11.5px; color:var(--muted); text-transform:uppercase; font-weight:700;">Tier / Payment</div>
            <strong>{{ $invoice->pricing_tier }} · {{ $invoice->payment_method }}</strong>
        </div>
    </div>

    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>#</th>
                    <th>Product</th>
                    <th>Spec / Batch</th>
                    <th style="text-align:right;">Qty</th>
                    <th style="text-align:right;">Unit Price</th>
                    <th style="text-align:right;">Line Total</th>
                </tr>
            </thead>
            <tbody>
                @foreach($items as $idx => $item)
                    <tr>
                        <td>{{ $idx + 1 }}</td>
                        <td><strong>{{ $item->product_name }}</strong></td>
                        <td style="color:var(--muted);">{{ trim(($item->pack_size ?? '') . ' ' . ($item->batch_number ? '· ' . $item->batch_number : '')) ?: '—' }}</td>
                        <td class="num" style="text-align:right;">{{ $item->quantity }}</td>
                        <td class="num" style="text-align:right;">{{ number_format($item->unit_price, 2) }}</td>
                        <td class="num" style="text-align:right;"><strong>{{ number_format($item->line_total, 2) }}</strong></td>
                    </tr>
                @endforeach
            </tbody>
        </table>
    </div>

    <div style="max-width:300px; margin-left:auto; margin-top:18px; font-size:14px;">
        <div style="display:flex; justify-content:space-between; padding:4px 0;"><span style="color:var(--muted);">Subtotal</span><span class="num">{{ number_format($invoice->subtotal, 2) }}</span></div>
        @if($invoice->discount_amount > 0)
            <div style="display:flex; justify-content:space-between; padding:4px 0;"><span style="color:var(--muted);">Discount</span><span class="num">- {{ number_format($invoice->discount_amount, 2) }}</span></div>
        @endif
        @if($invoice->tax_enabled)
            <div style="display:flex; justify-content:space-between; padding:4px 0;"><span style="color:var(--muted);">Tax ({{ number_format($invoice->tax_rate_percent, 1) }}%)</span><span class="num">{{ number_format($invoice->tax_amount, 2) }}</span></div>
        @endif
        <div style="display:flex; justify-content:space-between; padding:8px 0; border-top:2px solid var(--border); font-size:17px; font-weight:800; color:var(--primary);">
            <span>Grand Total</span><span class="num">Rs. {{ number_format($invoice->grand_total, 2) }}</span>
        </div>
        <div style="display:flex; justify-content:space-between; padding:4px 0; font-size:13px;"><span style="color:var(--muted);">Paid</span><span class="num">Rs. {{ number_format($invoice->amount_paid, 2) }}</span></div>
        @if($invoice->balance_due > 0 && !$invoice->is_returned)
            <div style="display:flex; justify-content:space-between; padding:4px 0; font-size:13.5px; font-weight:700; color:var(--danger);"><span>Balance Due</span><span class="num">Rs. {{ number_format($invoice->balance_due, 2) }}</span></div>
        @endif
    </div>
</div>

@if(!$invoice->is_returned && $invoice->balance_due > 0)
<div class="modal-backdrop no-print" id="collectModal">
    <div class="modal" style="max-width:420px;">
        <div class="card-header">
            <h2 class="card-title">Collect Credit Payment</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('collectModal')">✕</button>
        </div>
        <form method="POST" action="{{ route('admin.invoices.collect', $invoice->id) }}">
            @csrf
            <div class="field" style="margin-bottom:14px;">
                <label>Amount Received — Balance Due: Rs. {{ number_format($invoice->balance_due, 2) }}</label>
                <input class="input" type="number" step="0.01" min="0.01" max="{{ $invoice->balance_due }}" name="amount" value="{{ $invoice->balance_due }}" required>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn" onclick="closeModal('collectModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Record Payment</button>
            </div>
        </form>
    </div>
</div>
@endif
@endsection
