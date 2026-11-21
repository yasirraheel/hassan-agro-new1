<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login — Business Manager</title>
    <script>
        (function() {
            try {
                var saved = localStorage.getItem('bm_admin_theme') || 'auto';
                var resolved = saved === 'auto'
                    ? (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
                    : saved;
                document.documentElement.setAttribute('data-theme', resolved);
            } catch (e) {}
        })();
    </script>
    <style>
        :root {
            --bg-grad: linear-gradient(135deg, #0f172a 0%, #0d47a1 55%, #1565c0 100%);
            --card-bg: #ffffff;
            --card-border: rgba(255, 255, 255, 0.15);
            --text: #111827;
            --muted: #5b6472;
            --label: #374151;
            --input-bg: #ffffff;
            --input-border: #d1d5db;
            --primary: #0d47a1;
            --primary-hover: #1565c0;
            --err-bg: #fee2e2;
            --err-text: #b91c1c;
            --ok-bg: #e6f4ea;
            --ok-text: #1b5e20;
        }
        [data-theme="dark"] {
            --bg-grad: radial-gradient(circle at top left, #1e293b 0%, #0b1120 60%, #020617 100%);
            --card-bg: #111827;
            --card-border: #2a374b;
            --text: #f1f5f9;
            --muted: #94a3b8;
            --label: #cbd5e1;
            --input-bg: #0f172a;
            --input-border: #334155;
            --primary: #3b82f6;
            --primary-hover: #60a5fa;
            --err-bg: rgba(239, 68, 68, 0.16);
            --err-text: #f87171;
            --ok-bg: rgba(34, 197, 94, 0.16);
            --ok-text: #4ade80;
            color-scheme: dark;
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            background: var(--bg-grad);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 16px;
        }
        .card {
            width: 100%;
            max-width: 410px;
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 18px;
            padding: 28px;
            box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
        }
        .logo {
            width: 52px;
            height: 52px;
            border-radius: 14px;
            background: var(--primary);
            color: #fff;
            display: grid;
            place-items: center;
            font-weight: 800;
            font-size: 20px;
            margin-bottom: 14px;
        }
        h1 { margin: 0 0 4px; font-size: 21px; color: var(--text); }
        p { margin: 0 0 20px; font-size: 13.5px; color: var(--muted); }
        label { display: block; font-size: 12.5px; font-weight: 600; color: var(--label); margin-bottom: 5px; }
        input {
            width: 100%;
            padding: 11px 12px;
            border: 1px solid var(--input-border);
            background: var(--input-bg);
            color: var(--text);
            border-radius: 10px;
            font-size: 14px;
            margin-bottom: 14px;
        }
        input:focus { outline: 2px solid rgba(59, 130, 246, 0.25); border-color: var(--primary); }
        button {
            width: 100%;
            padding: 12px;
            border: none;
            border-radius: 10px;
            background: var(--primary);
            color: #fff;
            font-size: 14.5px;
            font-weight: 700;
            cursor: pointer;
        }
        button:hover { background: var(--primary-hover); }
        .msg { padding: 10px 12px; border-radius: 9px; font-size: 13px; font-weight: 600; margin-bottom: 14px; }
        .err { background: var(--err-bg); color: var(--err-text); }
        .ok { background: var(--ok-bg); color: var(--ok-text); }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">BM</div>
        <h1>Business Manager Admin</h1>
        <p>Sign in to manage businesses, themes, custom categories, roles, staff accounts, inventory, and sales.</p>

        @if(session('error'))
            <div class="msg err">{{ session('error') }}</div>
        @endif
        @if(session('success'))
            <div class="msg ok">{{ session('success') }}</div>
        @endif

        <form method="POST" action="{{ route('admin.login.submit') }}">
            @csrf
            <label for="email">Admin Email</label>
            <input id="email" type="email" name="email" value="{{ old('email') }}" placeholder="admin@hassanagro.com" required autofocus>

            <label for="password">Password</label>
            <input id="password" type="password" name="password" placeholder="Enter admin password" required>

            <button type="submit">Sign In to Admin Panel</button>
        </form>
    </div>
</body>
</html>
