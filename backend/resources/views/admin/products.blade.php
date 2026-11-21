@extends('admin.layout')
@section('title', 'Inventory & Stock')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.products') }}" style="display:flex; gap:8px; flex-wrap:wrap; align-items:center; flex:1;">
        <input class="input" name="q" value="{{ $search }}" placeholder="Search product, brand, SKU, batch, admin..." style="max-width:300px;">
        <select name="filter" onchange="this.form.submit()" style="width:auto;">
            <option value="all" {{ $filter === 'all' ? 'selected' : '' }}>All Stock</option>
            <option value="low" {{ $filter === 'low' ? 'selected' : '' }}>Low Stock</option>
            <option value="expiring" {{ $filter === 'expiring' ? 'selected' : '' }}>Expiring ≤ 30d</option>
            <option value="expired" {{ $filter === 'expired' ? 'selected' : '' }}>Expired</option>
        </select>
        <button type="submit" class="btn">Filter</button>
    </form>
    <div style="display:flex; gap:8px; flex-wrap:wrap;">
        <a href="{{ route('admin.products.export') }}" class="btn">⬇ Export CSV</a>
        <button type="button" class="btn btn-primary" onclick="openProductModal()">+ Add Product</button>
    </div>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Product &amp; Batch</th>
                    <th>Category</th>
                    <th>Expiry</th>
                    <th>Stock &amp; Cost Value</th>
                    <th>4-Tier Prices</th>
                    <th>Last Updated By</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($products as $p)
                    @php
                        $expDate = $p->expiry_date_millis > 0 ? \Carbon\Carbon::createFromTimestampMs($p->expiry_date_millis)->format('Y-m-d') : '';
                        $expLabel = $p->expiry_date_millis > 0 ? \Carbon\Carbon::createFromTimestampMs($p->expiry_date_millis)->format('d M Y') : '—';
                        $isExpired = $p->expiry_date_millis > 0 && $p->expiry_date_millis < $nowMs;
                        $isSoon = !$isExpired && $p->expiry_date_millis > 0 && $p->expiry_date_millis <= $thirtyDaysMs;
                        $isLow = $p->stock_qty <= $p->low_stock_threshold;
                        $stockValue = max(0, (int) $p->stock_qty) * (float) ($p->purchase_price ?? 0);
                        $pJson = (array) $p;
                        $pJson['expiry_date'] = $expDate;
                    @endphp
                    <tr>
                        <td>
                            <strong>{{ $p->name }}</strong>
                            @if($p->pack_size)<span class="badge badge-gray" style="margin-left:4px;">{{ $p->pack_size }}</span>@endif
                            <div style="font-size:12px; color:var(--muted);">
                                {{ $bizMap[$p->business_id]->name ?? 'Business' }}
                                @if($p->brand) · {{ $p->brand }} @endif
                                @if($p->batch_number) · {{ $p->batch_number }} @endif
                                @if($p->barcode) · SKU: {{ $p->barcode }} @endif
                            </div>
                        </td>
                        <td><span class="badge badge-blue">{{ $p->category }}</span></td>
                        <td class="num">
                            <div>{{ $expLabel }}</div>
                            @if($isExpired)<span class="badge badge-red">EXPIRED</span>
                            @elseif($isSoon)<span class="badge badge-amber">≤ 30 DAYS</span>
                            @else<span class="badge badge-green">VALID</span>@endif
                        </td>
                        <td>
                            <div style="display:flex; align-items:center; gap:6px;">
                                <form method="POST" action="{{ route('admin.products.stock', $p->id) }}">@csrf<input type="hidden" name="delta" value="-1"><button type="submit" class="btn btn-sm" title="Decrease by 1">−</button></form>
                                <strong class="num" style="min-width:34px; text-align:center; color:{{ $isLow ? 'var(--danger)' : 'var(--text)' }};">{{ $p->stock_qty }}</strong>
                                <button type="button" class="btn btn-sm" onclick='openRestockModal(@json($pJson))' title="Restock & record stock purchase cost">+ Restock</button>
                            </div>
                            <div style="font-size:11.5px; color:var(--muted); margin-top:3px;" class="num">
                                Cost: {{ number_format((float)$p->purchase_price, 0) }}/u · Asset: <strong>Rs. {{ number_format($stockValue, 0) }}</strong>
                            </div>
                        </td>
                        <td style="font-size:12px; line-height:1.5;" class="num">
                            <div><strong>Retail:</strong> {{ number_format($p->retail_price, 0) }} · <strong>MRP:</strong> {{ number_format($p->mrp_price, 0) }}</div>
                            <div style="color:var(--muted);"><strong>Whls:</strong> {{ number_format($p->wholesaler_price, 0) }} · <strong>Dist:</strong> {{ number_format($p->distributor_price, 0) }}</div>
                        </td>
                        <td>
                            @if(!empty($p->performed_by_name))
                                <span class="badge badge-primary">{{ $p->performed_by_name }}</span>
                            @else
                                <span style="color:var(--muted); font-size:12px;">—</span>
                            @endif
                        </td>
                        <td style="text-align:right; white-space:nowrap;">
                            <button type="button" class="btn btn-sm" onclick='openProductModal(@json($pJson))'>Edit</button>
                            <form method="POST" action="{{ route('admin.products.delete', $p->id) }}" style="display:inline;"
                                  data-confirm-title="Delete Product"
                                  data-confirm-item="{{ $p->name }}"
                                  data-confirm-message="This product will be removed from inventory and synced to connected phones. Existing invoices remain intact."
                                  data-confirm-btn="Delete Product">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @empty
                    <tr><td colspan="7" style="text-align:center; color:var(--muted); padding:28px;">No inventory items found.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

{{-- Add / Edit Product Modal with Stock Purchase Accounting --}}
<div class="modal-backdrop" id="productModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="prodModalTitle">Add Product</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('productModal')">✕</button>
        </div>
        <form id="prodForm" method="POST" action="{{ route('admin.products.store') }}">
            @csrf
            <input type="hidden" name="_method" id="prodMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Business *</label>
                    <select name="business_id" id="prod_biz" required>
                        @foreach($businesses as $b)
                            <option value="{{ $b->id }}" {{ $selectedBizId === $b->id ? 'selected' : '' }}>{{ $b->name }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field form-full">
                    <label>Product Name *</label>
                    <input class="input" name="name" id="prod_name" required>
                </div>
                <div class="field">
                    <label>Brand / Manufacturer</label>
                    <input class="input" name="brand" id="prod_brand">
                </div>
                <div class="field">
                    <label>Unit / Pack Size</label>
                    <input class="input" name="pack_size" id="prod_pack" placeholder="e.g. 1L / 500g / 1 Pc">
                </div>
                <div class="field">
                    <label>Category *</label>
                    <input class="input" name="category" id="prod_cat" required value="General" placeholder="Any business category">
                </div>
                <div class="field">
                    <label>Expiry Date *</label>
                    <input class="input" type="date" name="expiry_date" id="prod_exp" required value="{{ now()->addYear()->format('Y-m-d') }}">
                </div>
                <div class="field">
                    <label>Barcode / SKU</label>
                    <input class="input" name="barcode" id="prod_barcode">
                </div>
                <div class="field">
                    <label>Batch / Serial No.</label>
                    <input class="input" name="batch_number" id="prod_batch">
                </div>
                <div class="field">
                    <label>Stock Qty *</label>
                    <input class="input" type="number" min="0" name="stock_qty" id="prod_stock" required value="0" oninput="updateStockPurchaseSummary()">
                </div>
                <div class="field">
                    <label>Low Stock Alert Qty *</label>
                    <input class="input" type="number" min="0" name="low_stock_threshold" id="prod_low" required value="10">
                </div>
                <div class="field form-full">
                    <label>Purchase Cost (Per Unit)</label>
                    <div style="display:flex; gap:8px; align-items:center;">
                        <input class="input" type="number" step="0.01" min="0" name="purchase_price" id="prod_cost" value="0" oninput="autoCalcTierPrices(false); updateStockPurchaseSummary();">
                        <button type="button" class="btn btn-sm" onclick="autoCalcTierPrices(true)" style="white-space:nowrap;">Recalculate 4 Tiers</button>
                    </div>
                    <div id="prod_margin_hint" style="font-size:12px; color:var(--primary); margin-top:4px;"></div>
                </div>

                {{-- Stock Purchase Accounting Preview (for new product creation) --}}
                <div class="field form-full" id="stockAccountingBox" style="background:var(--surface-soft); border:1px solid var(--border); border-radius:11px; padding:12px 14px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:8px;">
                        <span style="font-size:12px; font-weight:700; color:var(--muted); text-transform:uppercase;">Total Stock Purchase Cost (Qty × Unit Cost)</span>
                        <strong id="prod_total_cost_preview" class="num" style="font-size:16px; color:var(--primary);">Rs. 0</strong>
                    </div>
                    <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
                        <label style="display:flex; align-items:center; gap:7px; margin:0; cursor:pointer; font-size:13px; color:var(--text);">
                            <input type="checkbox" name="record_stock_expense" id="prod_record_expense" value="1" checked>
                            <span>Log as <strong>Stock Purchase</strong> spend in Expenses Ledger</span>
                        </label>
                        <select name="purchase_payment_method" id="prod_pay_method" style="width:auto; padding:6px 10px; font-size:12.5px;">
                            <option value="Cash">Cash</option>
                            <option value="Bank Transfer">Bank Transfer</option>
                            <option value="JazzCash / EasyPaisa">JazzCash / EasyPaisa</option>
                            <option value="Cheque">Cheque</option>
                        </select>
                    </div>
                </div>

                <div class="field">
                    <label>MRP Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="mrp_price" id="prod_mrp" value="0" oninput="markTierManual('mrp')">
                </div>
                <div class="field">
                    <label>Distributor Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="distributor_price" id="prod_dist" value="0" oninput="markTierManual('dist')">
                </div>
                <div class="field">
                    <label>Wholesaler Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="wholesaler_price" id="prod_whole" value="0" oninput="markTierManual('whole')">
                </div>
                <div class="field">
                    <label>Retail Price *</label>
                    <input class="input" type="number" step="0.01" min="0" name="retail_price" id="prod_retail" required value="0" oninput="markTierManual('retail')">
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('productModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Product</button>
            </div>
        </form>
    </div>
</div>

{{-- Restock Modal with Stock Purchase Accounting --}}
<div class="modal-backdrop" id="restockModal">
    <div class="modal" style="max-width:460px;">
        <div class="card-header">
            <h2 class="card-title">Restock Inventory</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('restockModal')">✕</button>
        </div>
        <form id="restockForm" method="POST" action="">
            @csrf
            <div style="margin-bottom:12px;">
                <strong id="restockProdName" style="font-size:15px;"></strong>
                <div id="restockCurrentQty" style="font-size:12.5px; color:var(--muted);"></div>
            </div>
            <div class="form-grid">
                <div class="field">
                    <label>Quantity to Add *</label>
                    <input class="input" type="number" min="1" name="delta" id="restock_qty" value="10" required oninput="updateRestockTotal()">
                </div>
                <div class="field">
                    <label>Unit Purchase Cost *</label>
                    <input class="input" type="number" step="0.01" min="0" name="unit_cost" id="restock_cost" value="0" required oninput="updateRestockTotal()">
                </div>
                <div class="field form-full" style="background:var(--surface-soft); border:1px solid var(--border); border-radius:11px; padding:12px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                        <span style="font-size:12px; font-weight:700; color:var(--muted);">TOTAL RESTOCK COST</span>
                        <strong id="restock_total_preview" class="num" style="font-size:16px; color:var(--primary);">Rs. 0</strong>
                    </div>
                    <label style="display:flex; align-items:center; gap:7px; margin:0 0 8px; cursor:pointer; font-size:13px; color:var(--text);">
                        <input type="checkbox" name="record_stock_expense" id="restock_record_exp" value="1" checked>
                        <span>Record as <strong>Stock Purchase</strong> spend in Expenses</span>
                    </label>
                    <select name="purchase_payment_method" id="restock_pay_method">
                        <option value="Cash">Cash</option>
                        <option value="Bank Transfer">Bank Transfer</option>
                        <option value="JazzCash / EasyPaisa">JazzCash / EasyPaisa</option>
                        <option value="Cheque">Cheque</option>
                    </select>
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:16px;">
                <button type="button" class="btn" onclick="closeModal('restockModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Confirm Restock</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
@php
    $bizPricingRulesMap = [];
    foreach ($businesses as $b) {
        $decodedRules = !empty($b->pricing_rules_json) ? json_decode((string) $b->pricing_rules_json, true) : null;
        $bizPricingRulesMap[$b->id] = is_array($decodedRules) ? $decodedRules : [
            'ownerMarginMode' => 'PERCENT',
            'ownerMarginValue' => 5,
            'distributorStep' => 0,
            'wholesalerStep' => 5,
            'retailStep' => 12,
            'mrpStep' => 20,
        ];
    }
@endphp
<script>
const prodStoreUrl = "{{ route('admin.products.store') }}";
const prodBaseUrl = "{{ url('/admin/products') }}";
const bizPricingRules = @json($bizPricingRulesMap);
let manualTiers = { mrp: false, dist: false, whole: false, retail: false };

function markTierManual(key) {
    manualTiers[key] = true;
}

function getSelectedPricingRules() {
    const bizId = document.getElementById('prod_biz').value;
    const raw = bizPricingRules[bizId] || {};
    return {
        mode: String(raw.ownerMarginMode || 'PERCENT').toUpperCase(),
        margin: Math.max(0, parseFloat(raw.ownerMarginValue ?? 5) || 0),
        dist: Math.max(0, parseFloat(raw.distributorStep ?? 0) || 0),
        whole: Math.max(0, parseFloat(raw.wholesalerStep ?? 5) || 0),
        retail: Math.max(0, parseFloat(raw.retailStep ?? 12) || 0),
        mrp: Math.max(0, parseFloat(raw.mrpStep ?? 20) || 0),
    };
}

function updateMarginHint() {
    const r = getSelectedPricingRules();
    const hint = document.getElementById('prod_margin_hint');
    if (hint) {
        const baseTxt = r.mode === 'FLAT' ? `+${r.margin} flat` : `+${r.margin}%`;
        hint.textContent = `Auto-pricing from Business Settings (${baseTxt} margin • Dist +${r.dist}% • Whls +${r.whole}% • Retail +${r.retail}% • MRP +${r.mrp}%)`;
    }
}

function updateStockPurchaseSummary() {
    const qty = Math.max(0, parseInt(document.getElementById('prod_stock').value, 10) || 0);
    const cost = Math.max(0, parseFloat(document.getElementById('prod_cost').value) || 0);
    const total = qty * cost;
    const el = document.getElementById('prod_total_cost_preview');
    if (el) {
        el.textContent = 'Rs. ' + total.toLocaleString(undefined, { maximumFractionDigits: 2 });
    }
}

function autoCalcTierPrices(forceAll = false) {
    updateMarginHint();
    const cost = parseFloat(document.getElementById('prod_cost').value) || 0;
    if (cost <= 0) return;
    const r = getSelectedPricingRules();
    const baseFloor = r.mode === 'FLAT' ? (cost + r.margin) : (cost * (1 + r.margin / 100));
    const round2 = (v) => Math.round(v * 100) / 100;

    if (forceAll || !manualTiers.dist) {
        document.getElementById('prod_dist').value = round2(baseFloor * (1 + r.dist / 100));
        if (forceAll) manualTiers.dist = false;
    }
    if (forceAll || !manualTiers.whole) {
        document.getElementById('prod_whole').value = round2(baseFloor * (1 + r.whole / 100));
        if (forceAll) manualTiers.whole = false;
    }
    if (forceAll || !manualTiers.retail) {
        document.getElementById('prod_retail').value = round2(baseFloor * (1 + r.retail / 100));
        if (forceAll) manualTiers.retail = false;
    }
    if (forceAll || !manualTiers.mrp) {
        document.getElementById('prod_mrp').value = round2(baseFloor * (1 + r.mrp / 100));
        if (forceAll) manualTiers.mrp = false;
    }
}

document.getElementById('prod_biz')?.addEventListener('change', () => autoCalcTierPrices(false));

function openProductModal(p = null) {
    const form = document.getElementById('prodForm');
    const method = document.getElementById('prodMethod');
    const accountingBox = document.getElementById('stockAccountingBox');
    if (p) {
        document.getElementById('prodModalTitle').textContent = 'Edit Product';
        form.action = prodBaseUrl + '/' + p.id;
        method.value = 'PUT';
        document.getElementById('prod_biz').value = p.business_id;
        document.getElementById('prod_name').value = p.name || '';
        document.getElementById('prod_brand').value = p.brand || '';
        document.getElementById('prod_pack').value = p.pack_size || '';
        document.getElementById('prod_cat').value = p.category || 'General';
        document.getElementById('prod_exp').value = p.expiry_date || '';
        document.getElementById('prod_barcode').value = p.barcode || '';
        document.getElementById('prod_batch').value = p.batch_number || '';
        document.getElementById('prod_stock').value = p.stock_qty ?? 0;
        document.getElementById('prod_low').value = p.low_stock_threshold ?? 10;
        document.getElementById('prod_cost').value = p.purchase_price ?? 0;
        document.getElementById('prod_mrp').value = p.mrp_price ?? 0;
        document.getElementById('prod_dist').value = p.distributor_price ?? 0;
        document.getElementById('prod_whole').value = p.wholesaler_price ?? 0;
        document.getElementById('prod_retail').value = p.retail_price ?? 0;
        if (accountingBox) accountingBox.style.display = 'none';
        manualTiers = { mrp: true, dist: true, whole: true, retail: true };
    } else {
        document.getElementById('prodModalTitle').textContent = 'Add Product';
        form.action = prodStoreUrl;
        method.value = 'POST';
        form.reset();
        document.getElementById('prod_cat').value = 'General';
        document.getElementById('prod_exp').value = "{{ now()->addYear()->format('Y-m-d') }}";
        document.getElementById('prod_low').value = '10';
        document.getElementById('prod_record_expense').checked = true;
        if (accountingBox) accountingBox.style.display = 'block';
        manualTiers = { mrp: false, dist: false, whole: false, retail: false };
    }
    updateMarginHint();
    updateStockPurchaseSummary();
    openModal('productModal');
}

function updateRestockTotal() {
    const qty = Math.max(0, parseInt(document.getElementById('restock_qty').value, 10) || 0);
    const cost = Math.max(0, parseFloat(document.getElementById('restock_cost').value) || 0);
    document.getElementById('restock_total_preview').textContent = 'Rs. ' + (qty * cost).toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function openRestockModal(p) {
    document.getElementById('restockForm').action = prodBaseUrl + '/' + p.id + '/stock';
    document.getElementById('restockProdName').textContent = p.name || 'Product';
    document.getElementById('restockCurrentQty').textContent = `Current stock: ${p.stock_qty ?? 0} units`;
    document.getElementById('restock_qty').value = 10;
    document.getElementById('restock_cost').value = p.purchase_price ?? 0;
    document.getElementById('restock_record_exp').checked = true;
    updateRestockTotal();
    openModal('restockModal');
}
</script>
@endpush
