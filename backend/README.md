# Hassan Agro — Laravel + MySQL Sync Backend (Hostinger Ready)

This folder is a Laravel 12 + MySQL sync API for the **Hassan Agro** Android app.

## Endpoints
- `GET /api/v1/health` — Verify connection & Bearer token
- `POST /api/v1/sync/push` — Transactional last-write-wins upload (businesses, products, POS invoices + items, expenses)
- `GET /api/v1/sync/pull?since=<epochMs>` — Incremental multi-device download
- `GET /admin/login` — Admin dashboard sign-in

---

## Hostinger Shared Hosting Deployment

### 1. Create MySQL Database
In Hostinger **hPanel → Databases → Management**, create a MySQL database and note:
- `DB_DATABASE`
- `DB_USERNAME`
- `DB_PASSWORD`

### 2. Deploy only the backend to ledger.hassanagro.com
On the Hassan Agro Hostinger account, run `bash backend/scripts/deploy-hostinger.sh`
from a checkout of this repository. For later deployments, run the script from
`~/.local/share/hassan-agro-ledger/source/backend/scripts/deploy-hostinger.sh`.
It performs a sparse Git pull outside `public_html`, then copies only the
contents of `backend` into `domains/hassanagro.com/public_html/ledger`.
The existing `.env`, `vendor`, storage, and cache directories are preserved.
Each run backs up the previous files outside the web folder.

### 3. Configure `.env`
Create `domains/hassanagro.com/public_html/ledger/.env` from `.env.example`
and fill in your Hostinger MySQL credentials, a strong `SYNC_API_TOKEN`, and
separate admin credentials:

```env
APP_ENV=production
APP_DEBUG=false
APP_URL=https://ledger.hassanagro.com

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=u123456_hassanagro
DB_USERNAME=u123456_user
DB_PASSWORD=your_db_password

SYNC_API_TOKEN=your-strong-random-token
ADMIN_EMAIL=admin@hassanagro.com
ADMIN_PASSWORD=your-unique-admin-password
SESSION_DRIVER=file
SESSION_SECURE_COOKIE=true
```

### 4. Install Dependencies & Create Tables
The deployment script runs Composer, creates an app key if needed, runs
migrations, and caches configuration. Keep `.env` outside Git.

### 5. Connect the Hassan Agro Android App
Open **Settings → Custom Server Sync**:
- **Server Base URL**: `https://ledger.hassanagro.com/`
- **API Key / Bearer Token**: exact `SYNC_API_TOKEN` from `.env`
- **WebSocket URL**: leave **empty**
- Enable **Auto-Sync Changes**, tap **Test**, then **Sync All Businesses Now**.
