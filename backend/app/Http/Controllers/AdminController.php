<?php

namespace App\Http\Controllers;

use App\Services\AppClientAuthService;
use App\Services\ProfitLossService;
use App\Services\RbacService;
use App\Services\SmtpOtpService;
use Carbon\Carbon;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;
use Illuminate\View\View;
use Symfony\Component\HttpFoundation\StreamedResponse;

class AdminController extends Controller
{
    public function __construct(
        private readonly RbacService $rbac,
        private readonly ProfitLossService $pl,
        private readonly SmtpOtpService $smtpOtp,
        private readonly AppClientAuthService $appAuth
    ) {
    }

    private function activeBusinesses()
    {
        return DB::table('businesses')
            ->where('is_deleted', false)
            ->orderBy('created_at_ms')
            ->get();
    }

    private function selectedBusinessId(Request $request): string
    {
        if ($request->has('business_id')) {
            $id = (string) $request->query('business_id', '');
            $request->session()->put('admin_selected_business_id', $id);
            return $id;
        }
        return (string) $request->session()->get('admin_selected_business_id', '');
    }

    private function currentAdminId(Request $request): string
    {
        return (string) $request->session()->get('bm_admin_id', 'user-super-admin');
    }

    private function currentAdminName(Request $request): string
    {
        return (string) $request->session()->get('bm_admin_name', 'Super Admin');
    }

    private function activeTheme(Request $request, $businesses): array
    {
        $themes = config('sync.themes', []);
        $selectedId = $this->selectedBusinessId($request);
        $biz = $businesses->firstWhere('id', $selectedId);
        $key = strtoupper((string) ($biz->theme_key ?? 'OCEAN'));
        return $themes[$key] ?? ($themes['OCEAN'] ?? ['label' => 'Ocean Blue', 'primary' => '#0D47A1', 'accent' => '#1565C0', 'soft' => '#E3F2FD']);
    }

    // ---------- Auth ----------

    public function showLogin(Request $request): View|RedirectResponse
    {
        $this->rbac->ensureSeeded();
        if ($request->session()->get('bm_admin_logged_in', false)) {
            return redirect()->route('admin.dashboard');
        }
        return view('admin.login');
    }

    public function login(Request $request): RedirectResponse
    {
        $this->rbac->ensureSeeded();

        $request->validate([
            'email' => ['required', 'string'],
            'password' => ['required', 'string'],
        ]);

        $email = strtolower(trim((string) $request->input('email')));
        $password = (string) $request->input('password');

        $envEmail = strtolower(trim((string) config('sync.admin_email', 'admin@hassanagro.com')));
        $envPass = (string) config('sync.admin_password', 'Admin@2026');

        // Check app_users first (allows any Super Admin or manage_roles user to log into Web Admin Panel)
        $user = DB::table('app_users')->where('email', $email)->first();
        if ($user) {
            $passOk = Hash::check($password, (string) $user->password_hash)
                || ($envEmail !== '' && hash_equals($envEmail, $email) && hash_equals($envPass, $password));

            if ($passOk) {
                if (!$user->is_active) {
                    return back()->withInput(['email' => $email])->with('error', 'Account is deactivated.');
                }
                $payload = $this->rbac->formatUserPayload($user);
                if (!in_array('manage_roles', $payload['permissions'], true) && $payload['roleName'] !== 'Super Admin') {
                    return back()->withInput(['email' => $email])
                        ->with('error', 'This staff account is for the mobile app and does not have Web Admin Panel permission.');
                }
                $request->session()->regenerate();
                $request->session()->put('bm_admin_logged_in', true);
                $request->session()->put('bm_admin_id', (string) $user->id);
                $request->session()->put('bm_admin_name', (string) $user->name);
                $request->session()->put('bm_admin_email', (string) $user->email);
                $request->session()->put('bm_admin_role', (string) $payload['roleName']);
                return redirect()->route('admin.dashboard');
            }
        } elseif ($envEmail !== '' && hash_equals($envEmail, $email) && hash_equals($envPass, $password)) {
            $request->session()->regenerate();
            $request->session()->put('bm_admin_logged_in', true);
            $request->session()->put('bm_admin_id', 'user-super-admin');
            $request->session()->put('bm_admin_name', (string) config('sync.admin_name', 'Super Admin'));
            $request->session()->put('bm_admin_email', $envEmail);
            $request->session()->put('bm_admin_role', 'Super Admin');
            return redirect()->route('admin.dashboard');
        }

        return back()->withInput(['email' => $email])->with('error', 'Invalid admin email or password.');
    }

    public function logout(Request $request): RedirectResponse
    {
        $request->session()->invalidate();
        $request->session()->regenerateToken();
        return redirect()->route('admin.login')->with('success', 'Signed out safely.');
    }

    // ---------- Dashboard ----------

    public function dashboard(Request $request): View
    {
        $this->rbac->ensureSeeded();
        $this->smtpOtp->ensureTablesExist();
        $businesses = $this->activeBusinesses();
        $this->attachOwnerDetails($businesses);
        $unverifiedBusinessCount = $businesses->filter(fn ($b) => !empty($b->owner_id) && !$b->owner_is_verified)->count();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $nowMs = $this->rbac->nowMs();
        $thirtyDaysMs = $nowMs + (30 * 86400 * 1000);

        $periodLabels = ProfitLossService::PERIOD_LABELS;
        if ($request->has('period')) {
            $reqPeriod = strtolower((string) $request->query('period', 'month'));
            $selectedPeriod = array_key_exists($reqPeriod, $periodLabels) ? $reqPeriod : 'month';
            $request->session()->put('admin_dashboard_period', $selectedPeriod);
        } else {
            $selectedPeriod = (string) $request->session()->get('admin_dashboard_period', 'month');
            if (!array_key_exists($selectedPeriod, $periodLabels)) {
                $selectedPeriod = 'month';
            }
        }

        [$startMs, $endMs] = $this->pl->periodBounds($selectedPeriod);
        $pl = $this->pl->summarize($selectedBizId, $startMs, $endMs, $nowMs);

        $totalRevenue = $pl['totalSales'];
        $totalCollected = $pl['totalCollected'];
        $totalCreditDue = $pl['totalCreditDue'];
        $invoiceCount = $pl['invoiceCount'];
        $totalExpenses = $pl['operatingExpenses'];
        $netProfit = $pl['netProfit'];

        $products = $pl['products'];
        $totalProducts = $pl['totalProducts'];
        $totalUnits = $pl['totalUnits'];
        $lowStockCount = $pl['lowStockCount'];
        $expiredCount = $pl['expiredCount'];
        $expiringSoonCount = $pl['expiringSoonCount'];

        $activeStaffCount = DB::table('app_users')->where('is_active', true)->count();

        $alertProducts = $products->filter(function ($p) use ($nowMs, $thirtyDaysMs) {
            $low = (int) $p->stock_qty <= (int) $p->low_stock_threshold;
            $exp = (int) $p->expiry_date_millis > 0 && (int) $p->expiry_date_millis <= $thirtyDaysMs;
            return $low || $exp;
        })->sortBy('expiry_date_millis')->take(8);

        $recentInvoices = DB::table('invoices')
            ->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId))
            ->when($startMs !== null && $endMs !== null, fn ($q) => $q->whereBetween('created_at_ms', [$startMs, $endMs]))
            ->orderByDesc('created_at_ms')
            ->limit(8)
            ->get();

        return view('admin.dashboard', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme',
            'selectedPeriod', 'periodLabels', 'pl', 'unverifiedBusinessCount',
            'totalRevenue', 'totalCollected', 'totalCreditDue', 'invoiceCount',
            'totalExpenses', 'netProfit', 'totalProducts', 'totalUnits',
            'lowStockCount', 'expiredCount', 'expiringSoonCount', 'activeStaffCount',
            'alertProducts', 'recentInvoices', 'nowMs', 'thirtyDaysMs'
        ));
    }

    // ---------- Businesses ----------

    public function businesses(Request $request): View
    {
        $this->rbac->ensureSeeded();
        $this->smtpOtp->ensureTablesExist();
        $businesses = $this->activeBusinesses();
        $this->attachOwnerDetails($businesses);
        $unverifiedBusinesses = $businesses->filter(fn ($b) => !empty($b->owner_id) && !$b->owner_is_verified)->values();
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $themes = config('sync.themes', []);
        $presets = config('sync.business_presets', []);

        $productCounts = DB::table('products')->where('is_deleted', false)
            ->selectRaw('business_id, COUNT(*) as cnt')->groupBy('business_id')->pluck('cnt', 'business_id');
        $invoiceCounts = DB::table('invoices')->where('is_deleted', false)
            ->selectRaw('business_id, COUNT(*) as cnt, COALESCE(SUM(CASE WHEN is_returned = 0 THEN grand_total ELSE 0 END), 0) as rev')
            ->groupBy('business_id')->get()->keyBy('business_id');
        $expenseSums = DB::table('expenses')->where('is_deleted', false)
            ->selectRaw('business_id, COALESCE(SUM(amount), 0) as total')->groupBy('business_id')->pluck('total', 'business_id');

        return view('admin.businesses', compact(
            'businesses', 'unverifiedBusinesses', 'selectedBizId', 'activeTheme', 'themes', 'presets',
            'productCounts', 'invoiceCounts', 'expenseSums'
        ));
    }

    private function parseCommaList(?string $csv, array $fallback): string
    {
        $items = array_values(array_unique(array_filter(array_map('trim', explode(',', (string) $csv)))));
        return json_encode(!empty($items) ? $items : $fallback);
    }

    private function ensureBusinessOwnerRole(): string
    {
        $roleId = 'role-business-owner';
        $now = $this->rbac->nowMs();
        if (!DB::table('roles')->where('id', $roleId)->exists()) {
            DB::table('roles')->insert([
                'id' => $roleId,
                'name' => 'Business Owner',
                'description' => 'Full control and customization of the assigned business(es).',
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
        return $roleId;
    }

    private function findBusinessOwner(object $business): ?object
    {
        $ownerId = (string) ($business->owner_user_id ?? '');
        if ($ownerId !== '') {
            $owner = DB::table('app_users')->where('id', $ownerId)->first();
            if ($owner) {
                return $owner;
            }
        }

        // Link legacy owners created before owner_user_id was added.
        $users = DB::table('app_users')->where('id', '!=', 'user-super-admin')->get();
        foreach ($users as $user) {
            $businessIds = json_decode((string) $user->business_ids_json, true) ?: [];
            if (in_array((string) $business->id, array_map('strval', $businessIds), true)) {
                return $user;
            }
        }
        return null;
    }

    private function attachOwnerDetails($businesses): void
    {
        foreach ($businesses as $business) {
            $owner = $this->findBusinessOwner($business);
            $business->owner_id = $owner?->id;
            $business->owner_email = $owner?->email;
            $business->owner_name = $owner?->name;
            $business->owner_is_verified = $owner ? $this->smtpOtp->isUserEmailVerified($owner) : true;
        }
    }

    private function syncBusinessOwner(string $businessId, string $businessName, array $data): void
    {
        $ownerEmail = strtolower(trim((string) ($data['owner_email'] ?? '')));
        $ownerPassword = (string) ($data['owner_password'] ?? '');
        if ($ownerEmail === '' && $ownerPassword === '') {
            return;
        }

        $business = DB::table('businesses')->where('id', $businessId)->first();
        $owner = $business ? $this->findBusinessOwner($business) : null;
        if ($ownerEmail !== '' && DB::table('app_users')->where('email', $ownerEmail)
            ->when($owner, fn ($q) => $q->where('id', '!=', $owner->id))->exists()) {
            throw new \RuntimeException('This owner email is already assigned to another account.');
        }

        $roleId = $this->ensureBusinessOwnerRole();
        $now = $this->rbac->nowMs();
        if (!$owner) {
            if ($ownerEmail === '' || $ownerPassword === '') {
                throw new \RuntimeException('Owner email and a password are required for a new business login.');
            }
            $ownerId = (string) Str::uuid();
            DB::table('app_users')->insert([
                'id' => $ownerId,
                'name' => trim((string) ($data['owner_name'] ?? '')) ?: $businessName,
                'email' => $ownerEmail,
                'password_hash' => Hash::make($ownerPassword),
                'role_id' => $roleId,
                'business_ids_json' => json_encode([$businessId]),
                'is_active' => true,
                'last_login_ms' => 0,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ]);
            DB::table('businesses')->where('id', $businessId)->update(['owner_user_id' => $ownerId]);
            return;
        }

        $update = [
            'role_id' => $roleId,
            'business_ids_json' => json_encode([$businessId]),
            'is_active' => true,
            'updated_at_ms' => $now,
        ];
        $ownerName = trim((string) ($data['owner_name'] ?? ''));
        if ($ownerName !== '') {
            $update['name'] = $ownerName;
        }
        if ($ownerEmail !== '') {
            $update['email'] = $ownerEmail;
        }
        if ($ownerPassword !== '') {
            $update['password_hash'] = Hash::make($ownerPassword);
            DB::table('app_user_tokens')->where('user_id', $owner->id)->delete();
        }
        DB::table('app_users')->where('id', $owner->id)->update($update);
        DB::table('businesses')->where('id', $businessId)->update(['owner_user_id' => $owner->id]);
    }

    public function storeBusiness(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:200'],
            'tagline' => ['nullable', 'string', 'max:255'],
            'phone' => ['nullable', 'string', 'max:64'],
            'email' => ['nullable', 'string', 'max:200'],
            'address' => ['nullable', 'string', 'max:500'],
            'tax_id' => ['nullable', 'string', 'max:100'],
            'invoice_prefix' => ['nullable', 'string', 'max:16'],
            'theme_key' => ['required', 'string', 'max:32'],
            'business_type' => ['nullable', 'string', 'max:32'],
            'currency_symbol' => ['nullable', 'string', 'max:12'],
            'batch_field_label' => ['nullable', 'string', 'max:64'],
            'pack_size_label' => ['nullable', 'string', 'max:64'],
            'product_categories_csv' => ['nullable', 'string', 'max:1000'],
            'expense_categories_csv' => ['nullable', 'string', 'max:1000'],
            'payment_methods_csv' => ['nullable', 'string', 'max:500'],
            'default_tax_rate' => ['nullable', 'numeric', 'min:0', 'max:100'],
            'invoice_footer' => ['nullable', 'string', 'max:500'],
            'owner_name' => ['nullable', 'string', 'max:150'],
            'owner_email' => ['nullable', 'email', 'max:191'],
            'owner_password' => ['nullable', 'string', 'min:6', 'max:100'],
            'owner_password_confirmation' => ['nullable', 'same:owner_password'],
            'owner_margin_mode' => ['nullable', 'in:PERCENT,FLAT'],
            'owner_margin_value' => ['nullable', 'numeric', 'min:0'],
            'distributor_step' => ['nullable', 'numeric', 'min:0'],
            'wholesaler_step' => ['nullable', 'numeric', 'min:0'],
            'retail_step' => ['nullable', 'numeric', 'min:0'],
            'mrp_step' => ['nullable', 'numeric', 'min:0'],
        ]);

        $presets = config('sync.business_presets', []);
        $bType = strtoupper((string) ($data['business_type'] ?? 'GENERAL'));
        $preset = $presets[$bType] ?? $presets['GENERAL'];

        $now = $this->rbac->nowMs();
        $businessId = (string) Str::uuid();
        $insertData = [
            'id' => $businessId,
            'name' => trim($data['name']),
            'tagline' => (string) ($data['tagline'] ?: ($preset['tagline'] ?? '')),
            'phone' => (string) ($data['phone'] ?? ''),
            'email' => (string) ($data['email'] ?? ''),
            'address' => (string) ($data['address'] ?? ''),
            'tax_id' => (string) ($data['tax_id'] ?? ''),
            'invoice_prefix' => strtoupper(trim((string) ($data['invoice_prefix'] ?? 'BM'))) ?: 'BM',
            'theme_key' => strtoupper($data['theme_key']),
            'business_type' => $bType,
            'currency_symbol' => trim((string) ($data['currency_symbol'] ?? 'Rs.')) ?: 'Rs.',
            'track_expiry' => $request->boolean('track_expiry'),
            'track_batch' => $request->boolean('track_batch'),
            'batch_field_label' => trim((string) ($data['batch_field_label'] ?: ($preset['batch_field_label'] ?? 'Batch No.'))),
            'pack_size_label' => trim((string) ($data['pack_size_label'] ?: ($preset['pack_size_label'] ?? 'Pack / Size'))),
            'product_categories_json' => $this->parseCommaList($data['product_categories_csv'] ?? '', $preset['product_categories'] ?? ['General', 'Other']),
            'expense_categories_json' => $this->parseCommaList($data['expense_categories_csv'] ?? '', $preset['expense_categories'] ?? ['Stock Purchase', 'Rent', 'Utilities', 'Other']),
            'payment_methods_json' => $this->parseCommaList($data['payment_methods_csv'] ?? '', $preset['payment_methods'] ?? ['Cash', 'Bank Transfer']),
            'pricing_tier_labels_json' => json_encode($preset['pricing_tier_labels'] ?? ['MRP' => 'MRP', 'DISTRIBUTOR' => 'Distributor', 'WHOLESALER' => 'Wholesaler', 'RETAIL' => 'Retail']),
            'default_tax_rate' => (float) ($data['default_tax_rate'] ?? 0),
            'invoice_footer' => (string) ($data['invoice_footer'] ?? 'Thank you for your business!'),
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
            'is_deleted' => false,
        ];
        if (\Illuminate\Support\Facades\Schema::hasColumn('businesses', 'pricing_rules_json')) {
            $insertData['pricing_rules_json'] = $this->buildPricingRulesJson($data);
        }
        DB::table('businesses')->insert($insertData);

        try {
            $this->syncBusinessOwner($businessId, trim($data['name']), $data);
        } catch (\RuntimeException $e) {
            DB::table('businesses')->where('id', $businessId)->delete();
            return back()->withInput()->with('error', $e->getMessage());
        }

        return redirect()->route('admin.businesses')->with('success', 'Business created with custom theme, pricing margin & categories.');
    }

    private function buildPricingRulesJson(array $data): string
    {
        return json_encode([
            'ownerMarginMode' => strtoupper((string) ($data['owner_margin_mode'] ?? 'PERCENT')) === 'FLAT' ? 'FLAT' : 'PERCENT',
            'ownerMarginValue' => max(0.0, (float) ($data['owner_margin_value'] ?? 5.0)),
            'distributorStep' => max(0.0, (float) ($data['distributor_step'] ?? 0.0)),
            'wholesalerStep' => max(0.0, (float) ($data['wholesaler_step'] ?? 5.0)),
            'retailStep' => max(0.0, (float) ($data['retail_step'] ?? 12.0)),
            'mrpStep' => max(0.0, (float) ($data['mrp_step'] ?? 20.0)),
        ]);
    }

    private function computeSuggestedTiersForBusiness(string $businessId, float $purchasePrice): array
    {
        $cost = max(0.0, $purchasePrice);
        $rules = [
            'ownerMarginMode' => 'PERCENT',
            'ownerMarginValue' => 5.0,
            'distributorStep' => 0.0,
            'wholesalerStep' => 5.0,
            'retailStep' => 12.0,
            'mrpStep' => 20.0,
        ];
        if (\Illuminate\Support\Facades\Schema::hasColumn('businesses', 'pricing_rules_json')) {
            $biz = DB::table('businesses')->where('id', $businessId)->first();
            $decoded = $biz && !empty($biz->pricing_rules_json)
                ? json_decode((string) $biz->pricing_rules_json, true)
                : null;
            if (is_array($decoded)) {
                $rules = array_merge($rules, $decoded);
            }
        }
        $mode = strtoupper((string) ($rules['ownerMarginMode'] ?? 'PERCENT'));
        $marginVal = max(0.0, (float) ($rules['ownerMarginValue'] ?? 5.0));
        $baseFloor = $mode === 'FLAT'
            ? ($cost + $marginVal)
            : ($cost * (1.0 + $marginVal / 100.0));

        return [
            'distributor' => round($baseFloor * (1.0 + max(0.0, (float) ($rules['distributorStep'] ?? 0.0)) / 100.0), 2),
            'wholesaler' => round($baseFloor * (1.0 + max(0.0, (float) ($rules['wholesalerStep'] ?? 5.0)) / 100.0), 2),
            'retail' => round($baseFloor * (1.0 + max(0.0, (float) ($rules['retailStep'] ?? 12.0)) / 100.0), 2),
            'mrp' => round($baseFloor * (1.0 + max(0.0, (float) ($rules['mrpStep'] ?? 20.0)) / 100.0), 2),
        ];
    }

    public function updateBusiness(Request $request, string $id): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:200'],
            'tagline' => ['nullable', 'string', 'max:255'],
            'phone' => ['nullable', 'string', 'max:64'],
            'email' => ['nullable', 'string', 'max:200'],
            'address' => ['nullable', 'string', 'max:500'],
            'tax_id' => ['nullable', 'string', 'max:100'],
            'invoice_prefix' => ['nullable', 'string', 'max:16'],
            'theme_key' => ['required', 'string', 'max:32'],
            'business_type' => ['nullable', 'string', 'max:32'],
            'currency_symbol' => ['nullable', 'string', 'max:12'],
            'batch_field_label' => ['nullable', 'string', 'max:64'],
            'pack_size_label' => ['nullable', 'string', 'max:64'],
            'product_categories_csv' => ['nullable', 'string', 'max:1000'],
            'expense_categories_csv' => ['nullable', 'string', 'max:1000'],
            'payment_methods_csv' => ['nullable', 'string', 'max:500'],
            'default_tax_rate' => ['nullable', 'numeric', 'min:0', 'max:100'],
            'invoice_footer' => ['nullable', 'string', 'max:500'],
            'owner_name' => ['nullable', 'string', 'max:150'],
            'owner_email' => ['nullable', 'email', 'max:191'],
            'owner_password' => ['nullable', 'string', 'min:6', 'max:100'],
            'owner_password_confirmation' => ['nullable', 'same:owner_password'],
            'owner_margin_mode' => ['nullable', 'in:PERCENT,FLAT'],
            'owner_margin_value' => ['nullable', 'numeric', 'min:0'],
            'distributor_step' => ['nullable', 'numeric', 'min:0'],
            'wholesaler_step' => ['nullable', 'numeric', 'min:0'],
            'retail_step' => ['nullable', 'numeric', 'min:0'],
            'mrp_step' => ['nullable', 'numeric', 'min:0'],
        ]);

        $presets = config('sync.business_presets', []);
        $bType = strtoupper((string) ($data['business_type'] ?? 'GENERAL'));
        $preset = $presets[$bType] ?? $presets['GENERAL'];

        $now = $this->rbac->nowMs();
        if (!DB::table('businesses')->where('id', $id)->where('is_deleted', false)->exists()) {
            return back()->with('error', 'Business not found.');
        }

        try {
            $this->syncBusinessOwner($id, trim($data['name']), $data);
        } catch (\RuntimeException $e) {
            return back()->withInput()->with('error', $e->getMessage());
        }

        $updateData = [
            'name' => trim($data['name']),
            'tagline' => (string) ($data['tagline'] ?? ''),
            'phone' => (string) ($data['phone'] ?? ''),
            'email' => (string) ($data['email'] ?? ''),
            'address' => (string) ($data['address'] ?? ''),
            'tax_id' => (string) ($data['tax_id'] ?? ''),
            'invoice_prefix' => strtoupper(trim((string) ($data['invoice_prefix'] ?? 'BM'))) ?: 'BM',
            'theme_key' => strtoupper($data['theme_key']),
            'business_type' => $bType,
            'currency_symbol' => trim((string) ($data['currency_symbol'] ?? 'Rs.')) ?: 'Rs.',
            'track_expiry' => $request->boolean('track_expiry'),
            'track_batch' => $request->boolean('track_batch'),
            'batch_field_label' => trim((string) ($data['batch_field_label'] ?: 'Batch No.')),
            'pack_size_label' => trim((string) ($data['pack_size_label'] ?: 'Pack / Size')),
            'product_categories_json' => $this->parseCommaList($data['product_categories_csv'] ?? '', $preset['product_categories'] ?? ['General', 'Other']),
            'expense_categories_json' => $this->parseCommaList($data['expense_categories_csv'] ?? '', $preset['expense_categories'] ?? ['Stock Purchase', 'Rent', 'Utilities', 'Other']),
            'payment_methods_json' => $this->parseCommaList($data['payment_methods_csv'] ?? '', $preset['payment_methods'] ?? ['Cash', 'Bank Transfer']),
            'default_tax_rate' => (float) ($data['default_tax_rate'] ?? 0),
            'invoice_footer' => (string) ($data['invoice_footer'] ?? ''),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ];
        if (\Illuminate\Support\Facades\Schema::hasColumn('businesses', 'pricing_rules_json')) {
            $updateData['pricing_rules_json'] = $this->buildPricingRulesJson($data);
        }
        DB::table('businesses')->where('id', $id)->update($updateData);

        return redirect()->route('admin.businesses')->with('success', 'Business profile, theme & pricing margins updated and synced.');
    }

    public function verifyBusinessEmail(string $id): RedirectResponse
    {
        $this->smtpOtp->ensureTablesExist();
        $business = DB::table('businesses')->where('id', $id)->where('is_deleted', false)->first();
        if (!$business) {
            return redirect()->route('admin.businesses')->with('error', 'Business not found.');
        }

        $owner = $this->findBusinessOwner($business);
        if (!$owner) {
            return redirect()->route('admin.businesses')->with('error', 'No owner account is linked to this business.');
        }

        $this->smtpOtp->markUserEmailVerified((string) $owner->id, (string) $owner->email);

        return redirect()->route('admin.businesses')->with(
            'success',
            "Email verified manually for {$business->name} ({$owner->email}). The owner can now sign in from the app."
        );
    }

    public function deleteBusiness(string $id): RedirectResponse
    {
        if (DB::table('businesses')->where('is_deleted', false)->count() <= 1) {
            return redirect()->route('admin.businesses')->with('error', 'Cannot delete the only remaining business.');
        }

        $this->smtpOtp->ensureTablesExist();
        $business = DB::table('businesses')->where('id', $id)->first();
        $owner = $business ? $this->findBusinessOwner($business) : null;

        $now = $this->rbac->nowMs();
        DB::table('businesses')->where('id', $id)->update([
            'is_deleted' => true,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        // If removing an unverified business whose owner only belongs to this business,
        // also clean up the unverified user so the email address is freed up.
        if ($owner && !$this->smtpOtp->isUserEmailVerified($owner) && (string) $owner->id !== 'user-super-admin') {
            $bizIds = json_decode((string) ($owner->business_ids_json ?? '[]'), true) ?: [];
            if (count($bizIds) <= 1) {
                DB::table('app_user_tokens')->where('user_id', $owner->id)->delete();
                DB::table('email_otps')->where('email', strtolower((string) $owner->email))->delete();
                DB::table('app_users')->where('id', $owner->id)->delete();
            }
        }

        return redirect()->route('admin.businesses')->with('success', 'Business removed and synced.');
    }

    // ---------- Products / Inventory ----------

    public function products(Request $request): View
    {
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $search = trim((string) $request->query('q', ''));
        $filter = (string) $request->query('filter', 'all');
        $nowMs = $this->rbac->nowMs();
        $thirtyDaysMs = $nowMs + (30 * 86400 * 1000);

        $query = DB::table('products')->where('is_deleted', false);
        if ($selectedBizId !== '') {
            $query->where('business_id', $selectedBizId);
        }
        if ($search !== '') {
            $query->where(function ($q) use ($search) {
                $q->where('name', 'like', "%{$search}%")
                    ->orWhere('brand', 'like', "%{$search}%")
                    ->orWhere('barcode', 'like', "%{$search}%")
                    ->orWhere('batch_number', 'like', "%{$search}%")
                    ->orWhere('performed_by_name', 'like', "%{$search}%");
            });
        }
        if ($filter === 'low') {
            $query->whereColumn('stock_qty', '<=', 'low_stock_threshold');
        } elseif ($filter === 'expired') {
            $query->where('expiry_date_millis', '>', 0)->where('expiry_date_millis', '<', $nowMs);
        } elseif ($filter === 'expiring') {
            $query->where('expiry_date_millis', '>=', $nowMs)->where('expiry_date_millis', '<=', $thirtyDaysMs);
        }

        $products = $query->orderBy('name')->get();

        return view('admin.products', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme',
            'products', 'search', 'filter', 'nowMs', 'thirtyDaysMs'
        ));
    }

    public function storeProduct(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'name' => ['required', 'string', 'max:200'],
            'brand' => ['nullable', 'string', 'max:200'],
            'category' => ['required', 'string', 'max:64'],
            'pack_size' => ['nullable', 'string', 'max:64'],
            'barcode' => ['nullable', 'string', 'max:128'],
            'batch_number' => ['nullable', 'string', 'max:128'],
            'expiry_date' => ['required', 'date'],
            'stock_qty' => ['required', 'integer', 'min:0'],
            'low_stock_threshold' => ['required', 'integer', 'min:0'],
            'purchase_price' => ['nullable', 'numeric', 'min:0'],
            'mrp_price' => ['nullable', 'numeric', 'min:0'],
            'distributor_price' => ['nullable', 'numeric', 'min:0'],
            'wholesaler_price' => ['nullable', 'numeric', 'min:0'],
            'retail_price' => ['nullable', 'numeric', 'min:0'],
        ]);

        $now = $this->rbac->nowMs();
        $expiryMs = Carbon::parse($data['expiry_date'])->endOfDay()->getTimestampMs();
        $purchasePrice = (float) ($data['purchase_price'] ?? 0);
        $suggested = $purchasePrice > 0
            ? $this->computeSuggestedTiersForBusiness($data['business_id'], $purchasePrice)
            : ['distributor' => 0.0, 'wholesaler' => 0.0, 'retail' => 0.0, 'mrp' => 0.0];

        $mrpPrice = (float) ($data['mrp_price'] ?? 0);
        $distPrice = (float) ($data['distributor_price'] ?? 0);
        $whlsPrice = (float) ($data['wholesaler_price'] ?? 0);
        $retailPrice = (float) ($data['retail_price'] ?? 0);

        if ($purchasePrice > 0) {
            if ($mrpPrice <= 0) $mrpPrice = $suggested['mrp'];
            if ($distPrice <= 0) $distPrice = $suggested['distributor'];
            if ($whlsPrice <= 0) $whlsPrice = $suggested['wholesaler'];
            if ($retailPrice <= 0) $retailPrice = $suggested['retail'];
        }

        $stockQty = (int) $data['stock_qty'];
        $productName = trim($data['name']);
        $adminId = $this->currentAdminId($request);
        $adminName = $this->currentAdminName($request);

        DB::table('products')->insert([
            'id' => (string) Str::uuid(),
            'business_id' => $data['business_id'],
            'name' => $productName,
            'brand' => (string) ($data['brand'] ?? ''),
            'category' => $data['category'],
            'pack_size' => (string) ($data['pack_size'] ?? ''),
            'barcode' => (string) ($data['barcode'] ?? ''),
            'batch_number' => (string) ($data['batch_number'] ?? ''),
            'expiry_date_millis' => $expiryMs,
            'stock_qty' => $stockQty,
            'low_stock_threshold' => (int) $data['low_stock_threshold'],
            'purchase_price' => $purchasePrice,
            'mrp_price' => $mrpPrice,
            'distributor_price' => $distPrice,
            'wholesaler_price' => $whlsPrice,
            'retail_price' => $retailPrice,
            'performed_by_id' => $adminId,
            'performed_by_name' => $adminName,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
            'is_deleted' => false,
        ]);

        $totalStockCost = $stockQty * $purchasePrice;
        $loggedExpenseMsg = '';
        if ($request->boolean('record_stock_expense') && $totalStockCost > 0) {
            $payMethod = trim((string) $request->input('purchase_payment_method', 'Cash')) ?: 'Cash';
            DB::table('expenses')->insert([
                'id' => (string) Str::uuid(),
                'business_id' => $data['business_id'],
                'title' => "Stock Purchase: {$productName} ({$stockQty} × " . number_format($purchasePrice, 2) . ')',
                'category' => 'Stock Purchase',
                'amount' => $totalStockCost,
                'payment_method' => $payMethod,
                'payee' => (string) ($data['brand'] ?? ''),
                'notes' => "Auto-logged when adding {$stockQty} units of {$productName}",
                'performed_by_id' => $adminId,
                'performed_by_name' => $adminName,
                'expense_date_millis' => Carbon::now()->startOfDay()->getTimestampMs(),
                'updated_at_ms' => $now,
                'server_modified_at_ms' => $now,
                'is_deleted' => false,
            ]);
            $loggedExpenseMsg = ' & Stock Purchase expense (Rs. ' . number_format($totalStockCost, 0) . ') logged.';
        }

        return redirect()->route('admin.products')->with('success', 'Product added and queued for sync' . $loggedExpenseMsg);
    }

    public function updateProduct(Request $request, string $id): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'name' => ['required', 'string', 'max:200'],
            'brand' => ['nullable', 'string', 'max:200'],
            'category' => ['required', 'string', 'max:64'],
            'pack_size' => ['nullable', 'string', 'max:64'],
            'barcode' => ['nullable', 'string', 'max:128'],
            'batch_number' => ['nullable', 'string', 'max:128'],
            'expiry_date' => ['required', 'date'],
            'stock_qty' => ['required', 'integer', 'min:0'],
            'low_stock_threshold' => ['required', 'integer', 'min:0'],
            'purchase_price' => ['nullable', 'numeric', 'min:0'],
            'mrp_price' => ['nullable', 'numeric', 'min:0'],
            'distributor_price' => ['nullable', 'numeric', 'min:0'],
            'wholesaler_price' => ['nullable', 'numeric', 'min:0'],
            'retail_price' => ['required', 'numeric', 'min:0'],
        ]);

        $now = $this->rbac->nowMs();
        $expiryMs = Carbon::parse($data['expiry_date'])->endOfDay()->getTimestampMs();

        DB::table('products')->where('id', $id)->update([
            'business_id' => $data['business_id'],
            'name' => trim($data['name']),
            'brand' => (string) ($data['brand'] ?? ''),
            'category' => $data['category'],
            'pack_size' => (string) ($data['pack_size'] ?? ''),
            'barcode' => (string) ($data['barcode'] ?? ''),
            'batch_number' => (string) ($data['batch_number'] ?? ''),
            'expiry_date_millis' => $expiryMs,
            'stock_qty' => (int) $data['stock_qty'],
            'low_stock_threshold' => (int) $data['low_stock_threshold'],
            'purchase_price' => (float) ($data['purchase_price'] ?? 0),
            'mrp_price' => (float) ($data['mrp_price'] ?? 0),
            'distributor_price' => (float) ($data['distributor_price'] ?? 0),
            'wholesaler_price' => (float) ($data['wholesaler_price'] ?? 0),
            'retail_price' => (float) $data['retail_price'],
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return redirect()->route('admin.products')->with('success', 'Product updated and synced.');
    }

    public function adjustStock(Request $request, string $id): RedirectResponse
    {
        $request->validate([
            'delta' => ['required', 'integer'],
            'unit_cost' => ['nullable', 'numeric', 'min:0'],
            'purchase_payment_method' => ['nullable', 'string', 'max:64'],
        ]);
        $product = DB::table('products')->where('id', $id)->first();
        if (!$product) {
            return back()->with('error', 'Product not found.');
        }

        $delta = (int) $request->input('delta');
        $newQty = max(0, (int) $product->stock_qty + $delta);
        $now = $this->rbac->nowMs();
        $adminId = $this->currentAdminId($request);
        $adminName = $this->currentAdminName($request);

        $unitCost = $request->filled('unit_cost')
            ? (float) $request->input('unit_cost')
            : (float) ($product->purchase_price ?? 0);

        $updatePayload = [
            'stock_qty' => $newQty,
            'performed_by_id' => $adminId,
            'performed_by_name' => $adminName,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ];
        if ($delta > 0 && $request->filled('unit_cost') && $unitCost > 0) {
            $updatePayload['purchase_price'] = $unitCost;
        }

        DB::table('products')->where('id', $id)->update($updatePayload);

        $extraMsg = '';
        if ($delta > 0 && $request->boolean('record_stock_expense') && $unitCost > 0) {
            $totalCost = $delta * $unitCost;
            $payMethod = trim((string) $request->input('purchase_payment_method', 'Cash')) ?: 'Cash';
            DB::table('expenses')->insert([
                'id' => (string) Str::uuid(),
                'business_id' => (string) $product->business_id,
                'title' => "Stock Restock: {$product->name} (+{$delta} × " . number_format($unitCost, 2) . ')',
                'category' => 'Stock Purchase',
                'amount' => $totalCost,
                'payment_method' => $payMethod,
                'payee' => (string) ($product->brand ?? ''),
                'notes' => "Restocked +{$delta} units via Admin Panel",
                'performed_by_id' => $adminId,
                'performed_by_name' => $adminName,
                'expense_date_millis' => Carbon::now()->startOfDay()->getTimestampMs(),
                'updated_at_ms' => $now,
                'server_modified_at_ms' => $now,
                'is_deleted' => false,
            ]);
            $extraMsg = ' & Stock Purchase spend (Rs. ' . number_format($totalCost, 0) . ') logged.';
        }

        return back()->with('success', "Stock for {$product->name} updated to {$newQty}{$extraMsg}");
    }

    public function deleteProduct(Request $request, string $id): RedirectResponse
    {
        $now = $this->rbac->nowMs();
        DB::table('products')->where('id', $id)->update([
            'is_deleted' => true,
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);
        return redirect()->route('admin.products')->with('success', 'Product removed and synced.');
    }

    // ---------- POS Sales & Invoices ----------

    public function invoices(Request $request): View
    {
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $status = strtoupper((string) $request->query('status', 'ALL'));
        $search = trim((string) $request->query('q', ''));

        $query = DB::table('invoices')->where('is_deleted', false);
        if ($selectedBizId !== '') {
            $query->where('business_id', $selectedBizId);
        }
        if (in_array($status, ['PAID', 'PARTIAL', 'CREDIT'], true)) {
            $query->where('payment_status', $status)->where('is_returned', false);
        } elseif ($status === 'RETURNED') {
            $query->where('is_returned', true);
        }
        if ($search !== '') {
            $query->where(function ($q) use ($search) {
                $q->where('invoice_number', 'like', "%{$search}%")
                    ->orWhere('customer_name', 'like', "%{$search}%")
                    ->orWhere('customer_phone', 'like', "%{$search}%")
                    ->orWhere('performed_by_name', 'like', "%{$search}%");
            });
        }

        $invoices = $query->orderByDesc('created_at_ms')->limit(200)->get();

        return view('admin.invoices', compact('businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'invoices', 'status', 'search'));
    }

    public function showInvoice(Request $request, string $id): View|RedirectResponse
    {
        $invoice = DB::table('invoices')->where('id', $id)->where('is_deleted', false)->first();
        if (!$invoice) {
            return redirect()->route('admin.invoices')->with('error', 'Invoice not found.');
        }

        $businesses = $this->activeBusinesses();
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $business = DB::table('businesses')->where('id', $invoice->business_id)->first();
        $items = DB::table('invoice_items')->where('invoice_id', $id)->get();

        return view('admin.invoice_show', compact('businesses', 'selectedBizId', 'activeTheme', 'invoice', 'business', 'items'));
    }

    public function collectCredit(Request $request, string $id): RedirectResponse
    {
        $request->validate(['amount' => ['required', 'numeric', 'min:0.01']]);
        $invoice = DB::table('invoices')->where('id', $id)->first();
        if (!$invoice || $invoice->is_returned) {
            return back()->with('error', 'Invoice cannot receive payment.');
        }

        $collect = (float) $request->input('amount');
        $newPaid = min((float) $invoice->grand_total, (float) $invoice->amount_paid + $collect);
        $newBalance = max(0.0, (float) $invoice->grand_total - $newPaid);
        $newStatus = $newBalance <= 0.01 ? 'PAID' : 'PARTIAL';
        $now = $this->rbac->nowMs();

        DB::table('invoices')->where('id', $id)->update([
            'amount_paid' => $newPaid,
            'balance_due' => $newBalance,
            'payment_status' => $newStatus,
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return back()->with('success', 'Payment recorded and synced.');
    }

    public function returnInvoice(Request $request, string $id): RedirectResponse
    {
        $invoice = DB::table('invoices')->where('id', $id)->first();
        if (!$invoice || $invoice->is_returned) {
            return back()->with('error', 'Invoice already returned or not found.');
        }

        $now = $this->rbac->nowMs();
        $adminId = $this->currentAdminId($request);
        $adminName = $this->currentAdminName($request);

        DB::transaction(function () use ($id, $now, $adminId, $adminName) {
            $items = DB::table('invoice_items')->where('invoice_id', $id)->get();
            foreach ($items as $item) {
                if (!empty($item->product_id)) {
                    $prod = DB::table('products')->where('id', $item->product_id)->first();
                    if ($prod) {
                        DB::table('products')->where('id', $prod->id)->update([
                            'stock_qty' => (int) $prod->stock_qty + (int) $item->quantity,
                            'performed_by_id' => $adminId,
                            'performed_by_name' => $adminName,
                            'updated_at_ms' => $now,
                            'server_modified_at_ms' => $now,
                        ]);
                    }
                }
            }

            DB::table('invoices')->where('id', $id)->update([
                'is_returned' => true,
                'balance_due' => 0,
                'performed_by_id' => $adminId,
                'performed_by_name' => $adminName,
                'updated_at_ms' => $now,
                'server_modified_at_ms' => $now,
            ]);
        });

        return back()->with('success', 'Sale returned, stock restored, and synced.');
    }

    // ---------- Customers & Credit (Udhaar) Directory ----------

    public function customers(Request $request): View
    {
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $search = trim((string) $request->query('q', ''));

        if (!Schema::hasTable('customers')) {
            $customers = collect();
            $totalOutstanding = 0.0;
            $totalBilled = 0.0;
            $customersWithDue = 0;
            return view('admin.customers', compact(
                'businesses', 'bizMap', 'selectedBizId', 'activeTheme',
                'customers', 'search', 'totalOutstanding', 'totalBilled', 'customersWithDue'
            ));
        }

        $query = DB::table('customers')->where('is_deleted', false);
        if ($selectedBizId !== '') {
            $query->where('business_id', $selectedBizId);
        }
        if ($search !== '') {
            $query->where(function ($q) use ($search) {
                $q->where('name', 'like', "%{$search}%")
                    ->orWhere('phone', 'like', "%{$search}%")
                    ->orWhere('address', 'like', "%{$search}%");
            });
        }

        $rawCustomers = $query->orderBy('name')->get();
        $invQuery = DB::table('invoices')->where('is_deleted', false)->where('is_returned', false);
        if ($selectedBizId !== '') {
            $invQuery->where('business_id', $selectedBizId);
        }
        $invoices = $invQuery->get();

        $customers = $rawCustomers->map(function ($c) use ($invoices) {
            $matched = $invoices->filter(function ($inv) use ($c) {
                if ((string) $inv->business_id !== (string) $c->business_id) {
                    return false;
                }
                if (!empty($inv->customer_id) && $inv->customer_id === $c->id) {
                    return true;
                }
                if (!empty($c->phone) && !empty($inv->customer_phone) && trim($inv->customer_phone) === trim($c->phone)) {
                    return true;
                }
                return strtolower(trim((string) $inv->customer_name)) === strtolower(trim((string) $c->name));
            });
            $c->total_billed = (float) $matched->sum('grand_total');
            $c->invoice_due = (float) $matched->sum('balance_due');
            $c->outstanding_balance = (float) $c->opening_balance + $c->invoice_due;
            return $c;
        });

        $totalOutstanding = (float) $customers->sum('outstanding_balance');
        $totalBilled = (float) $customers->sum('total_billed');
        $customersWithDue = $customers->filter(fn ($c) => $c->outstanding_balance > 0.01)->count();

        return view('admin.customers', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme',
            'customers', 'search', 'totalOutstanding', 'totalBilled', 'customersWithDue'
        ));
    }

    public function storeCustomer(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'name' => ['required', 'string', 'max:200'],
            'phone' => ['nullable', 'string', 'max:64'],
            'email' => ['nullable', 'email', 'max:200'],
            'address' => ['nullable', 'string', 'max:500'],
            'pricing_tier' => ['nullable', 'string', 'max:32'],
            'credit_limit' => ['nullable', 'numeric', 'min:0'],
            'opening_balance' => ['nullable', 'numeric', 'min:0'],
            'notes' => ['nullable', 'string', 'max:500'],
        ]);

        $now = $this->rbac->nowMs();
        DB::table('customers')->insert([
            'id' => (string) Str::uuid(),
            'business_id' => $data['business_id'],
            'name' => trim($data['name']),
            'phone' => trim((string) ($data['phone'] ?? '')),
            'email' => trim((string) ($data['email'] ?? '')),
            'address' => trim((string) ($data['address'] ?? '')),
            'pricing_tier' => strtoupper((string) ($data['pricing_tier'] ?? 'RETAIL')),
            'credit_limit' => (float) ($data['credit_limit'] ?? 0),
            'opening_balance' => (float) ($data['opening_balance'] ?? 0),
            'notes' => trim((string) ($data['notes'] ?? '')),
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
            'is_deleted' => false,
        ]);

        return redirect()->route('admin.customers')->with('success', 'Customer added and synced.');
    }

    public function updateCustomer(Request $request, string $id): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'name' => ['required', 'string', 'max:200'],
            'phone' => ['nullable', 'string', 'max:64'],
            'email' => ['nullable', 'email', 'max:200'],
            'address' => ['nullable', 'string', 'max:500'],
            'pricing_tier' => ['nullable', 'string', 'max:32'],
            'credit_limit' => ['nullable', 'numeric', 'min:0'],
            'opening_balance' => ['nullable', 'numeric', 'min:0'],
            'notes' => ['nullable', 'string', 'max:500'],
        ]);

        $now = $this->rbac->nowMs();
        DB::table('customers')->where('id', $id)->update([
            'business_id' => $data['business_id'],
            'name' => trim($data['name']),
            'phone' => trim((string) ($data['phone'] ?? '')),
            'email' => trim((string) ($data['email'] ?? '')),
            'address' => trim((string) ($data['address'] ?? '')),
            'pricing_tier' => strtoupper((string) ($data['pricing_tier'] ?? 'RETAIL')),
            'credit_limit' => (float) ($data['credit_limit'] ?? 0),
            'opening_balance' => (float) ($data['opening_balance'] ?? 0),
            'notes' => trim((string) ($data['notes'] ?? '')),
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return redirect()->route('admin.customers')->with('success', 'Customer updated and synced.');
    }

    public function deleteCustomer(Request $request, string $id): RedirectResponse
    {
        $now = $this->rbac->nowMs();
        DB::table('customers')->where('id', $id)->update([
            'is_deleted' => true,
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return redirect()->route('admin.customers')->with('success', 'Customer removed and synced.');
    }

    // ---------- Expenses ----------

    public function expenses(Request $request): View
    {
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $month = (string) $request->query('month', '');

        $query = DB::table('expenses')->where('is_deleted', false);
        if ($selectedBizId !== '') {
            $query->where('business_id', $selectedBizId);
        }
        if ($month !== '' && preg_match('/^\d{4}-\d{2}$/', $month)) {
            $startMs = Carbon::createFromFormat('Y-m', $month)->startOfMonth()->getTimestampMs();
            $endMs = Carbon::createFromFormat('Y-m', $month)->endOfMonth()->getTimestampMs();
            $query->whereBetween('expense_date_millis', [$startMs, $endMs]);
        }

        $expenses = $query->orderByDesc('expense_date_millis')->limit(250)->get();
        $totalAmount = (float) $expenses->sum('amount');

        return view('admin.expenses', compact('businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'expenses', 'month', 'totalAmount'));
    }

    public function storeExpense(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'title' => ['required', 'string', 'max:200'],
            'category' => ['required', 'string', 'max:64'],
            'amount' => ['required', 'numeric', 'min:0.01'],
            'payment_method' => ['required', 'string', 'max:64'],
            'payee' => ['nullable', 'string', 'max:200'],
            'notes' => ['nullable', 'string', 'max:500'],
            'expense_date' => ['required', 'date'],
        ]);

        $now = $this->rbac->nowMs();
        $dateMs = Carbon::parse($data['expense_date'])->startOfDay()->getTimestampMs();

        DB::table('expenses')->insert([
            'id' => (string) Str::uuid(),
            'business_id' => $data['business_id'],
            'title' => trim($data['title']),
            'category' => $data['category'],
            'amount' => (float) $data['amount'],
            'payment_method' => $data['payment_method'],
            'payee' => (string) ($data['payee'] ?? ''),
            'notes' => (string) ($data['notes'] ?? ''),
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'expense_date_millis' => $dateMs,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
            'is_deleted' => false,
        ]);

        return redirect()->route('admin.expenses')->with('success', 'Expense recorded and synced.');
    }

    public function updateExpense(Request $request, string $id): RedirectResponse
    {
        $data = $request->validate([
            'business_id' => ['required', 'string'],
            'title' => ['required', 'string', 'max:200'],
            'category' => ['required', 'string', 'max:64'],
            'amount' => ['required', 'numeric', 'min:0.01'],
            'payment_method' => ['required', 'string', 'max:64'],
            'payee' => ['nullable', 'string', 'max:200'],
            'notes' => ['nullable', 'string', 'max:500'],
            'expense_date' => ['required', 'date'],
        ]);

        $now = $this->rbac->nowMs();
        $dateMs = Carbon::parse($data['expense_date'])->startOfDay()->getTimestampMs();

        DB::table('expenses')->where('id', $id)->update([
            'business_id' => $data['business_id'],
            'title' => trim($data['title']),
            'category' => $data['category'],
            'amount' => (float) $data['amount'],
            'payment_method' => $data['payment_method'],
            'payee' => (string) ($data['payee'] ?? ''),
            'notes' => (string) ($data['notes'] ?? ''),
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'expense_date_millis' => $dateMs,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return redirect()->route('admin.expenses')->with('success', 'Expense updated and synced.');
    }

    public function deleteExpense(Request $request, string $id): RedirectResponse
    {
        $now = $this->rbac->nowMs();
        DB::table('expenses')->where('id', $id)->update([
            'is_deleted' => true,
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return redirect()->route('admin.expenses')->with('success', 'Expense deleted and synced.');
    }

    // ---------- Roles & Permissions (RBAC) ----------

    public function roles(Request $request): View
    {
        $this->rbac->ensureSeeded();
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $permissionsCatalog = config('sync.permissions', []);

        $hasBizCol = Schema::hasColumn('roles', 'business_id');
        $rolesQuery = DB::table('roles')->orderByDesc('is_system')->orderBy('name');
        if ($hasBizCol && $selectedBizId !== '') {
            $rolesQuery->where(function ($q) use ($selectedBizId) {
                $q->whereNull('business_id')
                    ->orWhere('business_id', '')
                    ->orWhere('business_id', $selectedBizId);
            });
        }

        $roles = $rolesQuery->get()->map(function ($r) use ($bizMap, $hasBizCol) {
            $r->permissions = json_decode((string) $r->permissions_json, true) ?: [];
            $r->user_count = DB::table('app_users')->where('role_id', $r->id)->count();
            $r->business_id_val = $hasBizCol ? (string) ($r->business_id ?? '') : '';
            $r->business_label = $r->business_id_val !== '' ? ($bizMap[$r->business_id_val]->name ?? 'Custom Business') : 'Global / Shared';
            return $r;
        });

        return view('admin.roles', compact('businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'roles', 'permissionsCatalog'));
    }

    public function storeRole(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100'],
            'description' => ['nullable', 'string', 'max:255'],
            'permissions' => ['nullable', 'array'],
        ]);

        $validKeys = array_keys(config('sync.permissions', []));
        $chosen = array_values(array_intersect($validKeys, $data['permissions'] ?? []));
        if (empty($chosen)) {
            $chosen = ['view_dashboard'];
        }

        if (DB::table('roles')->where('name', trim($data['name']))->exists()) {
            return back()->with('error', 'A role with this name already exists.');
        }

        $now = $this->rbac->nowMs();
        DB::table('roles')->insert([
            'id' => (string) Str::uuid(),
            'name' => trim($data['name']),
            'description' => (string) ($data['description'] ?? ''),
            'permissions_json' => json_encode($chosen),
            'is_system' => false,
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
        ]);

        return redirect()->route('admin.roles')->with('success', 'Role created! Assign it to any staff/admin user.');
    }

    public function updateRole(Request $request, string $id): RedirectResponse
    {
        $role = DB::table('roles')->where('id', $id)->first();
        if (!$role) {
            return back()->with('error', 'Role not found.');
        }
        if ($role->is_system) {
            return back()->with('error', 'Super Admin system role always retains all permissions.');
        }

        $data = $request->validate([
            'name' => ['required', 'string', 'max:100'],
            'description' => ['nullable', 'string', 'max:255'],
            'permissions' => ['nullable', 'array'],
        ]);

        $validKeys = array_keys(config('sync.permissions', []));
        $chosen = array_values(array_intersect($validKeys, $data['permissions'] ?? []));
        if (empty($chosen)) {
            $chosen = ['view_dashboard'];
        }

        DB::table('roles')->where('id', $id)->update([
            'name' => trim($data['name']),
            'description' => (string) ($data['description'] ?? ''),
            'permissions_json' => json_encode($chosen),
            'updated_at_ms' => $this->rbac->nowMs(),
        ]);

        return redirect()->route('admin.roles')->with('success', 'Role permissions updated. Phones will apply them on next sync.');
    }

    public function deleteRole(string $id): RedirectResponse
    {
        $role = DB::table('roles')->where('id', $id)->first();
        if (!$role || $role->is_system) {
            return back()->with('error', 'Cannot delete system role.');
        }
        if (DB::table('app_users')->where('role_id', $id)->exists()) {
            return back()->with('error', 'Reassign users away from this role before deleting it.');
        }

        DB::table('roles')->where('id', $id)->delete();
        return redirect()->route('admin.roles')->with('success', 'Role deleted.');
    }

    // ---------- Staff / Multi-Admin Users ----------

    public function users(Request $request): View
    {
        $this->rbac->ensureSeeded();
        $this->smtpOtp->ensureTablesExist();
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);

        $hasBizCol = Schema::hasColumn('roles', 'business_id');
        $rolesQuery = DB::table('roles')->orderByDesc('is_system')->orderBy('name');
        if ($hasBizCol && $selectedBizId !== '') {
            $rolesQuery->where(function ($q) use ($selectedBizId) {
                $q->whereNull('business_id')->orWhere('business_id', '')->orWhere('business_id', $selectedBizId);
            });
        }
        $roles = $rolesQuery->get();
        $allRolesMap = DB::table('roles')->get()->keyBy('id');

        $tokenCounts = DB::table('app_user_tokens')
            ->selectRaw('user_id, COUNT(*) as cnt')
            ->groupBy('user_id')
            ->pluck('cnt', 'user_id');

        // Build map of owner_user_id -> owned business IDs
        $ownerBizMap = [];
        foreach ($businesses as $b) {
            if (!empty($b->owner_user_id)) {
                $ownerBizMap[(string) $b->owner_user_id][] = (string) $b->id;
            }
        }

        $allUsers = DB::table('app_users')->orderBy('created_at_ms')->get()->map(function ($u) use ($allRolesMap, $tokenCounts, $ownerBizMap, $bizMap) {
            $role = $allRolesMap[$u->role_id] ?? null;
            $rolePerms = $role ? (json_decode((string) $role->permissions_json, true) ?: []) : [];
            $u->role_name = $role->name ?? 'Unknown';
            $u->business_ids = array_values(array_unique(array_map('strval', json_decode((string) $u->business_ids_json, true) ?: ['*'])));
            $u->active_sessions = (int) ($tokenCounts[$u->id] ?? 0);
            $u->owned_business_ids = $ownerBizMap[(string) $u->id] ?? [];
            $u->is_owner = !empty($u->owned_business_ids)
                || $u->role_id === 'role-business-owner'
                || in_array('manage_team', $rolePerms, true);
            $u->is_email_verified = $this->smtpOtp->isUserEmailVerified($u);
            $u->business_names = in_array('*', $u->business_ids, true)
                ? ['All Businesses']
                : array_values(array_map(fn ($id) => $bizMap[$id]->name ?? $id, $u->business_ids));
            return $u;
        });

        // Business-scoped filtering: when a specific business is selected, show ONLY its owners & staff!
        $users = $selectedBizId !== ''
            ? $allUsers->filter(function ($u) use ($selectedBizId) {
                if (in_array($selectedBizId, $u->business_ids, true)) {
                    return true;
                }
                if (in_array($selectedBizId, $u->owned_business_ids, true)) {
                    return true;
                }
                return false;
            })->values()
            : $allUsers->values();

        return view('admin.users', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'roles', 'users'
        ));
    }

    public function verifyUserEmail(string $id): RedirectResponse
    {
        $this->smtpOtp->ensureTablesExist();
        $user = DB::table('app_users')->where('id', $id)->first();
        if (!$user) {
            return redirect()->route('admin.users')->with('error', 'User account not found.');
        }

        $this->smtpOtp->markUserEmailVerified((string) $user->id, (string) $user->email);

        return redirect()->route('admin.users')->with(
            'success',
            "Email verified manually for {$user->name} ({$user->email}). They can now sign in."
        );
    }

    public function storeUser(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:150'],
            'email' => ['required', 'email', 'max:191'],
            'password' => ['required', 'string', 'min:6', 'max:100'],
            'role_id' => ['required', 'string'],
            'all_businesses' => ['nullable', 'boolean'],
            'business_ids' => ['nullable', 'array'],
        ]);

        $email = strtolower(trim($data['email']));
        if (DB::table('app_users')->where('email', $email)->exists()) {
            return back()->with('error', 'An admin/staff account with this email already exists.');
        }

        $bizIds = $request->boolean('all_businesses') || empty($data['business_ids'])
            ? ['*']
            : array_values(array_unique(array_map('strval', $data['business_ids'])));

        $now = $this->rbac->nowMs();
        DB::table('app_users')->insert([
            'id' => (string) Str::uuid(),
            'name' => trim($data['name']),
            'email' => $email,
            'password_hash' => Hash::make($data['password']),
            'role_id' => $data['role_id'],
            'business_ids_json' => json_encode($bizIds),
            'is_active' => true,
            'last_login_ms' => 0,
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
        ]);

        return redirect()->route('admin.users')->with('success', 'Admin/staff account created. They can now sign in on the Android app.');
    }

    public function updateUser(Request $request, string $id): RedirectResponse
    {
        $user = DB::table('app_users')->where('id', $id)->first();
        if (!$user) {
            return back()->with('error', 'User not found.');
        }

        $data = $request->validate([
            'name' => ['required', 'string', 'max:150'],
            'email' => ['required', 'email', 'max:191'],
            'password' => ['nullable', 'string', 'min:6', 'max:100'],
            'role_id' => ['required', 'string'],
            'all_businesses' => ['nullable', 'boolean'],
            'business_ids' => ['nullable', 'array'],
        ]);

        $email = strtolower(trim($data['email']));
        if (DB::table('app_users')->where('email', $email)->where('id', '!=', $id)->exists()) {
            return back()->with('error', 'Another user already uses this email.');
        }

        $bizIds = $request->boolean('all_businesses') || empty($data['business_ids'])
            ? ['*']
            : array_values(array_unique(array_map('strval', $data['business_ids'])));

        $update = [
            'name' => trim($data['name']),
            'email' => $email,
            'role_id' => $data['role_id'],
            'business_ids_json' => json_encode($bizIds),
            'updated_at_ms' => $this->rbac->nowMs(),
        ];
        if (!empty($data['password'])) {
            $update['password_hash'] = Hash::make($data['password']);
        }

        DB::table('app_users')->where('id', $id)->update($update);
        return redirect()->route('admin.users')->with('success', 'User updated.');
    }

    public function toggleUser(string $id): RedirectResponse
    {
        if ($id === 'user-super-admin') {
            return back()->with('error', 'Cannot deactivate the primary Super Admin.');
        }
        $user = DB::table('app_users')->where('id', $id)->first();
        if (!$user) {
            return back()->with('error', 'User not found.');
        }

        $newActive = !$user->is_active;
        DB::table('app_users')->where('id', $id)->update([
            'is_active' => $newActive,
            'updated_at_ms' => $this->rbac->nowMs(),
        ]);
        if (!$newActive) {
            DB::table('app_user_tokens')->where('user_id', $id)->delete();
        }

        return back()->with('success', $newActive ? 'User reactivated.' : 'User deactivated and mobile sessions revoked.');
    }

    public function revokeUserTokens(string $id): RedirectResponse
    {
        DB::table('app_user_tokens')->where('user_id', $id)->delete();
        return back()->with('success', 'All mobile app sessions signed out for this user.');
    }

    public function deleteUser(string $id): RedirectResponse
    {
        if ($id === 'user-super-admin') {
            return back()->with('error', 'Cannot delete the primary Super Admin.');
        }
        DB::table('app_user_tokens')->where('user_id', $id)->delete();
        DB::table('app_users')->where('id', $id)->delete();
        return redirect()->route('admin.users')->with('success', 'User deleted.');
    }

    // ---------- Monthly Reports ----------

    public function reports(Request $request): View
    {
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $month = (string) $request->query('month', Carbon::now()->format('Y-m'));
        if (!preg_match('/^\d{4}-\d{2}$/', $month)) {
            $month = Carbon::now()->format('Y-m');
        }

        $startMs = Carbon::createFromFormat('Y-m', $month)->startOfMonth()->getTimestampMs();
        $endMs = Carbon::createFromFormat('Y-m', $month)->endOfMonth()->getTimestampMs();

        $pl = $this->pl->summarize($selectedBizId, $startMs, $endMs);
        $invoices = $pl['invoices'];
        $expenses = $pl['expenses'];

        $salesTotal = $pl['totalSales'];
        $taxTotal = $pl['taxTotal'];
        $discountTotal = $pl['discountTotal'];
        $cashCollected = $pl['totalCollected'];
        $creditOutstanding = $pl['totalCreditDue'];
        $expensesTotal = $pl['operatingExpenses'];
        $netProfit = $pl['netProfit'];

        $expensesByCategory = $pl['expensesByCategory'];
        $salesByPayment = $invoices->groupBy('payment_method')->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();
        $salesByTier = $invoices->groupBy('pricing_tier')->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();
        $salesByAdmin = $invoices->groupBy(fn ($i) => $i->performed_by_name ?: 'Unassigned')
            ->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();

        return view('admin.reports', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'month', 'pl',
            'invoices', 'expenses', 'salesTotal', 'taxTotal', 'discountTotal',
            'cashCollected', 'creditOutstanding', 'expensesTotal', 'netProfit',
            'expensesByCategory', 'salesByPayment', 'salesByTier', 'salesByAdmin'
        ));
    }

    // ---------- Settings / Sync Status ----------

    public function updateAdminAccount(Request $request): RedirectResponse
    {
        $adminId = $this->currentAdminId($request);
        $user = DB::table('app_users')->where('id', $adminId)->first();
        if (!$user) {
            return back()->with('error', 'Admin account was not found. Please sign out and sign in again.');
        }

        $data = $request->validate([
            'email' => ['required', 'email', 'max:191'],
            'password' => ['nullable', 'string', 'min:6', 'max:100', 'confirmed'],
        ]);
        $email = strtolower(trim($data['email']));
        if (DB::table('app_users')->where('email', $email)->where('id', '!=', $adminId)->exists()) {
            return back()->withInput()->with('error', 'Another account already uses this email.');
        }

        $update = ['email' => $email, 'updated_at_ms' => $this->rbac->nowMs()];
        if (!empty($data['password'])) {
            $update['password_hash'] = Hash::make($data['password']);
            DB::table('app_user_tokens')->where('user_id', $adminId)->delete();
        }
        DB::table('app_users')->where('id', $adminId)->update($update);
        $request->session()->put('bm_admin_email', $email);

        return redirect()->route('admin.settings')->with('success', 'Admin email/password updated successfully.');
    }

    public function settings(Request $request): View
    {
        $this->rbac->ensureSeeded();
        $businesses = $this->activeBusinesses();
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);

        $stats = [
            'businesses' => DB::table('businesses')->where('is_deleted', false)->count(),
            'products' => DB::table('products')->where('is_deleted', false)->count(),
            'invoices' => DB::table('invoices')->where('is_deleted', false)->count(),
            'expenses' => DB::table('expenses')->where('is_deleted', false)->count(),
            'users' => DB::table('app_users')->where('is_active', true)->count(),
            'roles' => DB::table('roles')->count(),
        ];

        $lastSyncMs = max(
            (int) DB::table('businesses')->max('server_modified_at_ms'),
            (int) DB::table('products')->max('server_modified_at_ms'),
            (int) DB::table('invoices')->max('server_modified_at_ms'),
            (int) DB::table('expenses')->max('server_modified_at_ms'),
        );

        $apiBaseUrl = rtrim(url('/'), '/') . '/';
        $apiToken = (string) config('sync.api_token', '');
        $currentAdmin = DB::table('app_users')->where('id', $this->currentAdminId($request))->first();
        $adminEmail = (string) ($currentAdmin->email ?? $request->session()->get('bm_admin_email', config('sync.admin_email', '')));
        $adminName = (string) ($currentAdmin->name ?? $this->currentAdminName($request));
        $smtpSettings = $this->smtpOtp->getSettings();
        $enforceAppAttestation = $this->appAuth->isEnforcementEnabled();
        $authorizedApps = $this->appAuth->listApps();

        return view('admin.settings', compact(
            'businesses', 'selectedBizId', 'activeTheme', 'stats', 'lastSyncMs',
            'apiBaseUrl', 'apiToken', 'adminEmail', 'adminName', 'smtpSettings',
            'enforceAppAttestation', 'authorizedApps'
        ));
    }

    public function updateAppSecurityToggle(Request $request): RedirectResponse
    {
        $enabled = $request->boolean('enforce_app_attestation');
        if ($enabled) {
            $activeCount = $this->appAuth->listApps()->where('is_active', true)->count();
            if ($activeCount === 0) {
                return back()->with('error', 'Add and activate at least one Android package before enabling Strict App Verification.');
            }
        }

        $this->appAuth->setEnforcementEnabled($enabled);
        $statusText = $enabled
            ? 'Strict App Verification is now ACTIVE. Only signed APKs with a registered package name and secret key can connect.'
            : 'Strict App Verification is now OFF (Setup Mode).';

        return redirect()->route('admin.settings')->with('success', $statusText);
    }

    public function storeAuthorizedApp(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'app_name' => ['required', 'string', 'max:120'],
            'package_name' => ['required', 'string', 'max:191', 'regex:/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/'],
            'cert_sha256' => ['nullable', 'string', 'max:128'],
        ], [
            'package_name.regex' => 'Enter a valid Android package name such as com.company.appname.',
        ]);

        try {
            $app = $this->appAuth->createApp(
                $data['app_name'],
                $data['package_name'],
                $data['cert_sha256'] ?? null
            );
            return redirect()->route('admin.settings')->with(
                'success',
                "Package {$app->package_name} registered with a fresh 64-character App Secret Key. Copy it into your Android build secrets before compiling."
            );
        } catch (\RuntimeException $e) {
            return back()->withInput()->with('error', $e->getMessage());
        }
    }

    public function rotateAuthorizedAppKey(string $id): RedirectResponse
    {
        $app = $this->appAuth->rotateKey($id);
        if (!$app) {
            return back()->with('error', 'Authorized app not found.');
        }

        return redirect()->route('admin.settings')->with(
            'success',
            "Generated a new App Secret Key for {$app->package_name}. Rebuild your APK with the new APP_CLIENT_SECRET."
        );
    }

    public function toggleAuthorizedApp(string $id): RedirectResponse
    {
        $app = $this->appAuth->toggleApp($id);
        if (!$app) {
            return back()->with('error', 'Authorized app not found.');
        }

        $label = $app->is_active ? 'enabled' : 'disabled';
        return redirect()->route('admin.settings')->with('success', "Package {$app->package_name} is now {$label}.");
    }

    public function updateAuthorizedAppCert(Request $request, string $id): RedirectResponse
    {
        $data = $request->validate([
            'cert_sha256' => ['nullable', 'string', 'max:128'],
        ]);

        try {
            $app = $this->appAuth->updateCertLock($id, $data['cert_sha256'] ?? null);
            if (!$app) {
                return back()->with('error', 'Authorized app not found.');
            }
            $msg = !empty($app->cert_sha256)
                ? "Signing certificate SHA-256 locked for {$app->package_name}. Repackaged clones with a different keystore will be blocked."
                : "Signing certificate lock cleared for {$app->package_name}.";
            return redirect()->route('admin.settings')->with('success', $msg);
        } catch (\RuntimeException $e) {
            return back()->with('error', $e->getMessage());
        }
    }

    public function deleteAuthorizedApp(string $id): RedirectResponse
    {
        if (!$this->appAuth->deleteApp($id)) {
            return back()->with('error', 'Authorized app not found.');
        }

        return redirect()->route('admin.settings')->with('success', 'Authorized package removed.');
    }

    public function updateSmtpSettings(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'smtp_host' => ['nullable', 'string', 'max:191'],
            'smtp_port' => ['nullable', 'integer', 'min:1', 'max:65535'],
            'smtp_encryption' => ['nullable', 'in:tls,ssl,none'],
            'smtp_username' => ['nullable', 'string', 'max:191'],
            'smtp_password' => ['nullable', 'string', 'max:255'],
            'smtp_from_address' => ['nullable', 'email', 'max:191'],
            'smtp_from_name' => ['nullable', 'string', 'max:120'],
        ]);

        $otpEnabled = $request->boolean('otp_verification_enabled');
        $existing = $this->smtpOtp->getSettings();
        $resolvedHost = trim((string) ($data['smtp_host'] ?? ''));
        $resolvedFrom = trim((string) ($data['smtp_from_address'] ?? '')) ?: trim((string) ($data['smtp_username'] ?? ''));

        if ($otpEnabled && ($resolvedHost === '' || $resolvedFrom === '')) {
            return back()->withInput()->with('error', 'To enable Email OTP Verification, please provide at least the SMTP Host and From Email (or SMTP Username).');
        }

        $data['otp_verification_enabled'] = $otpEnabled;
        if (empty($data['smtp_password']) && !empty($existing['smtp_password'])) {
            $data['smtp_password'] = $existing['smtp_password'];
        }

        $this->smtpOtp->saveSettings($data);

        $statusLabel = $otpEnabled ? 'enabled' : 'disabled';
        return redirect()->route('admin.settings')->with('success', "SMTP settings saved. Registration Email OTP verification is now {$statusLabel}.");
    }

    public function sendTestSmtpEmail(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'test_email' => ['required', 'email', 'max:191'],
        ]);

        try {
            $this->smtpOtp->sendTestEmail(strtolower(trim($data['test_email'])));
            return redirect()->route('admin.settings')->with('success', "Test OTP email sent to {$data['test_email']}.");
        } catch (\Throwable $e) {
            return redirect()->route('admin.settings')->with('error', 'SMTP test failed: ' . $e->getMessage());
        }
    }

    // ---------- CSV Exports ----------

    public function exportProductsCsv(Request $request): StreamedResponse
    {
        $selectedBizId = $this->selectedBusinessId($request);
        $bizMap = $this->activeBusinesses()->pluck('name', 'id');
        $rows = DB::table('products')->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId))
            ->orderBy('name')->get();

        return response()->streamDownload(function () use ($rows, $bizMap) {
            $out = fopen('php://output', 'w');
            fputcsv($out, ['Business', 'Product', 'Brand', 'Category', 'Pack Size', 'Barcode', 'Batch', 'Expiry Date', 'Stock Qty', 'Low Alert', 'Cost', 'MRP', 'Distributor', 'Wholesaler', 'Retail', 'Updated By']);
            foreach ($rows as $p) {
                $exp = $p->expiry_date_millis > 0 ? Carbon::createFromTimestampMs($p->expiry_date_millis)->format('Y-m-d') : '';
                fputcsv($out, [
                    $bizMap[$p->business_id] ?? '',
                    $p->name, $p->brand, $p->category, $p->pack_size, $p->barcode, $p->batch_number,
                    $exp, $p->stock_qty, $p->low_stock_threshold, $p->purchase_price,
                    $p->mrp_price, $p->distributor_price, $p->wholesaler_price, $p->retail_price,
                    $p->performed_by_name ?? '',
                ]);
            }
            fclose($out);
        }, 'business-manager-inventory-' . date('Y-m-d') . '.csv', ['Content-Type' => 'text/csv']);
    }

    public function exportInvoicesCsv(Request $request): StreamedResponse
    {
        $selectedBizId = $this->selectedBusinessId($request);
        $bizMap = $this->activeBusinesses()->pluck('name', 'id');
        $rows = DB::table('invoices')->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId))
            ->orderByDesc('created_at_ms')->get();

        return response()->streamDownload(function () use ($rows, $bizMap) {
            $out = fopen('php://output', 'w');
            fputcsv($out, ['Business', 'Invoice #', 'Date', 'Customer', 'Phone', 'Tier', 'Subtotal', 'Discount', 'Tax', 'Grand Total', 'Paid', 'Balance Due', 'Payment Method', 'Status', 'Returned', 'Performed By']);
            foreach ($rows as $i) {
                $dt = $i->created_at_ms > 0 ? Carbon::createFromTimestampMs($i->created_at_ms)->format('Y-m-d H:i') : '';
                fputcsv($out, [
                    $bizMap[$i->business_id] ?? '',
                    $i->invoice_number, $dt, $i->customer_name, $i->customer_phone, $i->pricing_tier,
                    $i->subtotal, $i->discount_amount, $i->tax_amount, $i->grand_total,
                    $i->amount_paid, $i->balance_due, $i->payment_method, $i->payment_status,
                    $i->is_returned ? 'YES' : 'NO', $i->performed_by_name ?? '',
                ]);
            }
            fclose($out);
        }, 'business-manager-invoices-' . date('Y-m-d') . '.csv', ['Content-Type' => 'text/csv']);
    }

    public function exportExpensesCsv(Request $request): StreamedResponse
    {
        $selectedBizId = $this->selectedBusinessId($request);
        $bizMap = $this->activeBusinesses()->pluck('name', 'id');
        $rows = DB::table('expenses')->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId))
            ->orderByDesc('expense_date_millis')->get();

        return response()->streamDownload(function () use ($rows, $bizMap) {
            $out = fopen('php://output', 'w');
            fputcsv($out, ['Business', 'Date', 'Title', 'Category', 'Amount', 'Payment Method', 'Payee', 'Notes', 'Performed By']);
            foreach ($rows as $e) {
                $dt = $e->expense_date_millis > 0 ? Carbon::createFromTimestampMs($e->expense_date_millis)->format('Y-m-d') : '';
                fputcsv($out, [
                    $bizMap[$e->business_id] ?? '',
                    $dt, $e->title, $e->category, $e->amount, $e->payment_method, $e->payee, $e->notes,
                    $e->performed_by_name ?? '',
                ]);
            }
            fclose($out);
        }, 'business-manager-expenses-' . date('Y-m-d') . '.csv', ['Content-Type' => 'text/csv']);
    }
}
