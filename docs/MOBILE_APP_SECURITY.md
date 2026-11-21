# Mobile app security and release checklist

This project keeps the mobile client configuration out of the user-facing app
screens. The current Android application ID is `com.business.manager`.

## What the security layer does

- The app's login and business-creation screens do not ask users for a server
  URL or API token.
- Sync settings expose cloud-sync status, auto-sync, and manual sync only.
- Gradle reads `BACKEND_BASE_URL`, `APP_CLIENT_SECRET`, and the optional
  `BACKEND_WS_URL` at build time. The values are sealed into `BuildConfig` with
  AES-256-GCM using the application ID and a random build salt; plaintext URL
  and secret are not emitted into `BuildConfig`.
- Each request is attested with `X-App-Package`, `X-App-Cert`,
  `X-App-Timestamp`, and `X-App-Signature`. The signature is HMAC-SHA256 over
  the package, certificate, timestamp, HTTP method, and request path. The raw
  secret is never sent over the network.
- Laravel records the latest certificate seen for a registered package. Strict
  mode can require a registered/active package, a valid signature, a fresh
  timestamp, and the locked signing certificate.
- Network errors are sanitized before they reach the UI, so server hostnames
  and IP addresses are not shown to end users.

## First-time setup

1. Deploy the backend and open **Admin Panel → Settings → Mobile App Security**.
2. Register or select `com.business.manager` and copy its generated 64-character
   App Secret Key. Do not commit this key to Git.
3. In AI Studio's Secrets panel, or in a local untracked `android-app/.env`, set:

   ```text
   BACKEND_BASE_URL=https://ledger.hassanagro.com/
   APP_CLIENT_SECRET=<64-character key from the Admin Panel>
   ```

   `android-app/.env.example` is a template only; replace the placeholder in
   the untracked `.env`/Secrets panel.

4. Compile and sign the app. Install it on a test device and perform login or
   sync once. The Admin Panel should show the latest detected certificate SHA-256.
5. Click **Lock to Detected Cert** (or enter the Google Play App Signing
   SHA-256 for a Play release), test another request, then enable **Strict App
   Verification**.

## Key rotation and package changes

- Rotate the App Secret Key in the Admin Panel whenever a build secret may have
  leaked, then update the build secret and release a new APK.
- If the application ID changes, register the new package, build with that
  package and its matching key, verify its detected certificate, and lock it
  before enabling strict mode. The old package can then be disabled/deleted.
- The package rename from `com.aistudio.hassanagro.k7m2px` to
  `com.business.manager` is handled by a backend migration that preserves the
  existing authorized-app key when possible and disables a duplicate legacy
  registration if both exist.

## Local verification

From `android-app/`:

```powershell
gradle :app:compileDebugKotlin --no-daemon --console=plain
```

Do not put real secrets in tracked files, APK screenshots, logs, or commit
messages. APK-embedded secrets can still be extracted by advanced reverse
engineering; certificate locking, rotation, Play App Signing, and eventually
Play Integrity reduce that risk but do not make a client secret absolute.
