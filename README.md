# Hassan Agro

This repository contains two projects:

- `android-app/` — Android Studio and Gradle project. Open this folder in Android Studio.
- `backend/` — Laravel API deployed to `https://ledger.hassanagro.com/`.

The repository uses one `main` branch. A normal `git pull --ff-only` in this
checkout updates both folders in their own paths. Git commands run inside either
folder still operate on this same repository.

The Hostinger server uses a sparse checkout that contains only `backend/`. To
pull and deploy backend changes there, run:

```bash
bash ~/.local/share/hassan-agro-ledger/source/backend/scripts/deploy-hostinger.sh
```

To make a separate Android-only checkout on another machine:

```bash
git clone --filter=blob:none --sparse https://github.com/yasirraheel/hassan-agro-new1.git hassan-agro-android
cd hassan-agro-android
git sparse-checkout set --no-cone /android-app/
git pull --ff-only
```

That checkout will contain `android-app/` and no backend files.

## Android app security and package setup

The Android application ID is `com.business.manager`. The app does not expose
the server URL or API key in its login/settings screens. At build time, Gradle
reads `BACKEND_BASE_URL` and `APP_CLIENT_SECRET`, then seals them with
AES-256-GCM using the application ID and a random build salt. Every API request
also carries package, signing-certificate, timestamp, and HMAC-SHA256
attestation headers.

Before compiling a release or debug build:

1. Open the Laravel Admin Panel and go to **Settings → Mobile App Security**.
2. Register/choose package `com.business.manager` and copy its generated
   64-character App Secret Key. Rotate the key whenever a build secret may
   have leaked.
3. In AI Studio Secrets (or local `android-app/.env`), set:

   ```text
   BACKEND_BASE_URL=https://ledger.hassanagro.com/
   APP_CLIENT_SECRET=<key copied from the Admin Panel>
   ```

4. Compile the app, sign/install it, then return to Mobile App Security. The
   server records the APK signing certificate as `last_seen_cert_sha256`.
   Click **Lock to Detected Cert**, verify a real request, and only then enable
   **Strict App Verification**.

The strict mode blocks unknown packages, disabled registrations, stale request
timestamps, invalid HMAC signatures, and mismatched locked certificates. A
client secret embedded in an APK is not an absolute secret against advanced
reverse engineering; certificate pinning, key rotation, and (for Play Store
releases) Google Play App Signing/Integrity are still recommended.

For the full operational checklist, see
[`docs/MOBILE_APP_SECURITY.md`](docs/MOBILE_APP_SECURITY.md).
