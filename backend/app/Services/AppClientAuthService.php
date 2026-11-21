<?php

namespace App\Services;

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Http\Request;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

class AppClientAuthService
{
    private const ENFORCE_SETTING_KEY = 'enforce_app_attestation';
    private const DEFAULT_ANDROID_PACKAGE = 'com.aistudio.hassanagro.k7m2px';
    private const MAX_CLOCK_SKEW_SECONDS = 300; // +/- 5 minutes

    public function nowMs(): int
    {
        return (int) round(microtime(true) * 1000);
    }

    public function ensureTablesExist(): void
    {
        if (!Schema::hasTable('app_settings')) {
            Schema::create('app_settings', function (Blueprint $table) {
                $table->string('key', 120)->primary();
                $table->text('value')->nullable();
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (!Schema::hasTable('authorized_apps')) {
            Schema::create('authorized_apps', function (Blueprint $table) {
                $table->string('id', 64)->primary();
                $table->string('name', 120);
                $table->string('package_name', 191)->unique();
                $table->string('secret_key', 128);
                $table->string('cert_sha256', 128)->nullable();
                $table->string('last_seen_cert_sha256', 128)->nullable();
                $table->bigInteger('last_seen_at_ms')->default(0);
                $table->boolean('is_active')->default(true);
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (DB::table('authorized_apps')->count() === 0) {
            $now = $this->nowMs();
            DB::table('authorized_apps')->insert([
                'id' => 'app-default-android',
                'name' => 'Business Manager Android',
                'package_name' => self::DEFAULT_ANDROID_PACKAGE,
                'secret_key' => $this->generateSecretKey(),
                'cert_sha256' => null,
                'last_seen_cert_sha256' => null,
                'last_seen_at_ms' => 0,
                'is_active' => true,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ]);
        }
    }

    public function generateSecretKey(): string
    {
        return bin2hex(random_bytes(32)); // 64-char hex (256-bit)
    }

    public function normalizeCert(?string $raw): ?string
    {
        $clean = strtoupper(preg_replace('/[^a-fA-F0-9]/', '', (string) $raw));
        if ($clean === '') {
            return null;
        }
        return $clean;
    }

    public function formatCert(?string $hex): string
    {
        $clean = $this->normalizeCert($hex);
        if ($clean === null) {
            return '';
        }
        return implode(':', str_split($clean, 2));
    }

    public function isEnforcementEnabled(): bool
    {
        $this->ensureTablesExist();
        $val = DB::table('app_settings')
            ->where('key', self::ENFORCE_SETTING_KEY)
            ->value('value');
        return ((string) $val) === '1';
    }

    public function setEnforcementEnabled(bool $enabled): void
    {
        $this->ensureTablesExist();
        DB::table('app_settings')->updateOrInsert(
            ['key' => self::ENFORCE_SETTING_KEY],
            [
                'value' => $enabled ? '1' : '0',
                'updated_at_ms' => $this->nowMs(),
            ]
        );
    }

    public function listApps(): Collection
    {
        $this->ensureTablesExist();
        return DB::table('authorized_apps')
            ->orderBy('created_at_ms')
            ->get()
            ->map(function ($row) {
                $row->cert_formatted = $this->formatCert($row->cert_sha256 ?? null);
                $row->last_seen_cert_formatted = $this->formatCert($row->last_seen_cert_sha256 ?? null);
                return $row;
            });
    }

    public function createApp(string $name, string $packageName, ?string $certSha256 = null): object
    {
        $this->ensureTablesExist();
        $cleanPkg = trim($packageName);
        if ($cleanPkg === '') {
            throw new \RuntimeException('Package name is required.');
        }
        if (DB::table('authorized_apps')->where('package_name', $cleanPkg)->exists()) {
            throw new \RuntimeException("Package name {$cleanPkg} is already registered.");
        }

        $normalizedCert = $this->normalizeCert($certSha256);
        if ($normalizedCert !== null && strlen($normalizedCert) !== 64) {
            throw new \RuntimeException('Signing certificate SHA-256 must be exactly 64 hex characters.');
        }

        $now = $this->nowMs();
        $id = (string) Str::uuid();
        DB::table('authorized_apps')->insert([
            'id' => $id,
            'name' => trim($name) ?: $cleanPkg,
            'package_name' => $cleanPkg,
            'secret_key' => $this->generateSecretKey(),
            'cert_sha256' => $normalizedCert,
            'last_seen_cert_sha256' => null,
            'last_seen_at_ms' => 0,
            'is_active' => true,
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
        ]);

        return DB::table('authorized_apps')->where('id', $id)->first();
    }

    public function rotateKey(string $id): ?object
    {
        $this->ensureTablesExist();
        $row = DB::table('authorized_apps')->where('id', $id)->first();
        if (!$row) {
            return null;
        }

        DB::table('authorized_apps')->where('id', $id)->update([
            'secret_key' => $this->generateSecretKey(),
            'updated_at_ms' => $this->nowMs(),
        ]);

        return DB::table('authorized_apps')->where('id', $id)->first();
    }

    public function toggleApp(string $id): ?object
    {
        $this->ensureTablesExist();
        $row = DB::table('authorized_apps')->where('id', $id)->first();
        if (!$row) {
            return null;
        }

        DB::table('authorized_apps')->where('id', $id)->update([
            'is_active' => !((bool) $row->is_active),
            'updated_at_ms' => $this->nowMs(),
        ]);

        return DB::table('authorized_apps')->where('id', $id)->first();
    }

    public function updateCertLock(string $id, ?string $certSha256): ?object
    {
        $this->ensureTablesExist();
        $row = DB::table('authorized_apps')->where('id', $id)->first();
        if (!$row) {
            return null;
        }

        $normalized = $this->normalizeCert($certSha256);
        if ($normalized !== null && strlen($normalized) !== 64) {
            throw new \RuntimeException('Signing certificate SHA-256 must be exactly 64 hex characters.');
        }

        DB::table('authorized_apps')->where('id', $id)->update([
            'cert_sha256' => $normalized,
            'updated_at_ms' => $this->nowMs(),
        ]);

        return DB::table('authorized_apps')->where('id', $id)->first();
    }

    public function deleteApp(string $id): bool
    {
        $this->ensureTablesExist();
        return DB::table('authorized_apps')->where('id', $id)->delete() > 0;
    }

    /**
     * Validates X-App-Package + X-App-Cert + X-App-Timestamp + X-App-Signature headers.
     * Always records the latest detected APK certificate SHA-256 for matching packages
     * so the admin can lock to it with one click before enabling strict enforcement.
     */
    public function verifyRequest(Request $request): array
    {
        $this->ensureTablesExist();

        $package = trim((string) $request->header('X-App-Package', ''));
        $providedCert = $this->normalizeCert($request->header('X-App-Cert'));
        $timestampRaw = trim((string) $request->header('X-App-Timestamp', ''));
        $signature = strtolower(trim((string) $request->header('X-App-Signature', '')));

        $app = $package !== ''
            ? DB::table('authorized_apps')->where('package_name', $package)->first()
            : null;

        if ($app) {
            $update = ['last_seen_at_ms' => $this->nowMs()];
            if ($providedCert !== null && strlen($providedCert) === 64) {
                $update['last_seen_cert_sha256'] = $providedCert;
            }
            DB::table('authorized_apps')->where('id', $app->id)->update($update);
        }

        if (!$this->isEnforcementEnabled()) {
            return ['allowed' => true, 'reason' => 'enforcement_off'];
        }

        if ($package === '' || $timestampRaw === '' || $signature === '') {
            return ['allowed' => false, 'reason' => 'missing_attestation_headers'];
        }

        if (!ctype_digit($timestampRaw)) {
            return ['allowed' => false, 'reason' => 'invalid_timestamp'];
        }

        $nowSec = time();
        if (abs($nowSec - (int) $timestampRaw) > self::MAX_CLOCK_SKEW_SECONDS) {
            return ['allowed' => false, 'reason' => 'expired_timestamp'];
        }

        if (!$app || !((bool) $app->is_active)) {
            return ['allowed' => false, 'reason' => 'unrecognized_or_disabled_package'];
        }

        $lockedCert = $this->normalizeCert($app->cert_sha256 ?? null);
        if ($lockedCert !== null) {
            if ($providedCert === null || !hash_equals($lockedCert, $providedCert)) {
                return ['allowed' => false, 'reason' => 'certificate_mismatch'];
            }
        }

        $method = strtoupper($request->getMethod());
        $certForSign = $providedCert ?? '';
        $uriPath = (string) (parse_url($request->getRequestUri(), PHP_URL_PATH) ?: '');
        $laravelPath = '/' . ltrim($request->path(), '/');

        $candidatePaths = array_values(array_unique(array_filter([$uriPath, $laravelPath])));
        $matchedSignature = false;
        foreach ($candidatePaths as $path) {
            $canonical = "{$package}|{$certForSign}|{$timestampRaw}|{$method}|{$path}";
            $expected = hash_hmac('sha256', $canonical, (string) $app->secret_key);
            if (hash_equals($expected, $signature)) {
                $matchedSignature = true;
                break;
            }
        }

        if (!$matchedSignature) {
            return ['allowed' => false, 'reason' => 'invalid_signature'];
        }

        return ['allowed' => true, 'reason' => 'verified'];
    }
}
