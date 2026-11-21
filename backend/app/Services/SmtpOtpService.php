<?php

namespace App\Services;

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\Schema;

class SmtpOtpService
{
    private const OTP_TTL_SECONDS = 600; // 10 minutes
    private const MAX_OTP_ATTEMPTS = 5;

    private const SETTING_KEYS = [
        'otp_verification_enabled',
        'smtp_host',
        'smtp_port',
        'smtp_encryption',
        'smtp_username',
        'smtp_password',
        'smtp_from_address',
        'smtp_from_name',
    ];

    public function nowMs(): int
    {
        return (int) round(microtime(true) * 1000);
    }

    /**
     * Self-healing table check so deployments work smoothly even before running artisan migrate.
     */
    public function ensureTablesExist(): void
    {
        if (!Schema::hasTable('app_settings')) {
            Schema::create('app_settings', function (Blueprint $table) {
                $table->string('key', 120)->primary();
                $table->text('value')->nullable();
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (!Schema::hasTable('email_otps')) {
            Schema::create('email_otps', function (Blueprint $table) {
                $table->bigIncrements('id');
                $table->string('email', 191)->index();
                $table->string('code_hash', 255);
                $table->string('business_name', 200)->default('');
                $table->bigInteger('expires_at_ms')->default(0);
                $table->bigInteger('consumed_at_ms')->nullable();
                $table->integer('attempts')->default(0);
                $table->bigInteger('created_at_ms')->default(0);
            });
        }

        if (Schema::hasTable('app_users')) {
            $missingFlag = !Schema::hasColumn('app_users', 'is_email_verified');
            $missingTime = !Schema::hasColumn('app_users', 'email_verified_at_ms');
            if ($missingFlag || $missingTime) {
                Schema::table('app_users', function (Blueprint $table) use ($missingFlag, $missingTime) {
                    if ($missingFlag) {
                        $table->boolean('is_email_verified')->default(true)->after('is_active');
                    }
                    if ($missingTime) {
                        $table->bigInteger('email_verified_at_ms')->nullable()->after('is_email_verified');
                    }
                });
            }
        }
    }

    public function isUserEmailVerified(?object $user): bool
    {
        if (!$user) {
            return true;
        }
        if (!property_exists($user, 'is_email_verified') || $user->is_email_verified === null) {
            return true;
        }
        return (bool) $user->is_email_verified;
    }

    public function markUserEmailVerified(string $userId, string $email = ''): void
    {
        $this->ensureTablesExist();
        $now = $this->nowMs();

        DB::table('app_users')->where('id', $userId)->update([
            'is_email_verified' => true,
            'email_verified_at_ms' => $now,
            'is_active' => true,
            'updated_at_ms' => $now,
        ]);

        $normalizedEmail = strtolower(trim($email));
        if ($normalizedEmail !== '') {
            DB::table('email_otps')
                ->where('email', $normalizedEmail)
                ->whereNull('consumed_at_ms')
                ->update(['consumed_at_ms' => $now]);
        }
    }

    public function getSettings(): array
    {
        $this->ensureTablesExist();

        $rows = DB::table('app_settings')
            ->whereIn('key', self::SETTING_KEYS)
            ->pluck('value', 'key');

        return [
            // OFF by default so registration works immediately until Admin turns OTP on
            'otp_verification_enabled' => ((string) ($rows['otp_verification_enabled'] ?? '0')) === '1',
            'smtp_host' => trim((string) ($rows['smtp_host'] ?? '')),
            'smtp_port' => (int) ($rows['smtp_port'] ?? 587) ?: 587,
            'smtp_encryption' => strtolower(trim((string) ($rows['smtp_encryption'] ?? 'tls'))) ?: 'tls',
            'smtp_username' => trim((string) ($rows['smtp_username'] ?? '')),
            'smtp_password' => (string) ($rows['smtp_password'] ?? ''),
            'smtp_from_address' => trim((string) ($rows['smtp_from_address'] ?? '')),
            'smtp_from_name' => trim((string) ($rows['smtp_from_name'] ?? '')),
        ];
    }

    public function isOtpEnabled(): bool
    {
        return (bool) $this->getSettings()['otp_verification_enabled'];
    }

    public function saveSettings(array $input): void
    {
        $this->ensureTablesExist();
        $existing = $this->getSettings();
        $now = $this->nowMs();

        $encryption = strtolower(trim((string) ($input['smtp_encryption'] ?? 'tls')));
        if (!in_array($encryption, ['tls', 'ssl', 'none'], true)) {
            $encryption = 'tls';
        }

        $port = (int) ($input['smtp_port'] ?? 587);
        if ($port <= 0 || $port > 65535) {
            $port = 587;
        }

        // Keep existing SMTP password if admin leaves the password field blank on edit
        $incomingPassword = (string) ($input['smtp_password'] ?? '');
        $resolvedPassword = $incomingPassword !== '' ? $incomingPassword : $existing['smtp_password'];

        $payload = [
            'otp_verification_enabled' => !empty($input['otp_verification_enabled']) ? '1' : '0',
            'smtp_host' => trim((string) ($input['smtp_host'] ?? '')),
            'smtp_port' => (string) $port,
            'smtp_encryption' => $encryption,
            'smtp_username' => trim((string) ($input['smtp_username'] ?? '')),
            'smtp_password' => $resolvedPassword,
            'smtp_from_address' => trim((string) ($input['smtp_from_address'] ?? '')),
            'smtp_from_name' => trim((string) ($input['smtp_from_name'] ?? '')),
        ];

        foreach ($payload as $key => $val) {
            DB::table('app_settings')->updateOrInsert(
                ['key' => $key],
                ['value' => $val, 'updated_at_ms' => $now]
            );
        }
    }

    public function applyRuntimeMailerConfig(array $settings, ?string $overrideFromName = null): void
    {
        $host = trim((string) ($settings['smtp_host'] ?? ''));
        $port = (int) ($settings['smtp_port'] ?? 587);
        $encryption = strtolower(trim((string) ($settings['smtp_encryption'] ?? 'tls')));
        $username = trim((string) ($settings['smtp_username'] ?? ''));
        $password = (string) ($settings['smtp_password'] ?? '');
        $fromAddress = trim((string) ($settings['smtp_from_address'] ?? '')) ?: $username;
        $fromName = trim((string) ($overrideFromName ?: ($settings['smtp_from_name'] ?? 'Business Manager'))) ?: 'Business Manager';

        if ($host === '' || $fromAddress === '') {
            throw new \RuntimeException('SMTP settings are incomplete. Please configure SMTP Host and From Email in Admin Settings.');
        }

        $scheme = $encryption === 'ssl' ? 'smtps' : 'smtp';
        $autoTls = $encryption === 'tls';

        config([
            'mail.default' => 'smtp',
            'mail.mailers.smtp' => [
                'transport' => 'smtp',
                'scheme' => $scheme,
                'url' => null,
                'host' => $host,
                'port' => $port > 0 ? $port : 587,
                'username' => $username !== '' ? $username : null,
                'password' => $password !== '' ? $password : null,
                'timeout' => 20,
                'local_domain' => parse_url((string) config('app.url', 'http://localhost'), PHP_URL_HOST) ?: 'localhost',
                'auto_tls' => $autoTls,
            ],
            'mail.from' => [
                'address' => $fromAddress,
                'name' => $fromName,
            ],
        ]);

        // Clear cached transport so updated Admin Panel credentials take effect immediately
        Mail::purge('smtp');
    }

    /**
     * Generates a 6-digit OTP, stores its hash in email_otps, and delivers a professional
     * business-branded HTML email via the Admin Panel SMTP configuration.
     */
    public function sendRegistrationOtp(
        string $email,
        string $userName,
        string $businessName,
        string $themeKey = 'OCEAN'
    ): array {
        $this->ensureTablesExist();
        $settings = $this->getSettings();

        $normalizedEmail = strtolower(trim($email));
        $cleanBusinessName = trim($businessName) ?: 'Your Business';
        $cleanUserName = trim($userName) ?: 'Business Owner';

        $themes = config('sync.themes', []);
        $theme = $themes[strtoupper($themeKey)] ?? ($themes['OCEAN'] ?? [
            'primary' => '#0D47A1',
            'accent' => '#1565C0',
            'soft' => '#E3F2FD',
        ]);

        $otpCode = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        $now = $this->nowMs();
        $expiresAtMs = $now + (self::OTP_TTL_SECONDS * 1000);

        // Invalidate any previous unconsumed OTPs for this email
        DB::table('email_otps')
            ->where('email', $normalizedEmail)
            ->whereNull('consumed_at_ms')
            ->update(['consumed_at_ms' => $now]);

        DB::table('email_otps')->insert([
            'email' => $normalizedEmail,
            'code_hash' => Hash::make($otpCode),
            'business_name' => $cleanBusinessName,
            'expires_at_ms' => $expiresAtMs,
            'consumed_at_ms' => null,
            'attempts' => 0,
            'created_at_ms' => $now,
        ]);

        // Brand the sender display name with the user's business name if Admin From Name is blank
        $senderName = $settings['smtp_from_name'] !== ''
            ? $settings['smtp_from_name']
            : $cleanBusinessName;

        $this->applyRuntimeMailerConfig($settings, $senderName);

        $subject = "{$cleanBusinessName}: Email Verification Code ({$otpCode})";

        Mail::mailer('smtp')->send('emails.registration_otp', [
            'businessName' => $cleanBusinessName,
            'userName' => $cleanUserName,
            'email' => $normalizedEmail,
            'otpCode' => $otpCode,
            'expiryMinutes' => (int) (self::OTP_TTL_SECONDS / 60),
            'primaryColor' => (string) ($theme['primary'] ?? '#0D47A1'),
            'accentColor' => (string) ($theme['accent'] ?? '#1565C0'),
            'softColor' => (string) ($theme['soft'] ?? '#E3F2FD'),
            'year' => date('Y'),
        ], function ($message) use ($normalizedEmail, $cleanUserName, $subject) {
            $message->to($normalizedEmail, $cleanUserName)->subject($subject);
        });

        return [
            'email' => $normalizedEmail,
            'expiresInSeconds' => self::OTP_TTL_SECONDS,
        ];
    }

    /**
     * Verifies and consumes the latest active OTP for an email address.
     * Returns ['ok' => bool, 'message' => string].
     */
    public function verifyAndConsumeOtp(string $email, string $otpCode): array
    {
        $this->ensureTablesExist();
        $normalizedEmail = strtolower(trim($email));
        $cleanCode = preg_replace('/\D+/', '', trim($otpCode));
        $now = $this->nowMs();

        if (strlen($cleanCode) !== 6) {
            return ['ok' => false, 'message' => 'Please enter a valid 6-digit verification code.'];
        }

        $record = DB::table('email_otps')
            ->where('email', $normalizedEmail)
            ->whereNull('consumed_at_ms')
            ->orderByDesc('id')
            ->first();

        if (!$record) {
            return ['ok' => false, 'message' => 'No active verification code found. Please request a new code.'];
        }

        if ((int) $record->expires_at_ms < $now) {
            DB::table('email_otps')->where('id', $record->id)->update(['consumed_at_ms' => $now]);
            return ['ok' => false, 'message' => 'This verification code has expired. Please tap Resend Code.'];
        }

        if ((int) $record->attempts >= self::MAX_OTP_ATTEMPTS) {
            DB::table('email_otps')->where('id', $record->id)->update(['consumed_at_ms' => $now]);
            return ['ok' => false, 'message' => 'Too many incorrect attempts. Please request a new verification code.'];
        }

        if (!Hash::check($cleanCode, (string) $record->code_hash)) {
            DB::table('email_otps')->where('id', $record->id)->increment('attempts');
            return ['ok' => false, 'message' => 'Invalid verification code. Please check your email and try again.'];
        }

        DB::table('email_otps')->where('id', $record->id)->update(['consumed_at_ms' => $now]);
        return ['ok' => true, 'message' => 'Email verified.'];
    }

    public function sendTestEmail(string $recipientEmail): void
    {
        $settings = $this->getSettings();
        $this->applyRuntimeMailerConfig($settings);

        $sampleCode = '482910';
        $fromName = $settings['smtp_from_name'] !== '' ? $settings['smtp_from_name'] : 'Business Manager';

        Mail::mailer('smtp')->send('emails.registration_otp', [
            'businessName' => $fromName,
            'userName' => 'Admin Test',
            'email' => $recipientEmail,
            'otpCode' => $sampleCode,
            'expiryMinutes' => 10,
            'primaryColor' => '#0D47A1',
            'accentColor' => '#1565C0',
            'softColor' => '#E3F2FD',
            'year' => date('Y'),
        ], function ($message) use ($recipientEmail, $fromName) {
            $message->to($recipientEmail)->subject("{$fromName}: SMTP Test Verification Email");
        });
    }
}
