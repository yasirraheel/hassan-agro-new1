@extends('admin.layout')
@section('title', 'Customers & Credit (Udhaar) Ledger')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.customers') }}" style="display:flex; gap:8px; flex:1; max-width:440px;">
        <input class="input" name="q" value="{{ $search }}" placeholder="Search customer name, phone, address...">
        <button type="submit" class="btn">Search</button>
    </form>
    <button type="button" class="btn btn-primary" onclick="openCustomerModal()">+ Add Customer</button>
</div>

<div class="grid-4">
    <div class="card">
        <div class="kpi-label">Saved Customers</div>
        <div class="kpi-val">{{ $customers->count() }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Total Credit (Udhaar)</div>
        <div class="kpi-val" style="color:var(--danger);">Rs. {{ number_format($totalOutstanding, 2) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Total Billed Volume</div>
        <div class="kpi-val" style="color:var(--primary);">Rs. {{ number_format($totalBilled, 2) }}</div>
    </div>
    <div class="card">
        <div class="kpi-label">Customers With Balance</div>
        <div class="kpi-val">{{ $customersWithDue }}</div>
    </div>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Customer</th>
                    <th>Business</th>
                    <th>Preferred Tier</th>
                    <th style="text-align:right;">Total Billed</th>
                    <th style="text-align:right;">Credit / Udhaar Due</th>
                    <th>Added By</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($customers as $c)
                    <tr>
                        <td>
                            <strong>{{ $c->name }}</strong>
                            <div style="font-size:12px; color:var(--muted);">
                                {{ $c->phone ?: 'No phone' }}
                                @if(!empty($c->address)) · {{ $c->address }} @endif
                            </div>
                        </td>
                        <td><span class="badge badge-gray">{{ $bizMap[$c->business_id]->name ?? '—' }}</span></td>
                        <td><span class="badge badge-blue">{{ $c->pricing_tier }}</span></td>
                        <td class="num" style="text-align:right;">Rs. {{ number_format($c->total_billed, 2) }}</td>
                        <td class="num" style="text-align:right;">
                            @if($c->outstanding_balance > 0)
                                <strong style="color:var(--danger);">Rs. {{ number_format($c->outstanding_balance, 2) }}</strong>
                            @else
                                <span class="badge badge-green">Clear</span>
                            @endif
                        </td>
                        <td style="font-size:12px; color:var(--muted);">{{ $c->performed_by_name ?: '—' }}</td>
                        <td style="text-align:right; white-space:nowrap;">
                            <button type="button" class="btn btn-sm" onclick='openCustomerModal(@json($c))'>Edit</button>
                            <form method="POST" action="{{ route('admin.customers.delete', $c->id) }}" style="display:inline;" onsubmit="return confirm('Delete customer {{ addslashes($c->name) }}?')">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @empty
                    <tr>
                        <td colspan="7" style="text-align:center; padding:28px; color:var(--muted);">No saved customers yet. Add your first customer above or create them in Android POS!</td>
                    </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="customerModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="custModalTitle">Add Customer</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('customerModal')">✕</button>
        </div>
        <form id="customerForm" method="POST" action="{{ route('admin.customers.store') }}">
            @csrf
            <input type="hidden" name="_method" id="custMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Business *</label>
                    <select name="business_id" id="cust_biz" required>
                        @foreach($businesses as $b)
                            <option value="{{ $b->id }}" {{ $selectedBizId === $b->id ? 'selected' : '' }}>{{ $b->name }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field">
                    <label>Customer / Shop Name *</label>
                    <input class="input" name="name" id="cust_name" required placeholder="e.g. Malik Traders">
                </div>
                <div class="field">
                    <label>Phone / WhatsApp</label>
                    <input class="input" name="phone" id="cust_phone" placeholder="+92 300 1234567">
                </div>
                <div class="field">
                    <label>Email</label>
                    <input class="input" type="email" name="email" id="cust_email" placeholder="customer@email.com">
                </div>
                <div class="field">
                    <label>Preferred Pricing Tier</label>
                    <select name="pricing_tier" id="cust_tier">
                        <option value="RETAIL">Retail</option>
                        <option value="WHOLESALER">Wholesaler</option>
                        <option value="DISTRIBUTOR">Distributor</option>
                        <option value="MRP">MRP</option>
                    </select>
                </div>
                <div class="field">
                    <label>Opening Udhaar / Balance</label>
                    <input class="input" type="number" step="0.01" min="0" name="opening_balance" id="cust_opening" value="0">
                </div>
                <div class="field">
                    <label>Credit Limit (0 = No Limit)</label>
                    <input class="input" type="number" step="0.01" min="0" name="credit_limit" id="cust_limit" value="0">
                </div>
                <div class="field form-full">
                    <label>Address / Area</label>
                    <input class="input" name="address" id="cust_addr" placeholder="Shop/City/Route">
                </div>
                <div class="field form-full">
                    <label>Notes</label>
                    <input class="input" name="notes" id="cust_notes" placeholder="Optional credit terms or notes">
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('customerModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Customer</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const custStoreUrl = "{{ route('admin.customers.store') }}";
const custBaseUrl = "{{ url('/admin/customers') }}";

function openCustomerModal(c = null) {
    const form = document.getElementById('customerForm');
    const method = document.getElementById('custMethod');
    if (c) {
        document.getElementById('custModalTitle').textContent = 'Edit Customer';
        form.action = custBaseUrl + '/' + c.id;
        method.value = 'PUT';
        document.getElementById('cust_biz').value = c.business_id || '';
        document.getElementById('cust_name').value = c.name || '';
        document.getElementById('cust_phone').value = c.phone || '';
        document.getElementById('cust_email').value = c.email || '';
        document.getElementById('cust_tier').value = c.pricing_tier || 'RETAIL';
        document.getElementById('cust_opening').value = c.opening_balance ?? 0;
        document.getElementById('cust_limit').value = c.credit_limit ?? 0;
        document.getElementById('cust_addr').value = c.address || '';
        document.getElementById('cust_notes').value = c.notes || '';
    } else {
        document.getElementById('custModalTitle').textContent = 'Add Customer';
        form.action = custStoreUrl;
        method.value = 'POST';
        form.reset();
    }
    openModal('customerModal');
}
</script>
@endpush
