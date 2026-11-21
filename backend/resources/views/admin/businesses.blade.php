@extends('admin.layout')
@section('title', 'Businesses & Themes')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <div style="color:var(--muted); font-size:13.5px;">
        Each business has its own custom <strong>Color Theme</strong>, <strong>Business Type</strong>, <strong>Product/Expense Categories</strong>, and <strong>Expiry/Batch Tracking</strong>.
    </div>
    <button type="button" class="btn btn-primary" onclick="openBusinessModal()">+ Add Business</button>
</div>

@if(isset($unverifiedBusinesses) && $unverifiedBusinesses->isNotEmpty())
<div class="card" style="margin-bottom:18px; border-color:#F59E0B; background:rgba(245, 158, 11, 0.06);">
    <div class="card-header" style="margin-bottom:10px;">
        <div>
            <h2 class="card-title" style="color:#B45309;">Pending Email Verification ({{ $unverifiedBusinesses->count() }})</h2>
            <div style="font-size:12.5px; color:var(--muted); margin-top:2px;">
                These businesses were registered from the app, but their owner has not completed OTP verification yet. You can verify their email manually below so they can sign in right away.
            </div>
        </div>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Business Name</th>
                    <th>Owner</th>
                    <th>Email Address</th>
                    <th>Registered</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @foreach($unverifiedBusinesses as $ub)
                    <tr>
                        <td>
                            <strong>{{ $ub->name }}</strong>
                            <span class="badge badge-amber" style="margin-left:6px;">Unverified</span>
                        </td>
                        <td>{{ $ub->owner_name ?? 'Owner' }}</td>
                        <td><span class="badge badge-blue">{{ $ub->owner_email }}</span></td>
                        <td style="font-size:12.5px; color:var(--muted);">
                            {{ ($ub->created_at_ms ?? 0) > 0 ? \Carbon\Carbon::createFromTimestampMs($ub->created_at_ms)->format('d M Y, h:i A') : 'Recently' }}
                        </td>
                        <td style="text-align:right; white-space:nowrap;">
                            <form method="POST" action="{{ route('admin.businesses.verify', $ub->id) }}" style="display:inline;">
                                @csrf
                                <button type="submit" class="btn btn-sm btn-primary">Verify Email</button>
                            </form>
                            <form method="POST" action="{{ route('admin.businesses.delete', $ub->id) }}" style="display:inline;"
                                  data-confirm-title="Remove Unverified Business"
                                  data-confirm-item="{{ $ub->name }} ({{ $ub->owner_email }})"
                                  data-confirm-message="This unverified business and owner account will be removed."
                                  data-confirm-btn="Remove">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @endforeach
            </tbody>
        </table>
    </div>
</div>
@endif

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Business &amp; Type</th>
                    <th>Owner Login</th>
                    <th>Theme</th>
                    <th>Features / Currency</th>
                    <th>SKUs</th>
                    <th>Sales</th>
                    <th>Expenses</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($businesses as $b)
                    @php
                        $invStat = $invoiceCounts[$b->id] ?? null;
                        $tKey = strtoupper((string)($b->theme_key ?? 'OCEAN'));
                        $tInfo = $themes[$tKey] ?? ['label' => $tKey, 'primary' => '#0D47A1'];
                        $bObj = (array) $b;
                        $bObj['product_categories_csv'] = implode(', ', json_decode((string)($b->product_categories_json ?? ''), true) ?: []);
                        $bObj['expense_categories_csv'] = implode(', ', json_decode((string)($b->expense_categories_json ?? ''), true) ?: []);
                        $bObj['payment_methods_csv'] = implode(', ', json_decode((string)($b->payment_methods_json ?? ''), true) ?: []);
                        $bObj['pricing_rules'] = json_decode((string)($b->pricing_rules_json ?? ''), true) ?: [
                            'ownerMarginMode' => 'PERCENT',
                            'ownerMarginValue' => 5,
                            'distributorStep' => 0,
                            'wholesalerStep' => 5,
                            'retailStep' => 12,
                            'mrpStep' => 20,
                        ];
                    @endphp
                    <tr>
                        <td>
                            <strong>{{ $b->name }}</strong>
                            <div style="font-size:12px; color:var(--muted);">
                                {{ $b->business_type ?? 'GENERAL' }} · Prefix: {{ $b->invoice_prefix }} · {{ number_format($b->default_tax_rate, 1) }}% Tax
                            </div>
                        </td>
                        <td>
                            @if($b->owner_email)
                                <div style="display:flex; flex-direction:column; gap:4px; align-items:flex-start;">
                                    <span class="badge badge-blue">{{ $b->owner_email }}</span>
                                    @if(!($b->owner_is_verified ?? true))
                                        <span class="badge badge-amber">Email Unverified</span>
                                    @else
                                        <span class="badge badge-green">Verified</span>
                                    @endif
                                </div>
                            @else
                                <span class="badge badge-amber">Not configured</span>
                            @endif
                        </td>
                        <td>
                            <span class="swatch" style="background:{{ $tInfo['primary'] }};"></span>
                            <strong>{{ $tInfo['label'] }}</strong>
                        </td>
                        <td>
                            <span class="badge badge-blue">{{ $b->currency_symbol ?? 'Rs.' }}</span>
                            @if($b->track_expiry ?? true)<span class="badge badge-amber">Expiry ON</span>@endif
                            @if($b->track_batch ?? true)<span class="badge badge-gray">{{ $b->batch_field_label ?? 'Batch' }}</span>@endif
                        </td>
                        <td class="num">{{ $productCounts[$b->id] ?? 0 }}</td>
                        <td class="num">{{ $b->currency_symbol ?? 'Rs.' }} {{ number_format((float)($invStat->rev ?? 0), 0) }}</td>
                        <td class="num">{{ $b->currency_symbol ?? 'Rs.' }} {{ number_format((float)($expenseSums[$b->id] ?? 0), 0) }}</td>
                        <td style="text-align:right; white-space:nowrap;">
                            @if($b->owner_email && !($b->owner_is_verified ?? true))
                                <form method="POST" action="{{ route('admin.businesses.verify', $b->id) }}" style="display:inline;">
                                    @csrf
                                    <button type="submit" class="btn btn-sm btn-primary">Verify Email</button>
                                </form>
                            @endif
                            <button type="button" class="btn btn-sm" onclick='openBusinessModal(@json($bObj))'>Customize / Edit</button>
                            <form method="POST" action="{{ route('admin.businesses.delete', $b->id) }}" style="display:inline;"
                                  data-confirm-title="Remove Business"
                                  data-confirm-item="{{ $b->name }}"
                                  data-confirm-message="This business will be archived and hidden across the Admin Panel and synced mobile devices."
                                  data-confirm-btn="Remove Business">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @empty
                    <tr><td colspan="8" style="text-align:center; color:var(--muted); padding:28px;">No businesses yet.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="businessModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="bizModalTitle">Add &amp; Customize Business</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('businessModal')">✕</button>
        </div>
        <form id="bizForm" method="POST" action="{{ route('admin.businesses.store') }}">
            @csrf
            <input type="hidden" name="_method" id="bizMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Business Name *</label>
                    <input class="input" name="name" id="biz_name" required placeholder="e.g. City Mart / Hassan Agro / Tech Point">
                </div>
                <div class="field form-full" style="border-top:1px solid var(--border); padding-top:14px;">
                    <label style="font-weight:700;">Business Owner Login</label>
                    <div style="font-size:12px; color:var(--muted); margin-bottom:8px;">Existing business ke owner ka email/password yahan set ya reset karein. Naye owner ke liye dono zaroori hain; purana password rakhne ke liye password khali chhor dein.</div>
                </div>
                <div class="field">
                    <label>Owner Name</label>
                    <input class="input" name="owner_name" id="biz_owner_name" placeholder="Owner name">
                </div>
                <div class="field">
                    <label>Owner Email</label>
                    <input class="input" type="email" name="owner_email" id="biz_owner_email" placeholder="owner@business.com">
                </div>
                <div class="field">
                    <label>Owner Password <span style="font-weight:400; color:var(--muted);">(new/reset)</span></label>
                    <input class="input" type="password" name="owner_password" id="biz_owner_password" minlength="6" autocomplete="new-password" placeholder="6+ characters">
                </div>
                <div class="field">
                    <label>Confirm Owner Password</label>
                    <input class="input" type="password" name="owner_password_confirmation" id="biz_owner_password_confirmation" minlength="6" autocomplete="new-password">
                </div>
                <div class="field">
                    <label>Business Type Preset</label>
                    <select name="business_type" id="biz_type" onchange="applyPreset(this.value)">
                        @foreach($presets as $key => $preset)
                            <option value="{{ $key }}">{{ $preset['label'] }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field">
                    <label>App Color Theme *</label>
                    <select name="theme_key" id="biz_theme" required>
                        @foreach($themes as $tKey => $tInfo)
                            <option value="{{ $tKey }}">{{ $tInfo['label'] }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field">
                    <label>Currency Symbol</label>
                    <input class="input" name="currency_symbol" id="biz_currency" value="Rs." maxlength="12">
                </div>
                <div class="field">
                    <label>Invoice Prefix</label>
                    <input class="input" name="invoice_prefix" id="biz_prefix" value="BM" maxlength="16">
                </div>
                <div class="field">
                    <label>Default Tax / GST (%)</label>
                    <input class="input" type="number" step="0.01" min="0" max="100" name="default_tax_rate" id="biz_tax" value="0">
                </div>
                <div class="field">
                    <label>Phone</label>
                    <input class="input" name="phone" id="biz_phone" placeholder="+92 300 0000000">
                </div>
                <div class="field">
                    <label>Email</label>
                    <input class="input" name="email" id="biz_email" placeholder="owner@business.com">
                </div>
                <div class="field">
                    <label>NTN / Tax ID</label>
                    <input class="input" name="tax_id" id="biz_tax_id">
                </div>
                <div class="field form-full">
                    <label>Tagline</label>
                    <input class="input" name="tagline" id="biz_tagline">
                </div>
                <div class="field">
                    <label>Batch / Serial Field Label</label>
                    <input class="input" name="batch_field_label" id="biz_batch_label" value="Batch No.">
                </div>
                <div class="field">
                    <label>Unit / Pack Field Label</label>
                    <input class="input" name="pack_size_label" id="biz_pack_label" value="Pack / Size">
                </div>
                <div class="field form-full" style="display:flex; gap:16px; flex-wrap:wrap; padding-top:4px;">
                    <label style="display:flex; align-items:center; gap:6px; cursor:pointer; font-size:13px; color:var(--text);">
                        <input type="checkbox" name="track_expiry" id="biz_track_expiry" value="1" checked> Track Expiry Dates &amp; Alerts
                    </label>
                    <label style="display:flex; align-items:center; gap:6px; cursor:pointer; font-size:13px; color:var(--text);">
                        <input type="checkbox" name="track_batch" id="biz_track_batch" value="1" checked> Track Batch / Serial Number
                    </label>
                </div>
                <div class="field form-full">
                    <label>Product Categories (comma-separated, add, edit, or delete freely)</label>
                    <textarea name="product_categories_csv" id="biz_prod_cats" rows="2"></textarea>
                </div>
                <div class="field form-full">
                    <label>Expense Categories (comma-separated)</label>
                    <textarea name="expense_categories_csv" id="biz_exp_cats" rows="2"></textarea>
                </div>
                <div class="field form-full">
                    <label>Payment Methods (comma-separated)</label>
                    <input class="input" name="payment_methods_csv" id="biz_pay_methods">
                </div>
                <div class="field form-full" style="border-top:1px solid var(--border); padding-top:14px;">
                    <label style="font-weight:700;">Default Profit Margin &amp; 4-Tier Auto-Pricing</label>
                    <div style="font-size:12px; color:var(--muted);">Automatically populates MRP, Distributor, Wholesaler &amp; Retail prices whenever a Purchase Cost is entered.</div>
                </div>
                <div class="field">
                    <label>Profit Margin Mode</label>
                    <select name="owner_margin_mode" id="biz_margin_mode">
                        <option value="PERCENT">Percentage (%)</option>
                        <option value="FLAT">Flat Amount</option>
                    </select>
                </div>
                <div class="field">
                    <label>Base Profit Margin (% or Flat)</label>
                    <input class="input" type="number" step="0.01" min="0" name="owner_margin_value" id="biz_margin_val" value="5">
                </div>
                <div class="field">
                    <label>Distributor Step (+%)</label>
                    <input class="input" type="number" step="0.01" min="0" name="distributor_step" id="biz_dist_step" value="0">
                </div>
                <div class="field">
                    <label>Wholesaler Step (+%)</label>
                    <input class="input" type="number" step="0.01" min="0" name="wholesaler_step" id="biz_whls_step" value="5">
                </div>
                <div class="field">
                    <label>Retail Step (+%)</label>
                    <input class="input" type="number" step="0.01" min="0" name="retail_step" id="biz_retail_step" value="12">
                </div>
                <div class="field">
                    <label>MRP Step (+%)</label>
                    <input class="input" type="number" step="0.01" min="0" name="mrp_step" id="biz_mrp_step" value="20">
                </div>
                <div class="field form-full">
                    <label>Address</label>
                    <input class="input" name="address" id="biz_address">
                </div>
                <div class="field form-full">
                    <label>Invoice Footer Note</label>
                    <input class="input" name="invoice_footer" id="biz_footer">
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('businessModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Business</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const bizStoreUrl = "{{ route('admin.businesses.store') }}";
const bizBaseUrl = "{{ url('/admin/businesses') }}";
const presetsMap = @json($presets);

function applyPreset(typeKey) {
    const p = presetsMap[typeKey];
    if (!p) return;
    document.getElementById('biz_theme').value = p.default_theme || 'OCEAN';
    document.getElementById('biz_tagline').value = p.tagline || '';
    document.getElementById('biz_track_expiry').checked = !!p.track_expiry;
    document.getElementById('biz_track_batch').checked = !!p.track_batch;
    document.getElementById('biz_batch_label').value = p.batch_field_label || 'Batch No.';
    document.getElementById('biz_pack_label').value = p.pack_size_label || 'Pack / Size';
    document.getElementById('biz_prod_cats').value = (p.product_categories || []).join(', ');
    document.getElementById('biz_exp_cats').value = (p.expense_categories || []).join(', ');
    document.getElementById('biz_pay_methods').value = (p.payment_methods || []).join(', ');
}

function openBusinessModal(biz = null) {
    const form = document.getElementById('bizForm');
    const method = document.getElementById('bizMethod');
    if (biz) {
        document.getElementById('bizModalTitle').textContent = 'Customize Business';
        form.action = bizBaseUrl + '/' + biz.id;
        method.value = 'PUT';
        document.getElementById('biz_name').value = biz.name || '';
        document.getElementById('biz_type').value = biz.business_type || 'GENERAL';
        document.getElementById('biz_theme').value = (biz.theme_key || 'OCEAN').toUpperCase();
        document.getElementById('biz_currency').value = biz.currency_symbol || 'Rs.';
        document.getElementById('biz_prefix').value = biz.invoice_prefix || 'BM';
        document.getElementById('biz_tax').value = biz.default_tax_rate ?? 0;
        document.getElementById('biz_phone').value = biz.phone || '';
        document.getElementById('biz_email').value = biz.email || '';
        document.getElementById('biz_owner_name').value = biz.owner_name || '';
        document.getElementById('biz_owner_email').value = biz.owner_email || '';
        document.getElementById('biz_owner_password').value = '';
        document.getElementById('biz_owner_password_confirmation').value = '';
        document.getElementById('biz_tax_id').value = biz.tax_id || '';
        document.getElementById('biz_tagline').value = biz.tagline || '';
        document.getElementById('biz_batch_label').value = biz.batch_field_label || 'Batch No.';
        document.getElementById('biz_pack_label').value = biz.pack_size_label || 'Pack / Size';
        document.getElementById('biz_track_expiry').checked = biz.track_expiry == null ? true : !!Number(biz.track_expiry);
        document.getElementById('biz_track_batch').checked = biz.track_batch == null ? true : !!Number(biz.track_batch);
        document.getElementById('biz_prod_cats').value = biz.product_categories_csv || '';
        document.getElementById('biz_exp_cats').value = biz.expense_categories_csv || '';
        document.getElementById('biz_pay_methods').value = biz.payment_methods_csv || '';
        document.getElementById('biz_address').value = biz.address || '';
        document.getElementById('biz_footer').value = biz.invoice_footer || '';
        const pr = biz.pricing_rules || {};
        document.getElementById('biz_margin_mode').value = (pr.ownerMarginMode || 'PERCENT').toUpperCase();
        document.getElementById('biz_margin_val').value = pr.ownerMarginValue ?? 5;
        document.getElementById('biz_dist_step').value = pr.distributorStep ?? 0;
        document.getElementById('biz_whls_step').value = pr.wholesalerStep ?? 5;
        document.getElementById('biz_retail_step').value = pr.retailStep ?? 12;
        document.getElementById('biz_mrp_step').value = pr.mrpStep ?? 20;
    } else {
        document.getElementById('bizModalTitle').textContent = 'Add & Customize Business';
        form.action = bizStoreUrl;
        method.value = 'POST';
        form.reset();
        document.getElementById('biz_owner_name').value = '';
        document.getElementById('biz_owner_email').value = '';
        document.getElementById('biz_owner_password').value = '';
        document.getElementById('biz_owner_password_confirmation').value = '';
        document.getElementById('biz_type').value = 'GENERAL';
        document.getElementById('biz_currency').value = 'Rs.';
        document.getElementById('biz_prefix').value = 'BM';
        document.getElementById('biz_margin_mode').value = 'PERCENT';
        document.getElementById('biz_margin_val').value = '5';
        document.getElementById('biz_dist_step').value = '0';
        document.getElementById('biz_whls_step').value = '5';
        document.getElementById('biz_retail_step').value = '12';
        document.getElementById('biz_mrp_step').value = '20';
        applyPreset('GENERAL');
    }
    openModal('businessModal');
}
</script>
@endpush
