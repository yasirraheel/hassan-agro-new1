@extends('admin.layout')
@section('title', 'App Sync Settings')

@section('content')
<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Android App Login &amp; Sync Server</h2>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Staff and business owners sign in directly on the <strong>Business Manager</strong> Android app using this Server URL and their assigned email &amp; password (or self-sign-up to create a new business).
        </p>

        <div class="field" style="margin-bottom:12px;">
            <label>Server Base URL</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncUrl" value="{{ $apiBaseUrl }}" readonly>
                <button type="button" class="btn" onclick="copyField('syncUrl')">Copy</button>
            </div>
        </div>

        <div class="field" style="margin-bottom:12px;">
            <label>Legacy Static API Token (optional for curl / health checks)</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncToken" type="password" value="{{ $apiToken }}" readonly>
                <button type="button" class="btn" onclick="toggleToken()">Show</button>
                <button type="button" class="btn" onclick="copyField('syncToken')">Copy</button>
            </div>
        </div>

        <div style="font-size:12.5px; color:var(--muted); line-height:1.6;">
            <strong>Endpoints:</strong><br>
            <code>POST /api/v1/auth/login</code> · <code>POST /api/v1/auth/register</code> · <code>GET /api/v1/auth/me</code><br>
            <code>GET /api/v1/health</code> · <code>POST /api/v1/sync/push</code> · <code>GET /api/v1/sync/pull</code>
        </div>
    </div>

    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Cloud Database &amp; RBAC Status</h2>
        <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:10px; margin-bottom:16px;">
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Businesses</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['businesses'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Products</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['products'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Invoices</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['invoices'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Expenses</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['expenses'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Active Staff</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['users'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Roles</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['roles'] }}</div>
            </div>
        </div>

        <div style="font-size:13px; color:var(--muted); line-height:1.7;">
            <div><strong>Last Synced Activity:</strong>
                {{ $lastSyncMs > 0 ? \Carbon\Carbon::createFromTimestampMs($lastSyncMs)->format('d M Y, h:i:s A') : 'Waiting for first sync' }}
            </div>
            <div><strong>Super Admin Email:</strong> {{ $adminEmail }}</div>
        </div>
    </div>
</div>
@endsection

@push('scripts')
<script>
function toggleToken() {
    const el = document.getElementById('syncToken');
    el.type = el.type === 'password' ? 'text' : 'password';
}
function copyField(id) {
    const el = document.getElementById(id);
    const prev = el.type;
    el.type = 'text';
    el.select();
    navigator.clipboard?.writeText(el.value);
    el.type = prev;
    alert('Copied to clipboard!');
}
</script>
@endpush
