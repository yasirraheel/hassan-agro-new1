<?php

namespace App\Http\Controllers;

use App\Services\RbacService;
use Carbon\Carbon;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Illuminate\View\View;
use Symfony\Component\HttpFoundation\StreamedResponse;

class AdminController extends Controller
{
    public function __construct(private readonly RbacService $rbac)
    {
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
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $nowMs = $this->rbac->nowMs();
        $thirtyDaysMs = $nowMs + (30 * 86400 * 1000);

        $invQuery = DB::table('invoices')->where('is_deleted', false)->where('is_returned', false);
        $expQuery = DB::table('expenses')->where('is_deleted', false);
        $prodQuery = DB::table('products')->where('is_deleted', false);

        if ($selectedBizId !== '') {
            $invQuery->where('business_id', $selectedBizId);
            $expQuery->where('business_id', $selectedBizId);
            $prodQuery->where('business_id', $selectedBizId);
        }

        $totalRevenue = (float) (clone $invQuery)->sum('grand_total');
        $totalCollected = (float) (clone $invQuery)->sum('amount_paid');
        $totalCreditDue = (float) (clone $invQuery)->sum('balance_due');
        $invoiceCount = (int) (clone $invQuery)->count();
        $totalExpenses = (float) (clone $expQuery)->sum('amount');
        $netProfit = $totalRevenue - $totalExpenses;

        $products = $prodQuery->get();
        $totalProducts = $products->count();
        $totalUnits = (int) $products->sum('stock_qty');
        $lowStockCount = $products->filter(fn ($p) => (int) $p->stock_qty <= (int) $p->low_stock_threshold)->count();
        $expiredCount = $products->filter(fn ($p) => (int) $p->expiry_date_millis > 0 && (int) $p->expiry_date_millis < $nowMs)->count();
        $expiringSoonCount = $products->filter(
            fn ($p) => (int) $p->expiry_date_millis >= $nowMs && (int) $p->expiry_date_millis <= $thirtyDaysMs
        )->count();

        $activeStaffCount = DB::table('app_users')->where('is_active', true)->count();

        $alertProducts = $products->filter(function ($p) use ($nowMs, $thirtyDaysMs) {
            $low = (int) $p->stock_qty <= (int) $p->low_stock_threshold;
            $exp = (int) $p->expiry_date_millis > 0 && (int) $p->expiry_date_millis <= $thirtyDaysMs;
            return $low || $exp;
        })->sortBy('expiry_date_millis')->take(8);

        $recentInvoices = DB::table('invoices')
            ->where('is_deleted', false)
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId))
            ->orderByDesc('created_at_ms')
            ->limit(8)
            ->get();

        return view('admin.dashboard', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme',
            'totalRevenue', 'totalCollected', 'totalCreditDue', 'invoiceCount',
            'totalExpenses', 'netProfit', 'totalProducts', 'totalUnits',
            'lowStockCount', 'expiredCount', 'expiringSoonCount', 'activeStaffCount',
            'alertProducts', 'recentInvoices', 'nowMs', 'thirtyDaysMs'
        ));
    }

    // ---------- Businesses ----------

    public function businesses(Request $request): View
    {
        $businesses = $this->activeBusinesses();
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
            'businesses', 'selectedBizId', 'activeTheme', 'themes', 'presets',
            'productCounts', 'invoiceCounts', 'expenseSums'
        ));
    }

    private function parseCommaList(?string $csv, array $fallback): string
    {
        $items = array_values(array_unique(array_filter(array_map('trim', explode(',', (string) $csv)))));
        return json_encode(!empty($items) ? $items : $fallback);
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
        ]);

        $presets = config('sync.business_presets', []);
        $bType = strtoupper((string) ($data['business_type'] ?? 'GENERAL'));
        $preset = $presets[$bType] ?? $presets['GENERAL'];

        $now = $this->rbac->nowMs();
        DB::table('businesses')->insert([
            'id' => (string) Str::uuid(),
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
        ]);

        return redirect()->route('admin.businesses')->with('success', 'Business created with custom theme & categories and queued for sync.');
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
        ]);

        $presets = config('sync.business_presets', []);
        $bType = strtoupper((string) ($data['business_type'] ?? 'GENERAL'));
        $preset = $presets[$bType] ?? $presets['GENERAL'];

        $now = $this->rbac->nowMs();
        DB::table('businesses')->where('id', $id)->update([
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
        ]);

        return redirect()->route('admin.businesses')->with('success', 'Business profile, theme & customization updated and synced.');
    }

    public function deleteBusiness(string $id): RedirectResponse
    {
        if (DB::table('businesses')->where('is_deleted', false)->count() <= 1) {
            return redirect()->route('admin.businesses')->with('error', 'Cannot delete the only remaining business.');
        }

        $now = $this->rbac->nowMs();
        DB::table('businesses')->where('id', $id)->update([
            'is_deleted' => true,
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

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
            'retail_price' => ['required', 'numeric', 'min:0'],
        ]);

        $now = $this->rbac->nowMs();
        $expiryMs = Carbon::parse($data['expiry_date'])->endOfDay()->getTimestampMs();

        DB::table('products')->insert([
            'id' => (string) Str::uuid(),
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
            'is_deleted' => false,
        ]);

        return redirect()->route('admin.products')->with('success', 'Product added and queued for sync.');
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
        $request->validate(['delta' => ['required', 'integer']]);
        $product = DB::table('products')->where('id', $id)->first();
        if (!$product) {
            return back()->with('error', 'Product not found.');
        }

        $newQty = max(0, (int) $product->stock_qty + (int) $request->input('delta'));
        $now = $this->rbac->nowMs();
        DB::table('products')->where('id', $id)->update([
            'stock_qty' => $newQty,
            'performed_by_id' => $this->currentAdminId($request),
            'performed_by_name' => $this->currentAdminName($request),
            'updated_at_ms' => $now,
            'server_modified_at_ms' => $now,
        ]);

        return back()->with('success', "Stock for {$product->name} updated to {$newQty}.");
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
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);
        $permissionsCatalog = config('sync.permissions', []);

        $roles = DB::table('roles')->orderByDesc('is_system')->orderBy('name')->get()->map(function ($r) {
            $r->permissions = json_decode((string) $r->permissions_json, true) ?: [];
            $r->user_count = DB::table('app_users')->where('role_id', $r->id)->count();
            return $r;
        });

        return view('admin.roles', compact('businesses', 'selectedBizId', 'activeTheme', 'roles', 'permissionsCatalog'));
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

        return redirect()->route('admin.roles')->with('success', 'Role permissions updated — phones will apply them on next sync.');
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
        $businesses = $this->activeBusinesses();
        $bizMap = $businesses->keyBy('id');
        $selectedBizId = $this->selectedBusinessId($request);
        $activeTheme = $this->activeTheme($request, $businesses);

        $roles = DB::table('roles')->orderByDesc('is_system')->orderBy('name')->get();
        $roleMap = $roles->keyBy('id');

        $tokenCounts = DB::table('app_user_tokens')
            ->selectRaw('user_id, COUNT(*) as cnt')
            ->groupBy('user_id')
            ->pluck('cnt', 'user_id');

        $users = DB::table('app_users')->orderBy('created_at_ms')->get()->map(function ($u) use ($roleMap, $tokenCounts) {
            $u->role_name = $roleMap[$u->role_id]->name ?? 'Unknown';
            $u->business_ids = json_decode((string) $u->business_ids_json, true) ?: ['*'];
            $u->active_sessions = (int) ($tokenCounts[$u->id] ?? 0);
            return $u;
        });

        return view('admin.users', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'roles', 'users'
        ));
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

        $invQ = DB::table('invoices')
            ->where('is_deleted', false)
            ->where('is_returned', false)
            ->whereBetween('created_at_ms', [$startMs, $endMs])
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId));

        $expQ = DB::table('expenses')
            ->where('is_deleted', false)
            ->whereBetween('expense_date_millis', [$startMs, $endMs])
            ->when($selectedBizId !== '', fn ($q) => $q->where('business_id', $selectedBizId));

        $invoices = $invQ->get();
        $expenses = $expQ->get();

        $salesTotal = (float) $invoices->sum('grand_total');
        $taxTotal = (float) $invoices->sum('tax_amount');
        $discountTotal = (float) $invoices->sum('discount_amount');
        $cashCollected = (float) $invoices->sum('amount_paid');
        $creditOutstanding = (float) $invoices->sum('balance_due');
        $expensesTotal = (float) $expenses->sum('amount');
        $netProfit = $salesTotal - $expensesTotal;

        $expensesByCategory = $expenses->groupBy('category')->map(fn ($g) => (float) $g->sum('amount'))->sortDesc();
        $salesByPayment = $invoices->groupBy('payment_method')->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();
        $salesByTier = $invoices->groupBy('pricing_tier')->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();
        $salesByAdmin = $invoices->groupBy(fn ($i) => $i->performed_by_name ?: 'Unassigned')
            ->map(fn ($g) => (float) $g->sum('grand_total'))->sortDesc();

        return view('admin.reports', compact(
            'businesses', 'bizMap', 'selectedBizId', 'activeTheme', 'month',
            'invoices', 'expenses', 'salesTotal', 'taxTotal', 'discountTotal',
            'cashCollected', 'creditOutstanding', 'expensesTotal', 'netProfit',
            'expensesByCategory', 'salesByPayment', 'salesByTier', 'salesByAdmin'
        ));
    }

    // ---------- Settings / Sync Status ----------

    public function settings(Request $request): View
    {
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
        $adminEmail = (string) config('sync.admin_email', '');

        return view('admin.settings', compact(
            'businesses', 'selectedBizId', 'activeTheme', 'stats', 'lastSyncMs',
            'apiBaseUrl', 'apiToken', 'adminEmail'
        ));
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
