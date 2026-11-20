<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login — Hassan Agro</title>
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            background: linear-gradient(135deg, #102a16 0%, #1b5e20 55%, #2e7d32 100%);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 16px;
        }
        .card {
            width: 100%;
            max-width: 400px;
            background: #fff;
            border-radius: 18px;
            padding: 28px;
            box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
        }
        .logo {
            width: 52px;
            height: 52px;
            border-radius: 14px;
            background: #1b5e20;
            color: #fff;
            display: grid;
            place-items: center;
            font-weight: 800;
            font-size: 20px;
            margin-bottom: 14px;
        }
        h1 { margin: 0 0 4px; font-size: 21px; color: #142218; }
        p { margin: 0 0 20px; font-size: 13.5px; color: #5a6d60; }
        label { display: block; font-size: 12.5px; font-weight: 600; color: #374151; margin-bottom: 5px; }
        input {
            width: 100%;
            padding: 11px 12px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 14px;
            margin-bottom: 14px;
        }
        input:focus { outline: 2px solid #86efac; border-color: #1b5e20; }
        button {
            width: 100%;
            padding: 12px;
            border: none;
            border-radius: 10px;
            background: #1b5e20;
            color: #fff;
            font-size: 14.5px;
            font-weight: 700;
            cursor: pointer;
        }
        button:hover { background: #2e7d32; }
        .msg {
            padding: 10px 12px;
            border-radius: 9px;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 14px;
        }
        .err { background: #fee2e2; color: #b91c1c; }
        .ok { background: #e6f4ea; color: #1b5e20; }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">HA</div>
        <h1>Hassan Agro Admin</h1>
        <p>Sign in to manage businesses, pesticide stock, POS sales, expenses, and mobile sync.</p>

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
