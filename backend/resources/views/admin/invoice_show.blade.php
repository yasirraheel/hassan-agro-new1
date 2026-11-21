@extends('admin.layout')
@section('title', 'Invoice ' . $invoice->invoice_number)

@section('content')
@php $currency = $business->currency_symbol ?? 'Rs.'; @endphp

<style>
    .inv-mode-bar { display: flex; gap: 6px; background: var(--surface-soft); padding: 4px; border-radius: 10px; border: 1px solid var(--border); }
    .inv-mode-btn { padding: 6px 12px; border: none; border-radius: 7px; font-size: 12.5px; font-weight: 700; cursor: pointer; background: transparent; color: var(--muted); }
    .inv-mode-btn.active { background: var(--primary); color: #fff; }

    /* Truly Minimal, Compact, Easy-to-Read Invoice Card */
    .a4-card {
        max-width: 560px;
        margin: 0 auto;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
    }
    .a4-accent-bar { height: 3px; background: var(--primary); }
    .a4-inner { padding: 20px 24px 18px; font-size: 13px; color: #0f172a; line-height: 1.45; }
    .a4-status-pill {
        display: inline-block;
        padding: 2px 9px;
        border-radius: 999px;
        font-size: 10.5px;
        font-weight: 800;
        letter-spacing: 0.4px;
    }
    .a4-table { width: 100%; border-collapse: collapse; font-size: 12.5px; margin-top: 4px; }
    .a4-table th { background: #f8fafc !important; color: #475569 !important; font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; padding: 7px 8px; border-bottom: 1px solid #cbd5e1; }
    .a4-table td { padding: 8px 8px; border-bottom: 1px solid #f1f5f9; color: #0f172a !important; background: transparent !important; }
    .a4-table tbody tr:nth-child(even) td { background: #f8fafc !important; }

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
            <form method="POST" action="{{ route('admin.invoices.return', $invoice->id) }}"
                  data-confirm-title="Return Sale & Restock?"
                  data-confirm-item="{{ $invoice->invoice_number }} · {{ $invoice->customer_name }}"
                  data-confirm-message="This will mark the invoice as Returned, zero its balance due, and restore all billed item quantities back into stock."
                  data-confirm-btn="Return & Restock"
                  data-confirm-variant="warn"
                  data-confirm-icon="↩️">
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

{{-- 2. COMPACT MINIMAL COLORED PDF INVOICE --}}
@php
    $statusBg = $invoice->is_returned ? '#e2e8f0' : ($invoice->payment_status === 'PAID' ? '#dcfce7' : ($invoice->payment_status === 'PARTIAL' ? '#fef3c7' : '#fee2e2'));
    $statusFg = $invoice->is_returned ? '#475569' : ($invoice->payment_status === 'PAID' ? '#15803d' : ($invoice->payment_status === 'PARTIAL' ? '#b45309' : '#b91c1c'));
    $statusTxt = $invoice->is_returned ? 'RETURNED' : strtoupper((string) $invoice->payment_status);
@endphp
<div id="minimalView" class="a4-card" style="display:none;">
    <div class="a4-accent-bar"></div>
    <div class="a4-inner">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:14px; border-bottom:1px solid #e2e8f0; padding-bottom:12px; margin-bottom:12px;">
            <div>
                <h2 style="margin:0; font-size:17px; font-weight:800; color:#0f172a; letter-spacing:-0.2px;">{{ $business->name ?? 'Business Manager' }}</h2>
                @if(!empty($business->tagline))<div style="font-size:11.5px; color:#64748b;">{{ $business->tagline }}</div>@endif
                <div style="font-size:11px; color:#64748b; margin-top:2px;">
                    {{ collect([$business->phone ?? '', $business->address ?? '', !empty($business->tax_id) ? 'Tax: ' . $business->tax_id : ''])->filter()->implode(' · ') }}
                </div>
            </div>
            <div style="text-align:right; flex-shrink:0;">
                <div style="font-size:14px; font-weight:800; color:#0f172a;">{{ $invoice->invoice_number }}</div>
                <div style="font-size:11px; color:#64748b; margin-bottom:5px;">{{ $invoice->created_at_ms > 0 ? \Carbon\Carbon::createFromTimestampMs($invoice->created_at_ms)->format('d M Y · h:i A') : '' }}</div>
                <span class="a4-status-pill" style="background:{{ $statusBg }}; color:{{ $statusFg }};">{{ $statusTxt }}</span>
            </div>
        </div>

        <div style="display:flex; justify-content:space-between; gap:12px; font-size:12px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:9px 12px; margin-bottom:12px;">
            <div>
                <div style="color:#64748b; text-transform:uppercase; font-size:9.5px; font-weight:700; letter-spacing:0.4px;">Customer</div>
                <div style="font-weight:700; color:#0f172a;">{{ $invoice->customer_name }}@if($invoice->customer_phone) <span style="font-weight:500; color:#475569;">· {{ $invoice->customer_phone }}</span>@endif</div>
            </div>
            <div style="text-align:right;">
                <div style="color:#64748b; text-transform:uppercase; font-size:9.5px; font-weight:700; letter-spacing:0.4px;">Payment &amp; Staff</div>
                <div style="color:#0f172a;">{{ $invoice->payment_method }} · {{ $invoice->pricing_tier }}@if(!empty($invoice->performed_by_name)) · <strong>{{ $invoice->performed_by_name }}</strong>@endif</div>
            </div>
        </div>

        <table class="a4-table">
            <thead>
                <tr>
                    <th style="width:24px;">#</th>
                    <th>Item</th>
                    <th style="text-align:right;">Qty</th>
                    <th style="text-align:right;">Rate</th>
                    <th style="text-align:right;">Amount</th>
                </tr>
            </thead>
            <tbody>
                @foreach($items as $idx => $item)
                    <tr>
                        <td style="color:#64748b !important;">{{ $idx + 1 }}</td>
                        <td>
                            <div style="font-weight:700; color:#0f172a;">{{ $item->product_name }}</div>
                            @php $meta = trim(($item->pack_size ?? '') . (!empty($item->batch_number) ? ' · ' . $item->batch_number : '')); @endphp
                            @if($meta !== '')<div style="font-size:10.5px; color:#64748b;">{{ $meta }}</div>@endif
                        </td>
                        <td class="num" style="text-align:right;">{{ $item->quantity }}</td>
                        <td class="num" style="text-align:right;">{{ number_format($item->unit_price, 2) }}</td>
                        <td class="num" style="text-align:right; font-weight:700;">{{ number_format($item->line_total, 2) }}</td>
                    </tr>
                @endforeach
            </tbody>
        </table>

        <div style="max-width:235px; margin-left:auto; margin-top:12px; font-size:12.5px;">
            <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#64748b;">Subtotal</span><span class="num">{{ number_format($invoice->subtotal, 2) }}</span></div>
            @if($invoice->discount_amount > 0)
                <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#64748b;">Discount</span><span class="num">-{{ number_format($invoice->discount_amount, 2) }}</span></div>
            @endif
            @if($invoice->tax_enabled)
                <div style="display:flex; justify-content:space-between; padding:3px 0;"><span style="color:#64748b;">Tax ({{ number_format($invoice->tax_rate_percent, 1) }}%)</span><span class="num">{{ number_format($invoice->tax_amount, 2) }}</span></div>
            @endif
            <div style="display:flex; justify-content:space-between; padding:8px 10px; margin:6px -10px 4px; background:#f8fafc; border:1px solid #cbd5e1; border-radius:8px; font-size:14px; font-weight:800; color:#0f172a;">
                <span>Total</span><span class="num">{{ $currency }} {{ number_format($invoice->grand_total, 2) }}</span>
            </div>
            <div style="display:flex; justify-content:space-between; padding:2px 0; font-size:12px;"><span style="color:#64748b;">Paid</span><span class="num">{{ $currency }} {{ number_format($invoice->amount_paid, 2) }}</span></div>
            @if($invoice->balance_due > 0 && !$invoice->is_returned)
                <div style="display:flex; justify-content:space-between; padding:2px 0; font-size:12.5px; font-weight:800; color:#b91c1c;"><span>Balance Due</span><span class="num">{{ $currency }} {{ number_format($invoice->balance_due, 2) }}</span></div>
            @endif
        </div>

        <div style="margin-top:14px; padding-top:10px; border-top:1px solid #e2e8f0; font-size:11px; color:#64748b; text-align:center;">
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
