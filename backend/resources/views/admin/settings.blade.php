@extends('admin.layout')
@section('title', 'App Sync Settings')

@section('content')
<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Admin Account</h2>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Yahan se current admin ka email aur password change kar sakte hain. Password khali chhorne par purana password hi rahega.
        </p>
        <form method="POST" action="{{ route('admin.settings.account') }}">
            @csrf
            @method('PUT')
            <div class="field" style="margin-bottom:12px;">
                <label>Admin Name</label>
                <input class="input" value="{{ $adminName }}" readonly>
            </div>
            <div class="field" style="margin-bottom:12px;">
                <label>Admin Email *</label>
                <input class="input" type="email" name="email" value="{{ old('email', $adminEmail) }}" required>
            </div>
            <div class="field" style="margin-bottom:12px;">
                <label>New Password (optional)</label>
                <input class="input" type="password" name="password" minlength="6" autocomplete="new-password" placeholder="6+ characters">
            </div>
            <div class="field" style="margin-bottom:12px;">
                <label>Confirm New Password</label>
                <input class="input" type="password" name="password_confirmation" minlength="6" autocomplete="new-password">
            </div>
            <button type="submit" class="btn btn-primary">Save Admin Credentials</button>
        </form>
    </div>

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

    <div class="card">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:10px;">
            <h2 class="card-title" style="margin:0;">Registration Email OTP &amp; SMTP</h2>
            <span style="font-size:12px; font-weight:700; padding:4px 10px; border-radius:999px; background:{{ $smtpSettings['otp_verification_enabled'] ? '#DCFCE7' : '#F1F5F9' }}; color:{{ $smtpSettings['otp_verification_enabled'] ? '#166534' : '#475569' }};">
                {{ $smtpSettings['otp_verification_enabled'] ? 'OTP Active' : 'OTP Off (Default)' }}
            </span>
        </div>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Configure your SMTP server here. When enabled, new users registering a business from the app must verify a 6-digit OTP email branded with their business name before their account is created. Leave disabled to allow direct sign-up without OTP.
        </p>

        <form method="POST" action="{{ route('admin.settings.smtp') }}">
            @csrf
            @method('PUT')

            <div class="field" style="margin-bottom:14px; background:var(--surface-soft); padding:12px; border-radius:10px;">
                <label style="display:flex; align-items:center; gap:10px; cursor:pointer; margin:0; font-weight:600;">
                    <input type="checkbox" name="otp_verification_enabled" value="1" {{ old('otp_verification_enabled', $smtpSettings['otp_verification_enabled']) ? 'checked' : '' }} style="width:18px; height:18px;">
                    <span>Require Email OTP Verification on New Business Registration</span>
                </label>
            </div>

            <div style="display:grid; grid-template-columns:2fr 1fr; gap:10px; margin-bottom:12px;">
                <div class="field" style="margin:0;">
                    <label>SMTP Host</label>
                    <input class="input" type="text" name="smtp_host" value="{{ old('smtp_host', $smtpSettings['smtp_host']) }}" placeholder="smtp.hostinger.com or smtp.gmail.com">
                </div>
                <div class="field" style="margin:0;">
                    <label>Port</label>
                    <input class="input" type="number" name="smtp_port" value="{{ old('smtp_port', $smtpSettings['smtp_port']) }}" placeholder="587">
                </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:12px;">
                <div class="field" style="margin:0;">
                    <label>Encryption</label>
                    <select class="input" name="smtp_encryption">
                        @php $enc = old('smtp_encryption', $smtpSettings['smtp_encryption']); @endphp
                        <option value="tls" {{ $enc === 'tls' ? 'selected' : '' }}>TLS (Port 587)</option>
                        <option value="ssl" {{ $enc === 'ssl' ? 'selected' : '' }}>SSL (Port 465)</option>
                        <option value="none" {{ $enc === 'none' ? 'selected' : '' }}>None (Port 25)</option>
                    </select>
                </div>
                <div class="field" style="margin:0;">
                    <label>SMTP Username</label>
                    <input class="input" type="text" name="smtp_username" value="{{ old('smtp_username', $smtpSettings['smtp_username']) }}" placeholder="noreply@yourdomain.com" autocomplete="off">
                </div>
            </div>

            <div class="field" style="margin-bottom:12px;">
                <label>SMTP Password {{ $smtpSettings['smtp_password'] !== '' ? '(saved, leave blank to keep current)' : '' }}</label>
                <input class="input" type="password" name="smtp_password" placeholder="{{ $smtpSettings['smtp_password'] !== '' ? '••••••••••••' : 'Enter SMTP password / app password' }}" autocomplete="new-password">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:14px;">
                <div class="field" style="margin:0;">
                    <label>From Email Address</label>
                    <input class="input" type="email" name="smtp_from_address" value="{{ old('smtp_from_address', $smtpSettings['smtp_from_address']) }}" placeholder="noreply@yourdomain.com">
                </div>
                <div class="field" style="margin:0;">
                    <label>From Name (optional)</label>
                    <input class="input" type="text" name="smtp_from_name" value="{{ old('smtp_from_name', $smtpSettings['smtp_from_name']) }}" placeholder="Blank = uses user's Business Name">
                </div>
            </div>

            <button type="submit" class="btn btn-primary">Save SMTP &amp; OTP Settings</button>
        </form>

        <hr style="border:none; border-top:1px solid var(--border); margin:18px 0;">

        <form method="POST" action="{{ route('admin.settings.smtp.test') }}" style="display:flex; gap:8px; align-items:flex-end;">
            @csrf
            <div class="field" style="flex:1; margin:0;">
                <label>Test SMTP Delivery</label>
                <input class="input" type="email" name="test_email" value="{{ $adminEmail }}" placeholder="recipient@example.com" required>
            </div>
            <button type="submit" class="btn">Send Test Email</button>
        </form>
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
    showToast('Copied to clipboard!');
}
</script>
@endpush
