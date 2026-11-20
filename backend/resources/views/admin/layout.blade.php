@php
    $themePrimary = $activeTheme['primary'] ?? '#0D47A1';
    $themeAccent  = $activeTheme['accent']  ?? '#1565C0';
    $themeSoft    = $activeTheme['soft']    ?? '#E3F2FD';
@endphp
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>@yield('title', 'Admin Panel') — Business Manager</title>
    <style>
        :root {
            --bg: #f5f7fa;
            --surface: #ffffff;
            --surface-soft: #f0f4f8;
            --primary: {{ $themePrimary }};
            --primary-mid: {{ $themeAccent }};
            --primary-soft: {{ $themeSoft }};
            --border: #dde3ea;
            --text: #111827;
            --muted: #5b6472;
            --danger: #b91c1c;
            --danger-bg: #fee2e2;
            --warn: #b45309;
            --warn-bg: #fef3c7;
            --info: #1d4ed8;
            --info-bg: #dbeafe;
            --radius: 14px;
            --shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
            background: var(--bg);
            color: var(--text);
            line-height: 1.45;
        }
        a { color: inherit; text-decoration: none; }
        .shell { display: flex; min-height: 100vh; }
        .sidebar {
            width: 256px;
            background: #0f172a;
            color: #e2e8f0;
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
            background: linear-gradient(135deg, var(--primary-mid), var(--primary));
            display: grid;
            place-items: center;
            font-weight: 800;
            font-size: 15px;
            color: #fff;
            flex-shrink: 0;
        }
        .brand-title { font-weight: 700; font-size: 15px; line-height: 1.2; }
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
        .nav-link.active { background: var(--primary); color: #fff; font-weight: 600; }
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
        .page-title { font-size: 18px; font-weight: 700; margin: 0; }
        .content { padding: 22px 24px 40px; max-width: 1380px; width: 100%; margin: 0 auto; }
        .grid-4 { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 14px; margin-bottom: 20px; }
        .grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 18px;
            box-shadow: var(--shadow);
        }
        .kpi-label { font-size: 11.5px; font-weight: 700; color: var(--muted); text-transform: uppercase; letter-spacing: 0.4px; }
        .kpi-val { font-size: 24px; font-weight: 800; margin-top: 6px; font-variant-numeric: tabular-nums; }
        .kpi-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }
        .card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
        .card-title { font-size: 15px; font-weight: 700; margin: 0; }
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
        }
        .btn:hover { background: var(--surface-soft); }
        .btn-primary { background: var(--primary); color: #fff; border-color: var(--primary); }
        .btn-primary:hover { background: var(--primary-mid); }
        .btn-danger { background: var(--danger-bg); color: var(--danger); border-color: #fecaca; }
        .btn-danger:hover { background: #fecaca; }
        .btn-sm { padding: 5px 10px; font-size: 12px; border-radius: 7px; }
        .input, select, textarea {
            width: 100%;
            padding: 9px 12px;
            border: 1px solid var(--border);
            border-radius: 9px;
            font-size: 13.5px;
            background: #fff;
            color: var(--text);
            font-family: inherit;
        }
        .input:focus, select:focus, textarea:focus { outline: 2px solid var(--primary-soft); border-color: var(--primary); }
        .toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 13.5px; }
        th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #eef2f6; vertical-align: middle; }
        th { font-size: 11px; text-transform: uppercase; letter-spacing: 0.4px; color: var(--muted); background: #f8fafc; font-weight: 700; white-space: nowrap; }
        tr:hover td { background: #fafcff; }
        .num { font-variant-numeric: tabular-nums; white-space: nowrap; }
        .badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 999px;
            font-size: 11.5px;
            font-weight: 700;
            white-space: nowrap;
        }
        .badge-green { background: #e6f4ea; color: #1b5e20; }
        .badge-primary { background: var(--primary-soft); color: var(--primary); }
        .badge-red { background: var(--danger-bg); color: var(--danger); }
        .badge-amber { background: var(--warn-bg); color: var(--warn); }
        .badge-blue { background: var(--info-bg); color: var(--info); }
        .badge-gray { background: #e5e7eb; color: #374151; }
        .alert { padding: 11px 14px; border-radius: 10px; margin-bottom: 16px; font-size: 13.5px; font-weight: 600; }
        .alert-success { background: #e6f4ea; color: #1b5e20; border: 1px solid #bbf7d0; }
        .alert-error { background: var(--danger-bg); color: var(--danger); border: 1px solid #fecaca; }
        .modal-backdrop {
            display: none; position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55);
            z-index: 60; align-items: center; justify-content: center; padding: 16px;
        }
        .modal-backdrop.open { display: flex; }
        .modal {
            background: #fff; border-radius: 16px; width: 100%; max-width: 680px;
            max-height: 90vh; overflow-y: auto; padding: 20px 22px; box-shadow: 0 20px 50px rgba(0,0,0,0.25);
        }
        .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
        .form-full { grid-column: 1 / -1; }
        .field label { display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 4px; }
        .check-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
        .check-item {
            display: flex; align-items: center; gap: 8px; padding: 8px 10px;
            border: 1px solid var(--border); border-radius: 9px; font-size: 12.5px; cursor: pointer;
        }
        .swatch { display: inline-block; width: 14px; height: 14px; border-radius: 50%; vertical-align: middle; margin-right: 4px; }
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
            .sidebar, .topbar, .no-print { display: none !important; }
            body { background: #fff; }
            .card { box-shadow: none; border: none; padding: 0; }
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
        <a href="{{ route('admin.dashboard') }}" class="nav-link {{ request()->routeIs('admin.dashboard') ? 'active' : '' }}">📊 Dashboard</a>
        <a href="{{ route('admin.businesses') }}" class="nav-link {{ request()->routeIs('admin.businesses*') ? 'active' : '' }}">🏢 Businesses &amp; Themes</a>
        <a href="{{ route('admin.products') }}" class="nav-link {{ request()->routeIs('admin.products*') ? 'active' : '' }}">📦 Inventory &amp; Stock</a>
        <a href="{{ route('admin.invoices') }}" class="nav-link {{ request()->routeIs('admin.invoices*') ? 'active' : '' }}">🧾 POS Sales &amp; Invoices</a>
        <a href="{{ route('admin.expenses') }}" class="nav-link {{ request()->routeIs('admin.expenses*') ? 'active' : '' }}">💸 Expenses</a>
        <a href="{{ route('admin.reports') }}" class="nav-link {{ request()->routeIs('admin.reports*') ? 'active' : '' }}">📈 Monthly Reports</a>

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

<script>
    function openModal(id) { document.getElementById(id).classList.add('open'); }
    function closeModal(id) { document.getElementById(id).classList.remove('open'); }
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-backdrop.open').forEach(el => el.classList.remove('open'));
        }
    });
</script>
@stack('scripts')
</body>
</html>
