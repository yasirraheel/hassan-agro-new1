@extends('admin.layout')
@section('title', 'Business Expenses')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <form method="GET" action="{{ route('admin.expenses') }}" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
        <input class="input" type="month" name="month" value="{{ $month }}" style="width:auto;">
        <button type="submit" class="btn">Filter Month</button>
        @if($month)<a href="{{ route('admin.expenses') }}" class="btn">Clear</a>@endif
        <span class="badge badge-red" style="font-size:13px; padding:6px 12px;">Total: Rs. {{ number_format($totalAmount, 0) }}</span>
    </form>
    <div style="display:flex; gap:8px; flex-wrap:wrap;">
        <a href="{{ route('admin.expenses.export') }}" class="btn">⬇ Export CSV</a>
        <button type="button" class="btn btn-primary" onclick="openExpenseModal()">+ Add Expense</button>
    </div>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Expense Title</th>
                    <th>Category</th>
                    <th>Performed By</th>
                    <th style="text-align:right;">Amount</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($expenses as $e)
                    @php
                        $eDate = $e->expense_date_millis > 0 ? \Carbon\Carbon::createFromTimestampMs($e->expense_date_millis)->format('Y-m-d') : date('Y-m-d');
                        $eJson = (array) $e;
                        $eJson['expense_date'] = $eDate;
                    @endphp
                    <tr>
                        <td class="num">{{ \Carbon\Carbon::parse($eDate)->format('d M Y') }}</td>
                        <td>
                            <strong>{{ $e->title }}</strong>
                            <div style="font-size:12px; color:var(--muted);">
                                {{ $bizMap[$e->business_id]->name ?? '' }} · {{ $e->payment_method }}
                                @if($e->payee) · {{ $e->payee }} @endif
                            </div>
                        </td>
                        <td><span class="badge badge-amber">{{ $e->category }}</span></td>
                        <td>
                            @if(!empty($e->performed_by_name))
                                <span class="badge badge-primary">{{ $e->performed_by_name }}</span>
                            @else
                                <span style="color:var(--muted); font-size:12px;">—</span>
                            @endif
                        </td>
                        <td class="num" style="text-align:right; color:var(--danger); font-weight:700;">Rs. {{ number_format($e->amount, 0) }}</td>
                        <td style="text-align:right; white-space:nowrap;">
                            <button type="button" class="btn btn-sm" onclick='openExpenseModal(@json($eJson))'>Edit</button>
                            <form method="POST" action="{{ route('admin.expenses.delete', $e->id) }}" style="display:inline;" onsubmit="return confirm('Delete this expense?')">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                @empty
                    <tr><td colspan="6" style="text-align:center; color:var(--muted); padding:28px;">No expenses recorded for this period.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="expenseModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="expModalTitle">Add Expense</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('expenseModal')">✕</button>
        </div>
        <form id="expForm" method="POST" action="{{ route('admin.expenses.store') }}">
            @csrf
            <input type="hidden" name="_method" id="expMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Business *</label>
                    <select name="business_id" id="exp_biz" required>
                        @foreach($businesses as $b)
                            <option value="{{ $b->id }}" {{ $selectedBizId === $b->id ? 'selected' : '' }}>{{ $b->name }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field form-full">
                    <label>Title *</label>
                    <input class="input" name="title" id="exp_title" required>
                </div>
                <div class="field">
                    <label>Category *</label>
                    <input class="input" name="category" id="exp_cat" required value="Stock Purchase">
                </div>
                <div class="field">
                    <label>Amount *</label>
                    <input class="input" type="number" step="0.01" min="0.01" name="amount" id="exp_amount" required>
                </div>
                <div class="field">
                    <label>Payment Method *</label>
                    <input class="input" name="payment_method" id="exp_method" required value="Cash">
                </div>
                <div class="field">
                    <label>Date *</label>
                    <input class="input" type="date" name="expense_date" id="exp_date" required value="{{ date('Y-m-d') }}">
                </div>
                <div class="field form-full">
                    <label>Payee / Vendor</label>
                    <input class="input" name="payee" id="exp_payee">
                </div>
                <div class="field form-full">
                    <label>Notes</label>
                    <input class="input" name="notes" id="exp_notes">
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('expenseModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Expense</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const expStoreUrl = "{{ route('admin.expenses.store') }}";
const expBaseUrl = "{{ url('/admin/expenses') }}";

function openExpenseModal(e = null) {
    const form = document.getElementById('expForm');
    const method = document.getElementById('expMethod');
    if (e) {
        document.getElementById('expModalTitle').textContent = 'Edit Expense';
        form.action = expBaseUrl + '/' + e.id;
        method.value = 'PUT';
        document.getElementById('exp_biz').value = e.business_id;
        document.getElementById('exp_title').value = e.title || '';
        document.getElementById('exp_cat').value = e.category || 'Other';
        document.getElementById('exp_amount').value = e.amount ?? 0;
        document.getElementById('exp_method').value = e.payment_method || 'Cash';
        document.getElementById('exp_date').value = e.expense_date || "{{ date('Y-m-d') }}";
        document.getElementById('exp_payee').value = e.payee || '';
        document.getElementById('exp_notes').value = e.notes || '';
    } else {
        document.getElementById('expModalTitle').textContent = 'Add Expense';
        form.action = expStoreUrl;
        method.value = 'POST';
        form.reset();
        document.getElementById('exp_cat').value = 'Stock Purchase';
        document.getElementById('exp_method').value = 'Cash';
        document.getElementById('exp_date').value = "{{ date('Y-m-d') }}";
    }
    openModal('expenseModal');
}
</script>
@endpush
