<?php

namespace App\Services;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class RbacService
{
    public function nowMs(): int
    {
        return (int) round(microtime(true) * 1000);
    }

    public function allPermissionKeys(): array
    {
        return array_keys(config('sync.permissions', []));
    }

    /**
     * Ensures default roles and primary Super Admin from .env exist in MySQL.
     */
    public function ensureSeeded(): void
    {
        $now = $this->nowMs();

        $defaultRoles = [
            [
                'id' => 'role-super-admin',
                'name' => 'Super Admin',
                'description' => 'Full access to all businesses, POS, stock, reports, roles, and staff management.',
                'permissions' => $this->allPermissionKeys(),
                'is_system' => true,
            ],
            [
                'id' => 'role-business-manager',
                'name' => 'Business Manager',
                'description' => 'Manage inventory, POS billing, credit collection, returns, expenses, and reports.',
                'permissions' => [
                    'view_dashboard', 'manage_inventory', 'use_pos', 'collect_credit',
                    'return_sales', 'manage_expenses', 'view_reports', 'export_data',
                ],
                'is_system' => false,
            ],
            [
                'id' => 'role-business-owner',
                'name' => 'Business Owner',
                'description' => 'Full control and customization of the assigned business(es).',
                'permissions' => [
                    'view_dashboard', 'manage_inventory', 'use_pos', 'collect_credit',
                    'return_sales', 'manage_expenses', 'view_reports', 'export_data',
                    'manage_businesses', 'manage_settings',
                ],
                'is_system' => false,
            ],
            [
                'id' => 'role-pos-cashier',
                'name' => 'POS Cashier',
                'description' => 'Create POS invoices, apply pricing tiers, and collect credit payments.',
                'permissions' => ['view_dashboard', 'use_pos', 'collect_credit'],
                'is_system' => false,
            ],
            [
                'id' => 'role-inventory-staff',
                'name' => 'Inventory & Expense Staff',
                'description' => 'Manage stock, scan/add products, restock batches, and record expenses.',
                'permissions' => ['view_dashboard', 'manage_inventory', 'manage_expenses'],
                'is_system' => false,
            ],
            [
                'id' => 'role-accountant',
                'name' => 'Accountant',
                'description' => 'View dashboard, monthly reports, growth charts, and export PDF/CSV data.',
                'permissions' => ['view_dashboard', 'view_reports', 'export_data'],
                'is_system' => false,
            ],
        ];

        foreach ($defaultRoles as $r) {
            if (!DB::table('roles')->where('id', $r['id'])->exists()
                && !DB::table('roles')->where('name', $r['name'])->exists()) {
                DB::table('roles')->insert([
                    'id' => $r['id'],
                    'name' => $r['name'],
                    'description' => $r['description'],
                    'permissions_json' => json_encode(array_values($r['permissions'])),
                    'is_system' => $r['is_system'],
                    'created_at_ms' => $now,
                    'updated_at_ms' => $now,
                ]);
            }
        }

        // Keep Super Admin role always synced with all permission keys
        DB::table('roles')->where('id', 'role-super-admin')->update([
            'permissions_json' => json_encode($this->allPermissionKeys()),
        ]);

        // Bootstrap primary Super Admin from .env if no users exist yet
        $envEmail = strtolower(trim((string) config('sync.admin_email', 'admin@hassanagro.com')));
        $envPass = (string) config('sync.admin_password', 'Admin@2026');
        $envName = (string) config('sync.admin_name', 'Super Admin');

        if ($envEmail !== '' && !DB::table('app_users')->where('email', $envEmail)->exists()) {
            DB::table('app_users')->insert([
                'id' => 'user-super-admin',
                'name' => $envName ?: 'Super Admin',
                'email' => $envEmail,
                'password_hash' => Hash::make($envPass),
                'role_id' => 'role-super-admin',
                'business_ids_json' => json_encode(['*']),
                'is_active' => true,
                'last_login_ms' => 0,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ]);
        }
    }

    /**
     * Builds the canonical user + role + permissions array returned to Android & Web.
     */
    public function formatUserPayload(object $user): array
    {
        $role = DB::table('roles')->where('id', $user->role_id)->first();
        $permissions = $role ? (json_decode((string) $role->permissions_json, true) ?: []) : [];
        $allowedBusinesses = json_decode((string) $user->business_ids_json, true);
        if (!is_array($allowedBusinesses) || empty($allowedBusinesses)) {
            $allowedBusinesses = ['*'];
        }

        return [
            'id' => (string) $user->id,
            'name' => (string) $user->name,
            'email' => (string) $user->email,
            'roleId' => (string) ($role->id ?? $user->role_id),
            'roleName' => (string) ($role->name ?? 'Staff'),
            'permissions' => array_values(array_unique(array_map('strval', $permissions))),
            'allowedBusinessIds' => array_values(array_unique(array_map('strval', $allowedBusinesses))),
        ];
    }

    /**
     * Authenticates an API request via user Bearer token OR legacy static SYNC_API_TOKEN.
     * Returns ['type' => 'user'|'static', 'user' => payload|null] or null if unauthorized.
     */
    public function authenticateApiRequest(Request $request): ?array
    {
        $this->ensureSeeded();
        $token = trim((string) $request->bearerToken());
        if ($token === '') {
            return null;
        }

        // 1. Check user auth tokens table
        $hash = hash('sha256', $token);
        $tokenRow = DB::table('app_user_tokens')->where('token_hash', $hash)->first();
        if ($tokenRow) {
            $user = DB::table('app_users')->where('id', $tokenRow->user_id)->first();
            if (!$user || !$user->is_active) {
                return null;
            }
            DB::table('app_user_tokens')->where('id', $tokenRow->id)->update([
                'last_used_ms' => $this->nowMs(),
            ]);
            return [
                'type' => 'user',
                'tokenId' => (string) $tokenRow->id,
                'user' => $this->formatUserPayload($user),
            ];
        }

        // 2. Fallback: static SYNC_API_TOKEN from .env (for server health checks / admin scripts)
        $staticToken = (string) config('sync.api_token', '');
        if ($staticToken !== '' && hash_equals($staticToken, $token)) {
            return [
                'type' => 'static',
                'tokenId' => null,
                'user' => null,
            ];
        }

        return null;
    }

    public function canAccessBusiness(?array $userPayload, string $businessId): bool
    {
        if ($userPayload === null) {
            return true;
        }
        $allowed = $userPayload['allowedBusinessIds'] ?? ['*'];
        return in_array('*', $allowed, true) || in_array($businessId, $allowed, true);
    }

    public function hasPermission(?array $userPayload, string $permission): bool
    {
        if ($userPayload === null) {
            return true;
        }
        return in_array($permission, $userPayload['permissions'] ?? [], true);
    }

    public function issueToken(string $userId, string $deviceId = ''): string
    {
        $raw = bin2hex(random_bytes(32));
        $now = $this->nowMs();
        DB::table('app_user_tokens')->insert([
            'id' => (string) Str::uuid(),
            'user_id' => $userId,
            'token_hash' => hash('sha256', $raw),
            'device_id' => substr(trim($deviceId), 0, 100),
            'created_at_ms' => $now,
            'last_used_ms' => $now,
        ]);
        DB::table('app_users')->where('id', $userId)->update([
            'last_login_ms' => $now,
        ]);
        return $raw;
    }
}
