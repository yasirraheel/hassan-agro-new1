@extends('admin.layout')
@section('title', 'App Sync Settings')

@section('content')
<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Android App Connection Details</h2>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Enter these values in <strong>Hassan Agro Android App → Settings → Custom Server Sync</strong> on every phone you want to keep synchronized.
        </p>

        <div class="field" style="margin-bottom:12px;">
            <label>Server Base URL</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncUrl" value="{{ $apiBaseUrl }}" readonly>
                <button type="button" class="btn" onclick="copyField('syncUrl')">Copy</button>
            </div>
        </div>

        <div class="field" style="margin-bottom:12px;">
            <label>API Key / Bearer Token (from .env <code>SYNC_API_TOKEN</code>)</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncToken" type="password" value="{{ $apiToken }}" readonly>
                <button type="button" class="btn" onclick="toggleToken()">Show</button>
                <button type="button" class="btn" onclick="copyField('syncToken')">Copy</button>
            </div>
        </div>

        <div class="field">
            <label>WebSocket URL</label>
            <input class="input" value="Leave empty (Hostinger uses 30-sec automatic HTTPS sync)" readonly style="color:var(--muted);">
        </div>
    </div>

    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Cloud Database Status</h2>
        <div style="display:grid; grid-template-columns:repeat(2, 1fr); gap:10px; margin-bottom:16px;">
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Businesses</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['businesses'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Products</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['products'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">POS Invoices</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['invoices'] }}</div>
            </div>
            <div style="background:var(--surface-soft); padding:12px; border-radius:10px;">
                <div class="kpi-label">Expenses</div>
                <div style="font-size:22px; font-weight:800;">{{ $stats['expenses'] }}</div>
            </div>
        </div>

        <div style="font-size:13px; color:var(--muted); line-height:1.7;">
            <div><strong>Last Synced Activity:</strong>
                {{ $lastSyncMs > 0 ? \Carbon\Carbon::createFromTimestampMs($lastSyncMs)->format('d M Y, h:i:s A') : 'Waiting for first device sync' }}
            </div>
            <div><strong>Admin Login Email:</strong> {{ $adminEmail }}</div>
            <div><strong>Change Admin Password / Token:</strong> Edit <code>ADMIN_PASSWORD</code> or <code>SYNC_API_TOKEN</code> in <code>.env</code> and run <code>php artisan config:cache</code>.</div>
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
