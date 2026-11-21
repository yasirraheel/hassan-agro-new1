@extends('admin.layout')
@section('title', 'App Sync & Security Settings')

@section('content')
<div class="card" style="margin-bottom:20px;">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
        <div>
            <h2 class="card-title" style="margin:0 0 4px 0;">Mobile App Security &amp; Authorized Packages</h2>
            <p style="font-size:13.5px; color:var(--muted); margin:0;">
                End-users and business owners never see or enter your server URL or secret keys inside the mobile app. Both values are AES-256-GCM encrypted into the APK at build time and verified on every request using a per-request HMAC-SHA256 signature (Package Name + Secret Key + APK Signing Certificate SHA-256 + Timestamp).
            </p>
        </div>
        <span style="font-size:12px; font-weight:700; padding:5px 12px; border-radius:999px; white-space:nowrap; background:{{ $enforceAppAttestation ? '#DCFCE7' : '#FEF3C7' }}; color:{{ $enforceAppAttestation ? '#166534' : '#92400E' }};">
            {{ $enforceAppAttestation ? 'Strict Mode Active' : 'Off (Setup Mode)' }}
        </span>
    </div>

    <form method="POST" action="{{ route('admin.settings.app_security.toggle') }}" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; background:var(--surface-soft); padding:12px 14px; border-radius:10px; margin-bottom:18px;">
        @csrf
        @method('PUT')
        <label style="display:flex; align-items:center; gap:10px; cursor:pointer; margin:0; font-weight:600; font-size:13.5px;">
            <input type="checkbox" name="enforce_app_attestation" value="1" {{ $enforceAppAttestation ? 'checked' : '' }} style="width:18px; height:18px;">
            <span>Enforce Package Name + Secret Key + Signing Certificate Verification on all API requests</span>
        </label>
        <button type="submit" class="btn btn-primary">Save Security Mode</button>
    </form>

    <div style="display:grid; gap:14px; margin-bottom:20px;">
        @forelse($authorizedApps as $app)
            <div style="border:1px solid var(--border); border-radius:12px; padding:14px; background:var(--surface);">
                <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:10px; margin-bottom:10px;">
                    <div>
                        <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
                            <strong style="font-size:15px;">{{ $app->name }}</strong>
                            <span style="font-size:11.5px; font-weight:700; padding:2px 8px; border-radius:999px; background:{{ $app->is_active ? '#DCFCE7' : '#FEE2E2' }}; color:{{ $app->is_active ? '#166534' : '#991B1B' }};">
                                {{ $app->is_active ? 'Active' : 'Disabled' }}
                            </span>
                            @if(!empty($app->cert_sha256))
                                <span style="font-size:11.5px; font-weight:700; padding:2px 8px; border-radius:999px; background:#DBEAFE; color:#1E40AF;">
                                    Cert Locked
                                </span>
                            @endif
                        </div>
                        <div style="font-size:13px; color:var(--muted); margin-top:4px;">
                            Package Name: <code style="font-weight:600;">{{ $app->package_name }}</code>
                            @if(($app->last_seen_at_ms ?? 0) > 0)
                                · Last connected: {{ \Carbon\Carbon::createFromTimestampMs($app->last_seen_at_ms)->format('d M Y, h:i A') }}
                            @else
                                · Waiting for first connection
                            @endif
                        </div>
                    </div>

                    <div style="display:flex; gap:6px; flex-wrap:wrap;">
                        <form method="POST" action="{{ route('admin.settings.apps.toggle', $app->id) }}" style="margin:0;">
                            @csrf
                            <button type="submit" class="btn">{{ $app->is_active ? 'Disable' : 'Enable' }}</button>
                        </form>
                        <form method="POST" action="{{ route('admin.settings.apps.rotate', $app->id) }}" style="margin:0;" onsubmit="return confirm('Generate a new App Secret Key? Older APKs using the old key will be blocked when Strict Mode is enabled.');">
                            @csrf
                            <button type="submit" class="btn">Rotate Key</button>
                        </form>
                        <form method="POST" action="{{ route('admin.settings.apps.delete', $app->id) }}" style="margin:0;" onsubmit="return confirm('Remove authorized package {{ $app->package_name }}?');">
                            @csrf
                            @method('DELETE')
                            <button type="submit" class="btn" style="color:#B91C1C;">Delete</button>
                        </form>
                    </div>
                </div>

                <div class="field" style="margin-bottom:10px;">
                    <label style="font-size:12px;">App Secret Key (set as <code>APP_CLIENT_SECRET</code> in your build secrets before compiling APK)</label>
                    <div style="display:flex; gap:8px;">
                        <input class="input" id="appKey_{{ $app->id }}" type="password" value="{{ $app->secret_key }}" readonly style="font-family:monospace; font-size:12.5px;">
                        <button type="button" class="btn" onclick="toggleInput('appKey_{{ $app->id }}')">Show</button>
                        <button type="button" class="btn" onclick="copyField('appKey_{{ $app->id }}')">Copy Key</button>
                    </div>
                </div>

                <div style="background:var(--surface-soft); padding:10px 12px; border-radius:8px; font-size:12.5px;">
                    <div style="margin-bottom:6px;">
                        <strong>Signing Certificate SHA-256 Lock (Prevents Clone APKs with Same Package Name):</strong>
                    </div>
                    @if(!empty($app->last_seen_cert_formatted))
                        <div style="margin-bottom:8px; color:var(--muted); word-break:break-all;">
                            Last Detected APK Cert: <code style="font-size:11.5px;">{{ $app->last_seen_cert_formatted }}</code>
                            @if(empty($app->cert_sha256) || strtoupper((string) $app->cert_sha256) !== strtoupper((string) $app->last_seen_cert_sha256))
                                <form method="POST" action="{{ route('admin.settings.apps.cert', $app->id) }}" style="display:inline-block; margin-left:8px;">
                                    @csrf
                                    <input type="hidden" name="cert_sha256" value="{{ $app->last_seen_cert_sha256 }}">
                                    <button type="submit" class="btn btn-primary" style="padding:3px 10px; font-size:12px;">Lock to Detected Cert</button>
                                </form>
                            @endif
                        </div>
                    @endif
                    <form method="POST" action="{{ route('admin.settings.apps.cert', $app->id) }}" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                        @csrf
                        <input class="input" type="text" name="cert_sha256" value="{{ $app->cert_formatted }}" placeholder="Optional: Paste 64-hex Play App Signing SHA-256 (leave blank to allow any cert with valid key)" style="flex:1; min-width:240px; font-family:monospace; font-size:12px;">
                        <button type="submit" class="btn">Save Cert Lock</button>
                    </form>
                </div>
            </div>
        @empty
            <p style="font-size:13.5px; color:var(--muted);">No authorized packages registered yet.</p>
        @endforelse
    </div>

    <div style="border-top:1px solid var(--border); padding-top:16px;">
        <h3 style="font-size:14.5px; margin:0 0 10px 0;">Register Additional Android Package</h3>
        <form method="POST" action="{{ route('admin.settings.apps.store') }}" style="display:grid; grid-template-columns:1fr 1.3fr 1.5fr auto; gap:10px; align-items:end;">
            @csrf
            <div class="field" style="margin:0;">
                <label>App Label *</label>
                <input class="input" type="text" name="app_name" value="{{ old('app_name') }}" placeholder="e.g. Play Store Release" required>
            </div>
            <div class="field" style="margin:0;">
                <label>Android Package Name *</label>
                <input class="input" type="text" name="package_name" value="{{ old('package_name') }}" placeholder="com.company.businessapp" required>
            </div>
            <div class="field" style="margin:0;">
                <label>Signing Cert SHA-256 (optional)</label>
                <input class="input" type="text" name="cert_sha256" value="{{ old('cert_sha256') }}" placeholder="Optional 64-hex SHA-256 fingerprint">
            </div>
            <button type="submit" class="btn btn-primary">Add Package &amp; Generate Key</button>
        </form>
    </div>
</div>

<div class="grid-2">
    <div class="card">
        <h2 class="card-title" style="margin-bottom:12px;">Admin Account</h2>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Update the Super Admin login email and password for this web panel. Leave password blank to keep your existing password.
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
        <h2 class="card-title" style="margin-bottom:12px;">Android Build Configuration (Super Admin Only)</h2>
        <p style="font-size:13.5px; color:var(--muted); margin-top:0;">
            Configure <code>BACKEND_BASE_URL</code> and <code>APP_CLIENT_SECRET</code> in your build secrets before compiling the Android APK. Users never see or type these values inside the app.
        </p>

        <div class="field" style="margin-bottom:12px;">
            <label>BACKEND_BASE_URL</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncUrl" value="{{ $apiBaseUrl }}" readonly>
                <button type="button" class="btn" onclick="copyField('syncUrl')">Copy</button>
            </div>
        </div>

        <div class="field" style="margin-bottom:12px;">
            <label>Legacy Static API Token (optional for server health checks)</label>
            <div style="display:flex; gap:8px;">
                <input class="input" id="syncToken" type="password" value="{{ $apiToken }}" readonly>
                <button type="button" class="btn" onclick="toggleInput('syncToken')">Show</button>
                <button type="button" class="btn" onclick="copyField('syncToken')">Copy</button>
            </div>
        </div>

        <div style="font-size:12.5px; color:var(--muted); line-height:1.6;">
            <strong>Protected API Endpoints:</strong><br>
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
            Configure your SMTP server here. When enabled, new users registering a business from the app must verify a 6-digit OTP email branded with their business name before their account is activated. Leave disabled to allow direct sign-up without OTP.
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
function toggleInput(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.type = el.type === 'password' ? 'text' : 'password';
}
function copyField(id) {
    const el = document.getElementById(id);
    if (!el) return;
    const prev = el.type;
    el.type = 'text';
    el.select();
    navigator.clipboard?.writeText(el.value);
    el.type = prev;
    showToast('Copied to clipboard!');
}
</script>
@endpush
