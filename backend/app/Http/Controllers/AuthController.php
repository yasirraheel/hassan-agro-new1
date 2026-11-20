<?php

namespace App\Http\Controllers;

use App\Services\RbacService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class AuthController extends Controller
{
    public function __construct(private readonly RbacService $rbac)
    {
    }

    public function login(Request $request): JsonResponse
    {
        $this->rbac->ensureSeeded();

        $email = strtolower(trim((string) $request->input('email', '')));
        $password = (string) $request->input('password', '');
        $deviceId = (string) ($request->input('deviceId', '') ?: $request->header('X-Device-Id', ''));

        if ($email === '' || $password === '') {
            return response()->json([
                'success' => false,
                'message' => 'Email and password are required.',
            ], 422);
        }

        $user = DB::table('app_users')->where('email', $email)->first();

        $envEmail = strtolower(trim((string) config('sync.admin_email', '')));
        $envPass = (string) config('sync.admin_password', '');

        $passwordMatches = false;
        if ($user && Hash::check($password, (string) $user->password_hash)) {
            $passwordMatches = true;
        } elseif ($user && $envEmail !== '' && hash_equals($envEmail, $email) && hash_equals($envPass, $password)) {
            $passwordMatches = true;
            DB::table('app_users')->where('id', $user->id)->update([
                'password_hash' => Hash::make($password),
                'updated_at_ms' => $this->rbac->nowMs(),
            ]);
        }

        if (!$user || !$passwordMatches) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid email or password.',
            ], 401);
        }

        if (!$user->is_active) {
            return response()->json([
                'success' => false,
                'message' => 'This admin account has been deactivated.',
            ], 403);
        }

        $token = $this->rbac->issueToken((string) $user->id, $deviceId);

        return response()->json([
            'success' => true,
            'message' => 'Signed in successfully.',
            'token' => $token,
            'user' => $this->rbac->formatUserPayload($user),
            'serverTime' => $this->rbac->nowMs(),
        ]);
    }

    /**
     * Self-signup: creates a new Business Owner user + their fully customizable Business
     * scoped strictly to their own business!
     */
    public function register(Request $request): JsonResponse
    {
        $this->rbac->ensureSeeded();

        $name = trim((string) $request->input('name', ''));
        $email = strtolower(trim((string) $request->input('email', '')));
        $password = (string) $request->input('password', '');
        $businessName = trim((string) $request->input('businessName', ''));
        $businessType = strtoupper(trim((string) $request->input('businessType', 'GENERAL')));
        $themeKey = strtoupper(trim((string) $request->input('themeKey', '')));
        $currencySymbol = trim((string) $request->input('currencySymbol', 'Rs.')) ?: 'Rs.';
        $deviceId = (string) ($request->input('deviceId', '') ?: $request->header('X-Device-Id', ''));

        if ($name === '' || $email === '' || strlen($password) < 6 || $businessName === '') {
            return response()->json([
                'success' => false,
                'message' => 'Name, valid email, password (min 6 chars), and business name are required.',
            ], 422);
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return response()->json([
                'success' => false,
                'message' => 'Please enter a valid email address.',
            ], 422);
        }

        if (DB::table('app_users')->where('email', $email)->exists()) {
            return response()->json([
                'success' => false,
                'message' => 'An account with this email already exists. Please sign in.',
            ], 409);
        }

        $presets = config('sync.business_presets', []);
        $preset = $presets[$businessType] ?? $presets['GENERAL'];
        $themes = config('sync.themes', []);
        $resolvedTheme = array_key_exists($themeKey, $themes) ? $themeKey : ($preset['default_theme'] ?? 'OCEAN');

        $now = $this->rbac->nowMs();
        $businessId = (string) Str::uuid();
        $userId = (string) Str::uuid();

        // Ensure "Business Owner" role exists (full control of their own business, without global Super Admin access)
        $ownerRoleId = 'role-business-owner';
        if (!DB::table('roles')->where('id', $ownerRoleId)->exists()) {
            DB::table('roles')->insert([
                'id' => $ownerRoleId,
                'name' => 'Business Owner',
                'description' => 'Full control & customization of their own business(es).',
                'permissions_json' => json_encode([
                    'view_dashboard', 'manage_inventory', 'use_pos', 'collect_credit',
                    'return_sales', 'manage_expenses', 'view_reports', 'export_data',
                    'manage_businesses', 'manage_settings',
                ]),
                'is_system' => false,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ]);
        }

        $prefix = strtoupper(preg_replace('/[^A-Za-z]/', '', $businessName));
        $prefix = substr($prefix !== '' ? $prefix : 'BM', 0, 3);

        DB::transaction(function () use (
            $businessId, $businessName, $businessType, $resolvedTheme, $currencySymbol,
            $preset, $prefix, $userId, $name, $email, $password, $ownerRoleId, $now
        ) {
            DB::table('businesses')->insert([
                'id' => $businessId,
                'name' => $businessName,
                'tagline' => (string) ($preset['tagline'] ?? 'Business Management'),
                'phone' => '',
                'email' => $email,
                'address' => '',
                'tax_id' => '',
                'invoice_prefix' => $prefix,
                'theme_key' => $resolvedTheme,
                'business_type' => $businessType,
                'currency_symbol' => substr($currencySymbol, 0, 12),
                'track_expiry' => (bool) ($preset['track_expiry'] ?? false),
                'track_batch' => (bool) ($preset['track_batch'] ?? false),
                'batch_field_label' => (string) ($preset['batch_field_label'] ?? 'Batch / Lot'),
                'pack_size_label' => (string) ($preset['pack_size_label'] ?? 'Pack / Size'),
                'product_categories_json' => json_encode($preset['product_categories'] ?? ['General', 'Other']),
                'expense_categories_json' => json_encode($preset['expense_categories'] ?? ['Stock Purchase', 'Rent', 'Utilities', 'Other']),
                'payment_methods_json' => json_encode($preset['payment_methods'] ?? ['Cash', 'Bank Transfer']),
                'pricing_tier_labels_json' => json_encode($preset['pricing_tier_labels'] ?? ['MRP' => 'MRP', 'DISTRIBUTOR' => 'Distributor', 'WHOLESALER' => 'Wholesaler', 'RETAIL' => 'Retail']),
                'default_tax_rate' => 0.0,
                'invoice_footer' => 'Thank you for your business!',
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
                'server_modified_at_ms' => $now,
                'is_deleted' => false,
            ]);

            DB::table('app_users')->insert([
                'id' => $userId,
                'name' => $name,
                'email' => $email,
                'password_hash' => Hash::make($password),
                'role_id' => $ownerRoleId,
                'business_ids_json' => json_encode([$businessId]),
                'is_active' => true,
                'last_login_ms' => $now,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ]);
        });

        $user = DB::table('app_users')->where('id', $userId)->first();
        $token = $this->rbac->issueToken($userId, $deviceId);

        return response()->json([
            'success' => true,
            'message' => "Welcome! {$businessName} is ready.",
            'token' => $token,
            'user' => $this->rbac->formatUserPayload($user),
            'businessId' => $businessId,
            'serverTime' => $this->rbac->nowMs(),
        ], 201);
    }

    public function me(Request $request): JsonResponse
    {
        $auth = $this->rbac->authenticateApiRequest($request);
        if (!$auth || $auth['type'] !== 'user' || !$auth['user']) {
            return response()->json([
                'success' => false,
                'message' => 'Session expired or revoked. Please sign in again.',
            ], 401);
        }

        return response()->json([
            'success' => true,
            'user' => $auth['user'],
            'serverTime' => $this->rbac->nowMs(),
        ]);
    }

    public function logout(Request $request): JsonResponse
    {
        $token = trim((string) $request->bearerToken());
        if ($token !== '') {
            DB::table('app_user_tokens')->where('token_hash', hash('sha256', $token))->delete();
        }

        return response()->json([
            'success' => true,
            'message' => 'Signed out successfully.',
        ]);
    }
}
