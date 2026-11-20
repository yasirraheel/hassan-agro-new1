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
                    <th>Stock</th>
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
                                <form method="POST" action="{{ route('admin.products.stock', $p->id) }}">@csrf<input type="hidden" name="delta" value="-1"><button type="submit" class="btn btn-sm">−</button></form>
                                <strong class="num" style="min-width:34px; text-align:center; color:{{ $isLow ? 'var(--danger)' : 'var(--text)' }};">{{ $p->stock_qty }}</strong>
                                <form method="POST" action="{{ route('admin.products.stock', $p->id) }}">@csrf<input type="hidden" name="delta" value="10"><button type="submit" class="btn btn-sm">+10</button></form>
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
                            <form method="POST" action="{{ route('admin.products.delete', $p->id) }}" style="display:inline;" onsubmit="return confirm('Delete {{ addslashes($p->name) }}?')">
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
                    <input class="input" type="number" min="0" name="stock_qty" id="prod_stock" required value="0">
                </div>
                <div class="field">
                    <label>Low Stock Alert Qty *</label>
                    <input class="input" type="number" min="0" name="low_stock_threshold" id="prod_low" required value="10">
                </div>
                <div class="field">
                    <label>Purchase Cost</label>
                    <input class="input" type="number" step="0.01" min="0" name="purchase_price" id="prod_cost" value="0">
                </div>
                <div class="field">
                    <label>MRP Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="mrp_price" id="prod_mrp" value="0">
                </div>
                <div class="field">
                    <label>Distributor Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="distributor_price" id="prod_dist" value="0">
                </div>
                <div class="field">
                    <label>Wholesaler Price</label>
                    <input class="input" type="number" step="0.01" min="0" name="wholesaler_price" id="prod_whole" value="0">
                </div>
                <div class="field form-full">
                    <label>Retail Price *</label>
                    <input class="input" type="number" step="0.01" min="0" name="retail_price" id="prod_retail" required value="0">
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('productModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Product</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const prodStoreUrl = "{{ route('admin.products.store') }}";
const prodBaseUrl = "{{ url('/admin/products') }}";

function openProductModal(p = null) {
    const form = document.getElementById('prodForm');
    const method = document.getElementById('prodMethod');
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
    } else {
        document.getElementById('prodModalTitle').textContent = 'Add Product';
        form.action = prodStoreUrl;
        method.value = 'POST';
        form.reset();
        document.getElementById('prod_cat').value = 'General';
        document.getElementById('prod_exp').value = "{{ now()->addYear()->format('Y-m-d') }}";
        document.getElementById('prod_low').value = '10';
    }
    openModal('productModal');
}
</script>
@endpush
