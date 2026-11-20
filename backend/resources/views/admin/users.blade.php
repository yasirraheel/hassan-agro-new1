@extends('admin.layout')
@section('title', 'Admins & Staff Users')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <div style="color:var(--muted); font-size:13.5px;">
        Multiple admins/staff can log into the same business. Every sale, expense, and stock update records the exact admin name who performed it.
    </div>
    <button type="button" class="btn btn-primary" onclick="openUserModal()">+ Add Admin / Staff</button>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Admin / Staff Name</th>
                    <th>Role</th>
                    <th>Business Access</th>
                    <th>Status / Sessions</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @foreach($users as $u)
                    <tr>
                        <td>
                            <strong>{{ $u->name }}</strong>
                            <div style="font-size:12px; color:var(--muted);">{{ $u->email }}</div>
                        </td>
                        <td><span class="badge badge-primary">{{ $u->role_name }}</span></td>
                        <td>
                            @if(in_array('*', $u->business_ids, true))
                                <span class="badge badge-green">All Businesses</span>
                            @else
                                <div style="display:flex; flex-wrap:wrap; gap:4px;">
                                    @foreach($u->business_ids as $bId)
                                        <span class="badge badge-blue">{{ $bizMap[$bId]->name ?? $bId }}</span>
                                    @endforeach
                                </div>
                            @endif
                        </td>
                        <td>
                            @if($u->is_active)
                                <span class="badge badge-green">Active</span>
                            @else
                                <span class="badge badge-red">Deactivated</span>
                            @endif
                            <div style="font-size:11.5px; color:var(--muted); margin-top:2px;">
                                {{ $u->active_sessions }} active phone(s)
                                @if($u->last_login_ms > 0)
                                    · Last: {{ \Carbon\Carbon::createFromTimestampMs($u->last_login_ms)->format('d M, H:i') }}
                                @endif
                            </div>
                        </td>
                        <td style="text-align:right; white-space:nowrap;">
                            <button type="button" class="btn btn-sm" onclick='openUserModal(@json($u))'>Edit</button>
                            @if($u->active_sessions > 0)
                                <form method="POST" action="{{ route('admin.users.revoke', $u->id) }}" style="display:inline;">
                                    @csrf
                                    <button type="submit" class="btn btn-sm" title="Sign out all phones">Kick Phones</button>
                                </form>
                            @endif
                            @if($u->id !== 'user-super-admin')
                                <form method="POST" action="{{ route('admin.users.toggle', $u->id) }}" style="display:inline;">
                                    @csrf
                                    <button type="submit" class="btn btn-sm">{{ $u->is_active ? 'Disable' : 'Enable' }}</button>
                                </form>
                                <form method="POST" action="{{ route('admin.users.delete', $u->id) }}" style="display:inline;" onsubmit="return confirm('Delete user {{ addslashes($u->name) }}?')">
                                    @csrf
                                    @method('DELETE')
                                    <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                                </form>
                            @endif
                        </td>
                    </tr>
                @endforeach
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="userModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="userModalTitle">Add Admin / Staff User</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('userModal')">✕</button>
        </div>
        <form id="userForm" method="POST" action="{{ route('admin.users.store') }}">
            @csrf
            <input type="hidden" name="_method" id="userMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Full Name (shown on every invoice &amp; transaction) *</label>
                    <input class="input" name="name" id="usr_name" required placeholder="e.g. Ali Hassan">
                </div>
                <div class="field">
                    <label>Login Email *</label>
                    <input class="input" type="email" name="email" id="usr_email" required placeholder="ali@business.com">
                </div>
                <div class="field">
                    <label id="usr_pass_label">Password (min 6 chars) *</label>
                    <input class="input" type="password" name="password" id="usr_pass" minlength="6" placeholder="••••••••">
                </div>
                <div class="field form-full">
                    <label>Assigned Role *</label>
                    <select name="role_id" id="usr_role" required>
                        @foreach($roles as $r)
                            <option value="{{ $r->id }}">{{ $r->name }}</option>
                        @endforeach
                    </select>
                </div>
                <div class="field form-full">
                    <label style="margin-bottom:6px;">Business Access Scope</label>
                    <label class="check-item" style="margin-bottom:8px; background:var(--surface-soft);">
                        <input type="checkbox" name="all_businesses" id="usr_all_biz" value="1" onchange="toggleBizBoxes()">
                        <strong>Allow Access to All Businesses</strong>
                    </label>
                    <div class="check-grid" id="bizCheckboxes">
                        @foreach($businesses as $b)
                            <label class="check-item">
                                <input type="checkbox" name="business_ids[]" value="{{ $b->id }}" class="usr-biz-cb">
                                <span>{{ $b->name }}</span>
                            </label>
                        @endforeach
                    </div>
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('userModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save User</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const userStoreUrl = "{{ route('admin.users.store') }}";
const userBaseUrl = "{{ url('/admin/users') }}";

function toggleBizBoxes() {
    const all = document.getElementById('usr_all_biz').checked;
    document.getElementById('bizCheckboxes').style.opacity = all ? '0.45' : '1';
    document.querySelectorAll('.usr-biz-cb').forEach(cb => cb.disabled = all);
}

function openUserModal(u = null) {
    const form = document.getElementById('userForm');
    const method = document.getElementById('userMethod');
    const pass = document.getElementById('usr_pass');
    const cbs = document.querySelectorAll('.usr-biz-cb');
    if (u) {
        document.getElementById('userModalTitle').textContent = 'Edit Admin / Staff User';
        form.action = userBaseUrl + '/' + u.id;
        method.value = 'PUT';
        document.getElementById('usr_name').value = u.name || '';
        document.getElementById('usr_email').value = u.email || '';
        document.getElementById('usr_role').value = u.role_id || '';
        pass.value = '';
        pass.required = false;
        document.getElementById('usr_pass_label').textContent = 'New Password (leave blank to keep current)';
        const bizIds = u.business_ids || ['*'];
        const isAll = bizIds.includes('*');
        document.getElementById('usr_all_biz').checked = isAll;
        cbs.forEach(cb => cb.checked = !isAll && bizIds.includes(cb.value));
    } else {
        document.getElementById('userModalTitle').textContent = 'Add Admin / Staff User';
        form.action = userStoreUrl;
        method.value = 'POST';
        form.reset();
        pass.required = true;
        document.getElementById('usr_pass_label').textContent = 'Password (min 6 chars) *';
        document.getElementById('usr_all_biz').checked = false;
    }
    toggleBizBoxes();
    openModal('userModal');
}
</script>
@endpush
