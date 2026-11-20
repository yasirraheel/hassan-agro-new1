# Hassan Agro — Laravel + MySQL Sync Backend (Hostinger Ready)

This folder is a complete Laravel 11 + MySQL sync API for the **Hassan Agro** Android app.

## Endpoints
- `GET /api/v1/health` — Verify connection & Bearer token
- `POST /api/v1/sync/push` — Transactional last-write-wins upload (businesses, products, POS invoices + items, expenses)
- `GET /api/v1/sync/pull?since=<epochMs>` — Incremental multi-device download

---

## Hostinger Shared Hosting Deployment

### 1. Create MySQL Database
In Hostinger **hPanel → Databases → Management**, create a MySQL database and note:
- `DB_DATABASE`
- `DB_USERNAME`
- `DB_PASSWORD`

### 2. Upload `backend-laravel` to Your Domain/Subdomain
Upload all files inside `backend-laravel/` to your subdomain folder (e.g. `domains/api.yourdomain.com/public_html`).
- A root `.htaccess` is already included to forward requests into `public/` and preserve the `Authorization: Bearer` header on Hostinger Apache/LiteSpeed.

### 3. Configure `.env`
Copy `.env.example` to `.env` and fill in your Hostinger MySQL credentials + a strong secret `SYNC_API_TOKEN`:

```env
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.yourdomain.com

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=u123456_hassanagro
DB_USERNAME=u123456_user
DB_PASSWORD=your_db_password

SYNC_API_TOKEN=your-strong-random-token
```

### 4. Install Dependencies & Create Tables (Hostinger SSH)
```bash
cd ~/domains/api.yourdomain.com/public_html
composer install --no-dev --optimize-autoloader
php artisan key:generate --force
php artisan migrate --force
php artisan config:cache
chmod -R 775 storage bootstrap/cache
```
*(Alternatively, you can import `database/schema.sql` directly inside Hostinger **phpMyAdmin** instead of running `php artisan migrate`.)*

### 5. Connect the Hassan Agro Android App
Open **Settings → Custom Server Sync**:
- **Server Base URL**: `https://api.yourdomain.com/`
- **API Key / Bearer Token**: exact `SYNC_API_TOKEN` from `.env`
- **WebSocket URL**: leave **empty**
- Enable **Auto-Sync Changes**, tap **Test**, then **Sync All Businesses Now**.
