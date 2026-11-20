@extends('admin.layout')
@section('title', 'Businesses')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <div style="color:var(--muted); font-size:13.5px;">
        Add or edit businesses here — all Android devices pull updates automatically within 30 seconds.
    </div>
    <button type="button" class="btn btn-primary" onclick="openBusinessModal()">+ Add Business</button>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Business Name</th>
                    <th>Contact &amp; NTN</th>
                    <th>Invoice Prefix / Tax</th>
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
                    @endphp
                    <tr>
                        <td>
                            <strong>{{ $b->name }}</strong>
                            @if($b->tagline)
                                <div style="font-size:12px; color:var(--muted);">{{ $b->tagline }}</div>
                            @endif
                        </td>
                        <td>
                            <div>{{ $b->phone ?: '—' }}</div>
                            <div style="font-size:12px; color:var(--muted);">{{ $b->tax_id ? "NTN/GST: {$b->tax_id}" : ($b->email ?: '') }}</div>
                        </td>
                        <td>
                            <span class="badge badge-green">{{ $b->invoice_prefix }}</span>
                            <span class="badge badge-blue">{{ number_format($b->default_tax_rate, 1) }}% Tax</span>
                        </td>
                        <td class="num">{{ $productCounts[$b->id] ?? 0 }}</td>
                        <td class="num">Rs. {{ number_format((float)($invStat->rev ?? 0), 0) }}</td>
                        <td class="num">Rs. {{ number_format((float)($expenseSums[$b->id] ?? 0), 0) }}</td>
                        <td style="text-align:right; white-space:nowrap;">
                            <button type="button" class="btn btn-sm" onclick='openBusinessModal(@json($b))'>Edit</button>
                            <form method="POST" action="{{ route('admin.businesses.delete', $b->id) }}" style="display:inline;" onsubmit="return confirm('Remove {{ addslashes($b->name) }} and sync deletion to phones?')">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @empty
                    <tr><td colspan="7" style="text-align:center; color:var(--muted); padding:28px;">No businesses yet. Click "+ Add Business" or sync from your Android app.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="businessModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="bizModalTitle">Add Business</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('businessModal')">✕</button>
        </div>
        <form id="bizForm" method="POST" action="{{ route('admin.businesses.store') }}">
            @csrf
            <input type="hidden" name="_method" id="bizMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Business Name *</label>
                    <input class="input" name="name" id="biz_name" required placeholder="e.g. Hassan Agro">
                </div>
                <div class="field form-full">
                    <label>Tagline</label>
                    <input class="input" name="tagline" id="biz_tagline" placeholder="e.g. Crop Protection & Agri Solutions">
                </div>
                <div class="field">
                    <label>Phone</label>
                    <input class="input" name="phone" id="biz_phone" placeholder="+92 300 0000000">
                </div>
                <div class="field">
                    <label>Email</label>
                    <input class="input" name="email" id="biz_email" placeholder="info@hassanagro.com">
                </div>
                <div class="field">
                    <label>Invoice Prefix</label>
                    <input class="input" name="invoice_prefix" id="biz_prefix" value="HA" maxlength="16">
                </div>
                <div class="field">
                    <label>Default Tax / GST (%)</label>
                    <input class="input" type="number" step="0.01" min="0" max="100" name="default_tax_rate" id="biz_tax" value="0">
                </div>
                <div class="field form-full">
                    <label>NTN / Tax ID</label>
                    <input class="input" name="tax_id" id="biz_tax_id" placeholder="Optional NTN / STRN">
                </div>
                <div class="field form-full">
                    <label>Address</label>
                    <input class="input" name="address" id="biz_address" placeholder="Shop / Office Address">
                </div>
                <div class="field form-full">
                    <label>Invoice Footer Note</label>
                    <input class="input" name="invoice_footer" id="biz_footer" placeholder="Thank you for your business!">
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

function openBusinessModal(biz = null) {
    const form = document.getElementById('bizForm');
    const method = document.getElementById('bizMethod');
    if (biz) {
        document.getElementById('bizModalTitle').textContent = 'Edit Business';
        form.action = bizBaseUrl + '/' + biz.id;
        method.value = 'PUT';
        document.getElementById('biz_name').value = biz.name || '';
        document.getElementById('biz_tagline').value = biz.tagline || '';
        document.getElementById('biz_phone').value = biz.phone || '';
        document.getElementById('biz_email').value = biz.email || '';
        document.getElementById('biz_prefix').value = biz.invoice_prefix || 'HA';
        document.getElementById('biz_tax').value = biz.default_tax_rate ?? 0;
        document.getElementById('biz_tax_id').value = biz.tax_id || '';
        document.getElementById('biz_address').value = biz.address || '';
        document.getElementById('biz_footer').value = biz.invoice_footer || '';
    } else {
        document.getElementById('bizModalTitle').textContent = 'Add Business';
        form.action = bizStoreUrl;
        method.value = 'POST';
        form.reset();
        document.getElementById('biz_prefix').value = 'HA';
        document.getElementById('biz_tax').value = '0';
    }
    openModal('businessModal');
}
</script>
@endpush
