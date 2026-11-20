@extends('admin.layout')
@section('title', 'Invoice ' . $invoice->invoice_number)

@section('content')
@php $currency = $business->currency_symbol ?? 'Rs.'; @endphp

<style>
    .inv-mode-bar { display: flex; gap: 6px; background: var(--surface-soft); padding: 4px; border-radius: 10px; border: 1px solid var(--border); }
    .inv-mode-btn { padding: 6px 12px; border: none; border-radius: 7px; font-size: 12.5px; font-weight: 700; cursor: pointer; background: transparent; color: var(--muted); }
    .inv-mode-btn.active { background: var(--primary); color: #fff; }

    /* Minimal Compact A4 Style */
    .a4-card {
        max-width: 720px;
        margin: 0 auto;
        background: #fff;
        border: 1px solid #e5e7eb;
        border-radius: 10px;
        overflow: hidden;
        box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
    }
    .a4-accent-bar { height: 4px; background: var(--primary); }
    .a4-inner { padding: 24px 28px; font-size: 12.5px; color: #1f2937; }
    .a4-table { width: 100%; border-collapse: collapse; font-size: 12px; }
    .a4-table th { background: #f8fafc; color: #64748b; font-size: 10.5px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.4px; padding: 7px 8px; border-bottom: 1px solid #e5e7eb; }
    .a4-table td { padding: 7px 8px; border-bottom: 1px solid #f1f5f9; }

    /* 80mm Thermal Coupon Receipt Style with Serrated Zig-Zag Bottom */
    .thermal-wrap {
        display: none;
        max-width: 310px;
        margin: 0 auto;
        filter: drop-shadow(0 6px 16px rgba(15, 23, 42, 0.12));
    }
    .thermal-paper {
        background: #ffffff;
        color: #111827;
        font-family: "Courier New", Courier, ui-monospace, monospace;
        font-size: 12px;
        line-height: 1.4;
        padding: 18px 16px 20px;
    }
    .thermal-dash { border-top: 1px dashed #6b7280; margin: 9px 0; }
    .thermal-row { display: flex; justify-content: space-between; gap: 8px; }
    .thermal-zig-zag {
        height: 12px;
        width: 100%;
        background:
            linear-gradient(-45deg, transparent 8px, #ffffff 0) 0 0 / 12px 12px repeat-x,
            linear-gradient(45deg, transparent 8px, #ffffff 0) 0 0 / 12px 12px repeat-x;
    }
    @media print {
        .a4-card { border: none; box-shadow: none; }
        .thermal-wrap { filter: none; }
    }
</style>

<div class="toolbar no-print" style="justify-content:space-between;">
    <a href="{{ route('admin.invoices') }}" class="btn">← Back to Invoices</a>
    <div style="display:flex; gap:8px; flex-wrap:wrap; align-items:center;">
        <div class="inv-mode-bar">
            <button type="button" id="btnThermal" class="inv-mode-btn active" onclick="setInvoiceView('thermal')">🧾 Thermal Receipt</button>
            <button type="button" id="btnMinimal" class="inv-mode-btn" onclick="setInvoiceView('minimal')">📄 Minimal PDF</button>
        </div>
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

{{-- 1. 80mm THERMAL RECEIPT COUPON --}}
<div id="thermalView" class="thermal-wrap" style="display:block;">
    <div class="thermal-paper">
        <div style="text-align:center;">
            <div style="font-size:15px; font-weight:800; letter-spacing:0.4px; text-transform:uppercase;">{{ $business->name ?? 'Business Manager' }}</div>
            @if(!empty($business->tagline))<div style="font-size:10.5px; color:#4b5563;">{{ $business->tagline }}</div>@endif
            @if(!empty($business->address))<div style="font-size:10.5px; margin-top:2px;">{{ $business->address }}</div>@endif
            @if(!empty($business->phone))<div style="font-size:11px;">Tel: {{ $business->phone }}</div>@endif
            @if(!empty($business->tax_id))<div style="font-size:10.5px;">Tax ID: {{ $business->tax_id }}</div>@endif
        </div>

        <div class="thermal-dash"></div>
        <div class="thermal-row"><span>Receipt #:</span><strong>{{ $invoice->invoice_number }}</strong></div>
        <div class="thermal-row"><span>Date:</span><span>{{ $invoice->created_at_ms > 0 ? \Carbon\Carbon::createFromTimestampMs($invoice->created_at_ms)->format('d/m/y h:i A') : '' }}</span></div>
        <div class="thermal-row"><span>Customer:</span><span>{{ $invoice->customer_name }}</span></div>
        @if(!empty($invoice->customer_phone))
            <div class="thermal-row"><span>Phone:</span><span>{{ $invoice->customer_phone }}</span></div>
        @endif
        @if(!empty($invoice->performed_by_name))
            <div class="thermal-row"><span>Served By:</span><strong>{{ $invoice->performed_by_name }}</strong></div>
        @endif

        <div class="thermal-dash"></div>
        <div class="thermal-row" style="font-weight:700; font-size:11px;"><span>ITEM / QTY x RATE</span><span>AMT</span></div>
        <div class="thermal-dash"></div>

        @foreach($items as $item)
            <div style="margin-bottom:5px;">
                <div style="font-weight:700;">{{ $item->product_name }}@if(!empty($item->pack_size)) ({{ $item->pack_size }})@endif</div>
                <div class="thermal-row" style="font-size:11px; color:#374151;">
                    <span>  {{ $item->quantity }} x {{ number_format($item->unit_price, 2) }}</span>
                    <span style="font-weight:700; color:#111827;">{{ number_format($item->line_total, 2) }}</span>
                </div>
            </div>
        @endforeach

        <div class="thermal-dash"></div>
        <div class="thermal-row"><span>Subtotal</span><span>{{ number_format($invoice->subtotal, 2) }}</span></div>
        @if($invoice->discount_amount > 0)
            <div class="thermal-row"><span>Discount</span><span>-{{ number_format($invoice->discount_amount, 2) }}</span></div>
        @endif
        @if($invoice->tax_enabled)
            <div class="thermal-row"><span>Tax ({{ number_format($invoice->tax_rate_percent, 1) }}%)</span><span>{{ number_format($invoice->tax_amount, 2) }}</span></div>
        @endif
        <div class="thermal-dash"></div>
        <div class="thermal-row" style="font-size:14px; font-weight:800;">
            <span>TOTAL</span>
            <span>{{ $currency }} {{ number_format($invoice->grand_total, 2) }}</span>
        </div>
        <div class="thermal-row" style="margin-top:3px;"><span>Paid ({{ $invoice->payment_method }})</span><span>{{ $currency }} {{ number_format($invoice->amount_paid, 2) }}</span></div>
        @if($invoice->balance_due > 0 && !$invoice->is_returned)
            <div class="thermal-row" style="font-weight:800;"><span>BALANCE DUE</span><span>{{ $currency }} {{ number_format($invoice->balance_due, 2) }}</span></div>
        @endif
        <div class="thermal-dash"></div>
        <div style="text-align:center; font-size:10.5px; color:#4b5563;">
            <div>{{ $business->invoice_footer ?: 'Thank you for your business!' }}</div>
            <div style="margin-top:4px; letter-spacing:1.5px;">*** {{ $invoice->payment_status }} ***</div>
        </div>
    </div>
    <div class="thermal-zig-zag"></div>
</div>

{{-- 2. ULTRA-MINIMAL COMPACT COLORED A4 INVOICE --}}
<div id="minimalView" class="a4-card" style="display:none;">
    <div class="a4-accent-bar"></div>
    <div class="a4-inner">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:16px; border-bottom:1px solid #e5e7eb; padding-bottom:12px; margin-bottom:14px;">
            <div>
                <h2 style="margin:0; font-size:16px; font-weight:700; color:var(--primary);">{{ $business->name ?? 'Business Manager' }}</h2>
                @if(!empty($business->tagline))<div style="font-size:11px; color:#6b7280;">{{ $business->tagline }}</div>@endif
                <div style="font-size:11px; color:#6b7280; margin-top:3px;">
                    {{ $business->address ?? '' }} {{ !empty($business->phone) ? ' · ' . $business->phone : '' }} {{ !empty($business->tax_id) ? ' · Tax: ' . $business->tax_id : '' }}
                </div>
            </div>
            <div style="text-align:right;">
                <div style="font-size:13px; font-weight:700; color:#111827;">{{ $invoice->invoice_number }}</div>
                <div style="font-size:11px; color:#6b7280;">{{ $invoice->created_at_ms > 0 ? \Carbon\Carbon::createFromTimestampMs($invoice->created_at_ms)->format('d M Y, h:i A') : '' }}</div>
                @if(!empty($invoice->performed_by_name))
                    <div style="font-size:11px; color:var(--primary); font-weight:600; margin-top:2px;">By: {{ $invoice->performed_by_name }}</div>
                @endif
            </div>
        </div>

        <div style="display:flex; justify-content:space-between; font-size:11.5px; margin-bottom:14px;">
            <div>
                <span style="color:#6b7280; text-transform:uppercase; font-size:10px; font-weight:700;">Billed To</span><br>
                <strong>{{ $invoice->customer_name }}</strong>
                @if($invoice->customer_phone) <span style="color:#6b7280;">· {{ $invoice->customer_phone }}</span>@endif
            </div>
            <div style="text-align:right;">
                <span style="color:#6b7280; text-transform:uppercase; font-size:10px; font-weight:700;">Payment</span><br>
                <span>{{ $invoice->pricing_tier }} · {{ $invoice->payment_method }} · <strong>{{ $invoice->payment_status }}</strong></span>
            </div>
        </div>

        <table class="a4-table">
            <thead>
                <tr>
                    <th style="width:28px;">#</th>
                    <th>Item</th>
                    <th>Spec</th>
                    <th style="text-align:right;">Qty</th>
                    <th style="text-align:right;">Rate</th>
                    <th style="text-align:right;">Amount</th>
                </tr>
            </thead>
            <tbody>
                @foreach($items as $idx => $item)
                    <tr>
                        <td style="color:#6b7280;">{{ $idx + 1 }}</td>
                        <td style="font-weight:600;">{{ $item->product_name }}</td>
                        <td style="color:#6b7280;">{{ trim(($item->pack_size ?? '') . ' ' . ($item->batch_number ? '· ' . $item->batch_number : '')) ?: '—' }}</td>
                        <td class="num" style="text-align:right;">{{ $item->quantity }}</td>
                        <td class="num" style="text-align:right;">{{ number_format($item->unit_price, 2) }}</td>
                        <td class="num" style="text-align:right; font-weight:600;">{{ number_format($item->line_total, 2) }}</td>
                    </tr>
                @endforeach
            </tbody>
        </table>

        <div style="max-width:240px; margin-left:auto; margin-top:14px; font-size:12px;">
            <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#6b7280;">Subtotal</span><span class="num">{{ number_format($invoice->subtotal, 2) }}</span></div>
            @if($invoice->discount_amount > 0)
                <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#6b7280;">Discount</span><span class="num">-{{ number_format($invoice->discount_amount, 2) }}</span></div>
            @endif
            @if($invoice->tax_enabled)
                <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#6b7280;">Tax ({{ number_format($invoice->tax_rate_percent, 1) }}%)</span><span class="num">{{ number_format($invoice->tax_amount, 2) }}</span></div>
            @endif
            <div style="display:flex; justify-content:space-between; padding:6px 0; margin-top:4px; border-top:1px solid #d1d5db; font-size:13.5px; font-weight:700; color:var(--primary);">
                <span>Grand Total</span><span class="num">{{ $currency }} {{ number_format($invoice->grand_total, 2) }}</span>
            </div>
            <div style="display:flex; justify-content:space-between; padding:2px 0; font-size:11.5px;"><span style="color:#6b7280;">Paid</span><span class="num">{{ $currency }} {{ number_format($invoice->amount_paid, 2) }}</span></div>
            @if($invoice->balance_due > 0 && !$invoice->is_returned)
                <div style="display:flex; justify-content:space-between; padding:2px 0; font-size:12px; font-weight:700; color:var(--danger);"><span>Balance Due</span><span class="num">{{ $currency }} {{ number_format($invoice->balance_due, 2) }}</span></div>
            @endif
        </div>

        <div style="margin-top:18px; padding-top:10px; border-top:1px solid #f1f5f9; font-size:10.5px; color:#6b7280; text-align:center;">
            {{ $business->invoice_footer ?: 'Thank you for your business!' }}
        </div>
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
                <label>Amount Received — Balance Due: {{ $currency }} {{ number_format($invoice->balance_due, 2) }}</label>
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

@push('scripts')
<script>
function setInvoiceView(mode) {
    const isThermal = mode === 'thermal';
    document.getElementById('thermalView').style.display = isThermal ? 'block' : 'none';
    document.getElementById('minimalView').style.display = isThermal ? 'none' : 'block';
    document.getElementById('btnThermal').classList.toggle('active', isThermal);
    document.getElementById('btnMinimal').classList.toggle('active', !isThermal);
}
</script>
@endpush
