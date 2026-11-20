@extends('admin.layout')
@section('title', 'Roles & Feature Permissions')

@section('content')
<div class="toolbar" style="justify-content:space-between;">
    <div style="color:var(--muted); font-size:13.5px;">
        Create custom Roles and choose exactly which app features each Role can access. Updated permissions sync to Android phones automatically.
    </div>
    <button type="button" class="btn btn-primary" onclick="openRoleModal()">+ Create Role</button>
</div>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Role Name</th>
                    <th>Assigned Users</th>
                    <th>Allowed App Features</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @foreach($roles as $role)
                    <tr>
                        <td>
                            <strong>{{ $role->name }}</strong>
                            @if($role->is_system)<span class="badge badge-primary" style="margin-left:4px;">System</span>@endif
                            @if(!empty($role->business_id_val))
                                <span class="badge badge-amber" style="margin-left:4px;">🏢 {{ $role->business_label }}</span>
                            @endif
                            @if($role->description)
                                <div style="font-size:12px; color:var(--muted);">{{ $role->description }}</div>
                            @endif
                        </td>
                        <td class="num"><span class="badge badge-gray">{{ $role->user_count }} users</span></td>
                        <td>
                            <div style="display:flex; flex-wrap:wrap; gap:5px;">
                                @foreach($role->permissions as $pKey)
                                    <span class="badge badge-blue">{{ $pKey }}</span>
                                @endforeach
                            </div>
                        </td>
                        <td style="text-align:right; white-space:nowrap;">
                            @if(!$role->is_system)
                                <button type="button" class="btn btn-sm" onclick='openRoleModal(@json($role))'>Edit</button>
                                <form method="POST" action="{{ route('admin.roles.delete', $role->id) }}" style="display:inline;" onsubmit="return confirm('Delete role {{ addslashes($role->name) }}?')">
                                    @csrf
                                    @method('DELETE')
                                    <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                                </form>
                            @else
                                <span style="font-size:12px; color:var(--muted);">Full Access</span>
                            @endif
                        </td>
                    </tr>
                @endforeach
            </tbody>
        </table>
    </div>
</div>

<div class="modal-backdrop" id="roleModal">
    <div class="modal">
        <div class="card-header">
            <h2 class="card-title" id="roleModalTitle">Create Role</h2>
            <button type="button" class="btn btn-sm" onclick="closeModal('roleModal')">✕</button>
        </div>
        <form id="roleForm" method="POST" action="{{ route('admin.roles.store') }}">
            @csrf
            <input type="hidden" name="_method" id="roleMethod" value="POST">
            <div class="form-grid">
                <div class="field form-full">
                    <label>Role Name *</label>
                    <input class="input" name="name" id="role_name" required placeholder="e.g. Branch Manager / Counter Cashier">
                </div>
                <div class="field form-full">
                    <label>Description</label>
                    <input class="input" name="description" id="role_desc" placeholder="What this role can manage">
                </div>
                <div class="field form-full">
                    <label style="margin-bottom:8px;">Feature Permissions (check all allowed features)</label>
                    <div class="check-grid">
                        @foreach($permissionsCatalog as $pKey => $pLabel)
                            <label class="check-item">
                                <input type="checkbox" name="permissions[]" value="{{ $pKey }}" class="role-perm-cb">
                                <span>{{ $pLabel }}</span>
                            </label>
                        @endforeach
                    </div>
                </div>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:18px;">
                <button type="button" class="btn" onclick="closeModal('roleModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Role</button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
const roleStoreUrl = "{{ route('admin.roles.store') }}";
const roleBaseUrl = "{{ url('/admin/roles') }}";

function openRoleModal(role = null) {
    const form = document.getElementById('roleForm');
    const method = document.getElementById('roleMethod');
    const cbs = document.querySelectorAll('.role-perm-cb');
    if (role) {
        document.getElementById('roleModalTitle').textContent = 'Edit Role Permissions';
        form.action = roleBaseUrl + '/' + role.id;
        method.value = 'PUT';
        document.getElementById('role_name').value = role.name || '';
        document.getElementById('role_desc').value = role.description || '';
        const set = new Set(role.permissions || []);
        cbs.forEach(cb => cb.checked = set.has(cb.value));
    } else {
        document.getElementById('roleModalTitle').textContent = 'Create Role';
        form.action = roleStoreUrl;
        method.value = 'POST';
        form.reset();
        cbs.forEach(cb => cb.checked = (cb.value === 'view_dashboard'));
    }
    openModal('roleModal');
}
</script>
@endpush
