@php
    $themePrimary     = $activeTheme['primary']      ?? '#0D47A1';
    $themeAccent      = $activeTheme['accent']       ?? '#1565C0';
    $themeSoft        = $activeTheme['soft']         ?? '#E3F2FD';
    $themeDarkPrimary = $activeTheme['dark_primary'] ?? '#60A5FA';
    $themeDarkAccent  = $activeTheme['dark_accent']  ?? '#3B82F6';
    $themeDarkSoft    = $activeTheme['dark_soft']    ?? 'rgba(96, 165, 250, 0.16)';
@endphp
<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>@yield('title', 'Admin Panel') — Business Manager</title>
    <script>
        (function() {
            try {
                var saved = localStorage.getItem('bm_admin_theme') || 'auto';
                var resolved = saved === 'auto'
                    ? (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
                    : saved;
                document.documentElement.setAttribute('data-theme', resolved);
                document.documentElement.setAttribute('data-theme-pref', saved);
            } catch (e) {}
        })();
    </script>
    <style>
        :root {
            --bg: #f3f6fa;
            --surface: #ffffff;
            --surface-elevated: #ffffff;
            --surface-soft: #eef2f7;
            --th-bg: #f8fafc;
            --row-border: #e8eef5;
            --row-hover: #f8fbff;
            --input-bg: #ffffff;
            --primary: {{ $themePrimary }};
            --primary-mid: {{ $themeAccent }};
            --primary-soft: {{ $themeSoft }};
            --primary-on: #ffffff;
            --border: #d8e0ea;
            --text: #0f172a;
            --muted: #556070;
            --success: #15803d;
            --success-bg: #dcfce7;
            --success-border: #bbf7d0;
            --danger: #b91c1c;
            --danger-bg: #fee2e2;
            --danger-border: #fecaca;
            --danger-solid: #dc2626;
            --warn: #b45309;
            --warn-bg: #fef3c7;
            --info: #1d4ed8;
            --info-bg: #dbeafe;
            --gray-bg: #e5e7eb;
            --gray-text: #374151;
            --bar-track: #e6ecf3;
            --backdrop: rgba(15, 23, 42, 0.58);
            --radius: 14px;
            --shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
            --modal-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
            color-scheme: light;
        }
        [data-theme="dark"] {
            --bg: #0b1120;
            --surface: #111827;
            --surface-elevated: #172033;
            --surface-soft: #1e293b;
            --th-bg: #151e2e;
            --row-border: #1f2b3e;
            --row-hover: #192438;
            --input-bg: #0e1626;
            --primary: {{ $themeDarkPrimary }};
            --primary-mid: {{ $themeDarkAccent }};
            --primary-soft: {{ $themeDarkSoft }};
            --primary-on: #0b1120;
            --border: #2b384e;
            --text: #f1f5f9;
            --muted: #94a3b8;
            --success: #4ade80;
            --success-bg: rgba(34, 197, 94, 0.16);
            --success-border: rgba(74, 222, 128, 0.35);
            --danger: #f87171;
            --danger-bg: rgba(239, 68, 68, 0.16);
            --danger-border: rgba(248, 113, 113, 0.35);
            --danger-solid: #ef4444;
            --warn: #fbbf24;
            --warn-bg: rgba(245, 158, 11, 0.16);
            --info: #60a5fa;
            --info-bg: rgba(59, 130, 246, 0.18);
            --gray-bg: #263244;
            --gray-text: #cbd5e1;
            --bar-track: #1f2b3e;
            --backdrop: rgba(2, 6, 23, 0.78);
            --shadow: 0 4px 16px rgba(0, 0, 0, 0.32);
            --modal-shadow: 0 24px 64px rgba(0, 0, 0, 0.65);
            color-scheme: dark;
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
            background: var(--bg);
            color: var(--text);
            line-height: 1.45;
            transition: background 0.18s ease, color 0.18s ease;
        }
        a { color: inherit; text-decoration: none; }
        code {
            background: var(--surface-soft);
            color: var(--primary);
            padding: 2px 6px;
            border-radius: 6px;
            font-size: 12px;
        }
        .shell { display: flex; min-height: 100vh; }
        .sidebar {
            width: 256px;
            background: #0c1322;
            color: #e2e8f0;
            border-right: 1px solid rgba(255, 255, 255, 0.08);
            padding: 20px 14px;
            display: flex;
            flex-direction: column;
            gap: 5px;
            position: sticky;
            top: 0;
            height: 100vh;
            flex-shrink: 0;
            overflow-y: auto;
        }
        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 6px 10px 16px;
            border-bottom: 1px solid rgba(255,255,255,0.1);
            margin-bottom: 8px;
        }
        .brand-badge {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            background: linear-gradient(135deg, {{ $themeAccent }}, {{ $themePrimary }});
            display: grid;
            place-items: center;
            font-weight: 800;
            font-size: 15px;
            color: #fff;
            flex-shrink: 0;
        }
        .brand-title { font-weight: 700; font-size: 15px; line-height: 1.2; color: #f8fafc; }
        .brand-sub { font-size: 11px; color: #94a3b8; }
        .nav-section {
            font-size: 10.5px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.7px;
            color: #64748b;
            padding: 10px 12px 4px;
        }
        .nav-link {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 9px 12px;
            border-radius: 10px;
            font-size: 13.5px;
            font-weight: 500;
            color: #cbd5e1;
            transition: background 0.15s, color 0.15s;
        }
        .nav-link:hover { background: rgba(255,255,255,0.08); color: #fff; }
        .nav-link.active { background: {{ $themePrimary }}; color: #fff; font-weight: 600; box-shadow: 0 2px 10px rgba(0,0,0,0.25); }
        .sidebar-footer {
            margin-top: auto;
            padding-top: 12px;
            border-top: 1px solid rgba(255,255,255,0.1);
        }
        .main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
        .topbar {
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            padding: 12px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
            position: sticky;
            top: 0;
            z-index: 20;
        }
        .topbar-left { display: flex; align-items: center; gap: 12px; }
        .page-title { font-size: 18px; font-weight: 700; margin: 0; color: var(--text); }
        .content { padding: 22px 24px 40px; max-width: 1400px; width: 100%; margin: 0 auto; }
        .grid-4 { display: grid; grid-template-columns: repeat(auto-fit, minmax(215px, 1fr)); gap: 14px; margin-bottom: 18px; }
        .grid-3 { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; margin-bottom: 18px; }
        .grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 18px;
            box-shadow: var(--shadow);
        }
        .kpi-label { font-size: 11px; font-weight: 700; color: var(--muted); text-transform: uppercase; letter-spacing: 0.45px; }
        .kpi-val { font-size: 23px; font-weight: 800; margin-top: 6px; font-variant-numeric: tabular-nums; color: var(--text); }
        .kpi-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }
        .card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
        .card-title { font-size: 15px; font-weight: 700; margin: 0; color: var(--text); }

        /* Theme Mode Switcher */
        .theme-switcher {
            display: inline-flex;
            align-items: center;
            gap: 2px;
            padding: 3px;
            border-radius: 10px;
            background: var(--surface-soft);
            border: 1px solid var(--border);
        }
        .theme-btn {
            border: none;
            background: transparent;
            color: var(--muted);
            padding: 5px 9px;
            border-radius: 7px;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            transition: background 0.15s, color 0.15s;
        }
        .theme-btn:hover { color: var(--text); }
        .theme-btn.active {
            background: var(--surface);
            color: var(--primary);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
        }

        /* Period Filter Pills */
        .period-bar {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            flex-wrap: wrap;
            padding: 4px;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 12px;
        }
        .period-pill {
            display: inline-flex;
            align-items: center;
            padding: 6px 13px;
            border-radius: 8px;
            font-size: 12.5px;
            font-weight: 700;
            color: var(--muted);
            transition: background 0.15s, color 0.15s;
        }
        .period-pill:hover { background: var(--surface-soft); color: var(--text); }
        .period-pill.active {
            background: {{ $themePrimary }};
            color: #ffffff;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.18);
        }

        /* P&L Summary Banner */
        .pl-hero {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 18px 20px;
            margin-bottom: 18px;
            box-shadow: var(--shadow);
        }
        .pl-formula {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            align-items: center;
            padding: 12px 14px;
            background: var(--surface-soft);
            border: 1px solid var(--border);
            border-radius: 11px;
            margin-top: 14px;
            font-size: 12.5px;
        }
        .pl-chip {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 4px 10px;
            border-radius: 8px;
            background: var(--surface);
            border: 1px solid var(--border);
            font-weight: 700;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            padding: 8px 14px;
            border-radius: 9px;
            border: 1px solid var(--border);
            background: var(--surface);
            color: var(--text);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            white-space: nowrap;
            transition: background 0.14s, border-color 0.14s, transform 0.1s;
        }
        .btn:hover { background: var(--surface-soft); }
        .btn:active { transform: scale(0.98); }
        .btn-primary { background: {{ $themePrimary }}; color: #fff; border-color: {{ $themePrimary }}; }
        .btn-primary:hover { background: {{ $themeAccent }}; border-color: {{ $themeAccent }}; color: #fff; }
        .btn-danger { background: var(--danger-bg); color: var(--danger); border-color: var(--danger-border); }
        .btn-danger:hover { background: var(--danger-solid); color: #fff; border-color: var(--danger-solid); }
        .btn-danger-solid { background: var(--danger-solid); color: #fff; border-color: var(--danger-solid); }
        .btn-danger-solid:hover { filter: brightness(0.92); color: #fff; }
        .btn-sm { padding: 5px 10px; font-size: 12px; border-radius: 7px; }
        .input, select, textarea {
            width: 100%;
            padding: 9px 12px;
            border: 1px solid var(--border);
            border-radius: 9px;
            font-size: 13.5px;
            background: var(--input-bg);
            color: var(--text);
            font-family: inherit;
        }
        .input:focus, select:focus, textarea:focus { outline: 2px solid var(--primary-soft); border-color: var(--primary); }
        .toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 13.5px; }
        th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--row-border); vertical-align: middle; color: var(--text); }
        th { font-size: 11px; text-transform: uppercase; letter-spacing: 0.45px; color: var(--muted); background: var(--th-bg); font-weight: 700; white-space: nowrap; }
        tr:hover td { background: var(--row-hover); }
        .num { font-variant-numeric: tabular-nums; white-space: nowrap; }
        .badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 999px;
            font-size: 11.5px;
            font-weight: 700;
            white-space: nowrap;
        }
        .badge-green { background: var(--success-bg); color: var(--success); }
        .badge-primary { background: var(--primary-soft); color: var(--primary); }
        .badge-red { background: var(--danger-bg); color: var(--danger); }
        .badge-amber { background: var(--warn-bg); color: var(--warn); }
        .badge-blue { background: var(--info-bg); color: var(--info); }
        .badge-gray { background: var(--gray-bg); color: var(--gray-text); }
        .alert { padding: 11px 14px; border-radius: 10px; margin-bottom: 16px; font-size: 13.5px; font-weight: 600; }
        .alert-success { background: var(--success-bg); color: var(--success); border: 1px solid var(--success-border); }
        .alert-error { background: var(--danger-bg); color: var(--danger); border: 1px solid var(--danger-border); }

        .modal-backdrop {
            display: none;
            position: fixed;
            inset: 0;
            background: var(--backdrop);
            backdrop-filter: blur(4px);
            z-index: 60;
            align-items: center;
            justify-content: center;
            padding: 16px;
        }
        .modal-backdrop.open { display: flex; animation: bmFadeIn 0.16s ease; }
        .modal {
            background: var(--surface-elevated);
            border: 1px solid var(--border);
            color: var(--text);
            border-radius: 16px;
            width: 100%;
            max-width: 680px;
            max-height: 90vh;
            overflow-y: auto;
            padding: 20px 22px;
            box-shadow: var(--modal-shadow);
            animation: bmScaleIn 0.18s cubic-bezier(0.16, 1, 0.3, 1);
        }

        /* Custom Themed Confirmation Dialog */
        .confirm-modal {
            max-width: 430px;
            padding: 24px;
            text-align: left;
        }
        .confirm-icon-wrap {
            width: 48px;
            height: 48px;
            border-radius: 14px;
            display: grid;
            place-items: center;
            font-size: 22px;
            margin-bottom: 14px;
            background: var(--danger-bg);
            color: var(--danger);
            border: 1px solid var(--danger-border);
        }
        .confirm-icon-wrap.warn {
            background: var(--warn-bg);
            color: var(--warn);
            border-color: rgba(245, 158, 11, 0.35);
        }
        .confirm-title {
            margin: 0 0 6px;
            font-size: 17px;
            font-weight: 800;
            color: var(--text);
        }
        .confirm-item-pill {
            display: inline-block;
            margin: 4px 0 10px;
            padding: 5px 11px;
            border-radius: 9px;
            background: var(--surface-soft);
            border: 1px solid var(--border);
            font-size: 13px;
            font-weight: 700;
            color: var(--text);
        }
        .confirm-body {
            margin: 0 0 20px;
            font-size: 13.5px;
            color: var(--muted);
            line-height: 1.5;
        }
        .confirm-actions {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }

        /* Custom Toast Notification */
        .bm-toast {
            position: fixed;
            bottom: 22px;
            right: 22px;
            z-index: 90;
            padding: 11px 16px;
            border-radius: 12px;
            background: var(--surface-elevated);
            color: var(--text);
            border: 1px solid var(--border);
            box-shadow: var(--modal-shadow);
            font-size: 13px;
            font-weight: 700;
            display: none;
            align-items: center;
            gap: 8px;
        }
        .bm-toast.show { display: inline-flex; animation: bmScaleIn 0.16s ease; }

        @keyframes bmFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes bmScaleIn { from { opacity: 0; transform: scale(0.95) translateY(6px); } to { opacity: 1; transform: scale(1) translateY(0); } }

        .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
        .form-full { grid-column: 1 / -1; }
        .field label { display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 4px; }
        .check-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
        .check-item {
            display: flex; align-items: center; gap: 8px; padding: 8px 10px;
            border: 1px solid var(--border); border-radius: 9px; font-size: 12.5px; cursor: pointer;
            background: var(--surface);
            color: var(--text);
        }
        .swatch { display: inline-block; width: 14px; height: 14px; border-radius: 50%; vertical-align: middle; margin-right: 4px; border: 1px solid rgba(255,255,255,0.2); }
        .menu-btn { display: none; }
        @media (max-width: 860px) {
            .sidebar { position: fixed; left: -270px; z-index: 50; transition: left 0.2s; }
            .sidebar.open { left: 0; }
            .menu-btn { display: inline-flex; }
            .content { padding: 16px; }
            .topbar { padding: 12px 16px; }
            .form-grid { grid-template-columns: 1fr; }
            .grid-2 { grid-template-columns: 1fr; }
        }
        @media print {
            .sidebar, .topbar, .no-print, .modal-backdrop, .bm-toast { display: none !important; }
            body { background: #fff !important; color: #111827 !important; }
            .card { box-shadow: none; border: 1px solid #e5e7eb; background: #fff !important; color: #111827 !important; }
        }
    </style>
</head>
<body>
<div class="shell">
    <aside class="sidebar" id="sidebar">
        <div class="brand">
            <div class="brand-badge">BM</div>
            <div>
                <div class="brand-title">Business Manager</div>
                <div class="brand-sub">Multi-Business Admin</div>
            </div>
        </div>

        <div class="nav-section">Operations</div>
        <a href="{{ route('admin.dashboard') }}" class="nav-link {{ request()->routeIs('admin.dashboard') ? 'active' : '' }}">📊 Dashboard &amp; P&amp;L</a>
        <a href="{{ route('admin.businesses') }}" class="nav-link {{ request()->routeIs('admin.businesses*') ? 'active' : '' }}">🏢 Businesses &amp; Themes</a>
        <a href="{{ route('admin.products') }}" class="nav-link {{ request()->routeIs('admin.products*') ? 'active' : '' }}">📦 Inventory &amp; Stock</a>
        <a href="{{ route('admin.invoices') }}" class="nav-link {{ request()->routeIs('admin.invoices*') ? 'active' : '' }}">🧾 POS Sales &amp; Invoices</a>
        <a href="{{ route('admin.customers') }}" class="nav-link {{ request()->routeIs('admin.customers*') ? 'active' : '' }}">🤝 Customers &amp; Udhaar</a>
        <a href="{{ route('admin.expenses') }}" class="nav-link {{ request()->routeIs('admin.expenses*') ? 'active' : '' }}">💸 Expenses</a>
        <a href="{{ route('admin.reports') }}" class="nav-link {{ request()->routeIs('admin.reports*') ? 'active' : '' }}">📈 Monthly P&amp;L Reports</a>

        <div class="nav-section">Access Control</div>
        <a href="{{ route('admin.users') }}" class="nav-link {{ request()->routeIs('admin.users*') ? 'active' : '' }}">👥 Admins &amp; Staff</a>
        <a href="{{ route('admin.roles') }}" class="nav-link {{ request()->routeIs('admin.roles*') ? 'active' : '' }}">🔐 Roles &amp; Permissions</a>
        <a href="{{ route('admin.settings') }}" class="nav-link {{ request()->routeIs('admin.settings*') ? 'active' : '' }}">⚙️ Sync Settings</a>

        <div class="sidebar-footer">
            <div style="font-size:12px; font-weight:700; color:#fff; padding:0 8px;">{{ session('bm_admin_name', 'Admin') }}</div>
            <div style="font-size:11px; color:#94a3b8; padding:0 8px 8px; overflow:hidden; text-overflow:ellipsis;">
                {{ session('bm_admin_email') }} · {{ session('bm_admin_role', 'Super Admin') }}
            </div>
            <form method="POST" action="{{ route('admin.logout') }}">
                @csrf
                <button type="submit" class="btn" style="width:100%; background:rgba(255,255,255,0.08); color:#fff; border-color:rgba(255,255,255,0.16);">
                    Sign Out
                </button>
            </form>
        </div>
    </aside>

    <div class="main">
        <header class="topbar">
            <div class="topbar-left">
                <button type="button" class="btn btn-sm menu-btn" onclick="document.getElementById('sidebar').classList.toggle('open')">☰</button>
                <h1 class="page-title">@yield('title', 'Dashboard')</h1>
            </div>

            <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
                <div class="theme-switcher" role="group" aria-label="Appearance mode">
                    <button type="button" class="theme-btn" data-theme-set="light" onclick="setAdminTheme('light')" title="Light Mode">☀️ Light</button>
                    <button type="button" class="theme-btn" data-theme-set="dark" onclick="setAdminTheme('dark')" title="Dark Mode">🌙 Dark</button>
                    <button type="button" class="theme-btn" data-theme-set="auto" onclick="setAdminTheme('auto')" title="System Auto">💻 Auto</button>
                </div>

                @if(isset($businesses))
                    <form method="GET" action="{{ url()->current() }}" style="display:flex; align-items:center; gap:6px;">
                        <select name="business_id" onchange="this.form.submit()" style="width:auto; min-width:185px; padding:7px 10px; font-weight:600;">
                            <option value="">All Businesses ({{ $businesses->count() }})</option>
                            @foreach($businesses as $b)
                                <option value="{{ $b->id }}" {{ ($selectedBizId ?? '') === $b->id ? 'selected' : '' }}>
                                    {{ $b->name }}
                                </option>
                            @endforeach
                        </select>
                    </form>
                @endif
                <span class="badge badge-primary">● Cloud Sync Active</span>
            </div>
        </header>

        <main class="content">
            @if(session('success'))
                <div class="alert alert-success">{{ session('success') }}</div>
            @endif
            @if(session('error'))
                <div class="alert alert-error">{{ session('error') }}</div>
            @endif
            @if($errors->any())
                <div class="alert alert-error">{{ $errors->first() }}</div>
            @endif

            @yield('content')
        </main>
    </div>
</div>

{{-- Global Custom Themed Confirmation Dialog (replaces all native browser confirm() dialogs) --}}
<div class="modal-backdrop no-print" id="bmConfirmModal" role="dialog" aria-modal="true" aria-labelledby="bmConfirmTitle">
    <div class="modal confirm-modal">
        <div class="confirm-icon-wrap" id="bmConfirmIconWrap">
            <span id="bmConfirmIcon">⚠️</span>
        </div>
        <h3 class="confirm-title" id="bmConfirmTitle">Confirm Action</h3>
        <div class="confirm-item-pill" id="bmConfirmItem" style="display:none;"></div>
        <p class="confirm-body" id="bmConfirmMessage">Are you sure you want to proceed?</p>
        <div class="confirm-actions">
            <button type="button" class="btn" id="bmConfirmCancel">Cancel</button>
            <button type="button" class="btn btn-danger-solid" id="bmConfirmOk">Confirm</button>
        </div>
    </div>
</div>

<div class="bm-toast no-print" id="bmToast"></div>

<script>
    function openModal(id) { document.getElementById(id)?.classList.add('open'); }
    function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }

    // Theme Mode Controller (Light / Dark / Auto)
    function applyAdminTheme(pref) {
        const resolved = pref === 'auto'
            ? (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
            : pref;
        document.documentElement.setAttribute('data-theme', resolved);
        document.documentElement.setAttribute('data-theme-pref', pref);
        document.querySelectorAll('.theme-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.themeSet === pref);
        });
    }
    function setAdminTheme(pref) {
        try { localStorage.setItem('bm_admin_theme', pref); } catch (e) {}
        applyAdminTheme(pref);
    }
    (function initThemeSwitcher() {
        const pref = document.documentElement.getAttribute('data-theme-pref') || 'auto';
        applyAdminTheme(pref);
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
                if ((localStorage.getItem('bm_admin_theme') || 'auto') === 'auto') {
                    applyAdminTheme('auto');
                }
            });
        }
    })();

    // Custom Toast Notification
    let bmToastTimer = null;
    function showToast(msg) {
        const el = document.getElementById('bmToast');
        if (!el) return;
        el.textContent = '✓ ' + msg;
        el.classList.add('show');
        clearTimeout(bmToastTimer);
        bmToastTimer = setTimeout(() => el.classList.remove('show'), 2600);
    }

    // Global Custom Confirmation Dialog (intercepts any form with data-confirm-message)
    let pendingConfirmForm = null;
    const confirmBackdrop = document.getElementById('bmConfirmModal');
    const confirmTitleEl = document.getElementById('bmConfirmTitle');
    const confirmItemEl = document.getElementById('bmConfirmItem');
    const confirmMsgEl = document.getElementById('bmConfirmMessage');
    const confirmOkBtn = document.getElementById('bmConfirmOk');
    const confirmCancelBtn = document.getElementById('bmConfirmCancel');
    const confirmIconWrap = document.getElementById('bmConfirmIconWrap');
    const confirmIconEl = document.getElementById('bmConfirmIcon');

    function closeConfirmModal() {
        pendingConfirmForm = null;
        confirmBackdrop?.classList.remove('open');
    }

    function openConfirmModal(opts, onConfirm) {
        confirmTitleEl.textContent = opts.title || 'Confirm Action';
        confirmMsgEl.textContent = opts.message || 'Are you sure you want to continue?';
        confirmOkBtn.textContent = opts.confirmText || 'Delete';
        if (opts.itemName) {
            confirmItemEl.textContent = opts.itemName;
            confirmItemEl.style.display = 'inline-block';
        } else {
            confirmItemEl.style.display = 'none';
        }
        const isWarn = opts.variant === 'warn';
        confirmIconWrap.classList.toggle('warn', isWarn);
        confirmIconEl.textContent = opts.icon || (isWarn ? '↩️' : '🗑️');
        confirmOkBtn.className = isWarn ? 'btn btn-primary' : 'btn btn-danger-solid';
        pendingConfirmForm = onConfirm;
        confirmBackdrop.classList.add('open');
        setTimeout(() => confirmCancelBtn?.focus(), 40);
    }

    confirmCancelBtn?.addEventListener('click', closeConfirmModal);
    confirmBackdrop?.addEventListener('click', function(e) {
        if (e.target === confirmBackdrop) closeConfirmModal();
    });
    confirmOkBtn?.addEventListener('click', function() {
        const fn = pendingConfirmForm;
        closeConfirmModal();
        if (typeof fn === 'function') fn();
    });

    document.addEventListener('submit', function(e) {
        const form = e.target;
        if (!(form instanceof HTMLFormElement)) return;
        if (!form.dataset.confirmMessage || form.dataset.confirmed === '1') return;
        e.preventDefault();
        openConfirmModal({
            title: form.dataset.confirmTitle || 'Confirm Deletion',
            itemName: form.dataset.confirmItem || '',
            message: form.dataset.confirmMessage,
            confirmText: form.dataset.confirmBtn || 'Delete',
            variant: form.dataset.confirmVariant || 'danger',
            icon: form.dataset.confirmIcon || ''
        }, function() {
            form.dataset.confirmed = '1';
            form.submit();
        });
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeConfirmModal();
            document.querySelectorAll('.modal-backdrop.open').forEach(el => el.classList.remove('open'));
        }
    });
</script>
@stack('scripts')
</body>
</html>
