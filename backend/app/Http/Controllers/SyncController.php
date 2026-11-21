<?php

namespace App\Http\Controllers;

use App\Services\RbacService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class SyncController extends Controller
{
    public function __construct(private readonly RbacService $rbac)
    {
    }

    private function nowMs(): int
    {
        return $this->rbac->nowMs();
    }

    public function health(Request $request): JsonResponse
    {
        $auth = $this->rbac->authenticateApiRequest($request);
        if (!$auth) {
            return response()->json([
                'status' => 'error',
                'message' => 'Unauthorized: please sign in or provide a valid Bearer token',
            ], 401);
        }

        return response()->json([
            'status' => 'ok',
            'serverTime' => $this->rbac->nowMs(),
            'message' => 'Business Manager Laravel Sync API ready',
        ]);
    }

    public function push(Request $request): JsonResponse
    {
        $auth = $this->rbac->authenticateApiRequest($request);
        if (!$auth) {
            return response()->json([
                'accepted' => false,
                'serverTime' => $this->rbac->nowMs(),
                'message' => 'Unauthorized: session expired or invalid token',
            ], 401);
        }

        $user = $auth['user'];
        $serverTime = $this->rbac->nowMs();
        $hasTaxPresetsCol = Schema::hasColumn('businesses', 'tax_presets_json');
        $hasPricingRulesCol = Schema::hasColumn('businesses', 'pricing_rules_json');
        $hasCustomersTable = Schema::hasTable('customers');
        $hasInvoiceCustomerCol = Schema::hasColumn('invoices', 'customer_id');

        DB::transaction(function () use (
            $request, $serverTime, $user, $hasTaxPresetsCol, $hasPricingRulesCol, $hasCustomersTable, $hasInvoiceCustomerCol
        ) {
            foreach ($request->input('businesses', []) as $b) {
                $bizId = (string) ($b['id'] ?? '');
                if ($bizId === '') {
                    continue;
                }
                if ($user && !$this->rbac->hasPermission($user, 'manage_businesses') && !$this->rbac->canAccessBusiness($user, $bizId)) {
                    continue;
                }
                $existing = DB::table('businesses')->where('id', $bizId)->first();
                $incomingUpdated = (int) ($b['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }

                $bizPayload = [
                    'name' => (string) ($b['name'] ?? ''),
                    'tagline' => (string) ($b['tagline'] ?? ''),
                    'phone' => (string) ($b['phone'] ?? ''),
                    'email' => (string) ($b['email'] ?? ''),
                    'address' => (string) ($b['address'] ?? ''),
                    'tax_id' => (string) ($b['taxId'] ?? ''),
                    'invoice_prefix' => (string) ($b['invoicePrefix'] ?? 'BM'),
                    'theme_key' => strtoupper((string) ($b['themeKey'] ?? ($existing->theme_key ?? 'OCEAN'))),
                    'business_type' => strtoupper((string) ($b['businessType'] ?? ($existing->business_type ?? 'GENERAL'))),
                    'currency_symbol' => (string) ($b['currencySymbol'] ?? ($existing->currency_symbol ?? 'Rs.')),
                    'track_expiry' => (bool) ($b['trackExpiry'] ?? ($existing->track_expiry ?? true)),
                    'track_batch' => (bool) ($b['trackBatch'] ?? ($existing->track_batch ?? true)),
                    'batch_field_label' => (string) ($b['batchFieldLabel'] ?? ($existing->batch_field_label ?? 'Batch No.')),
                    'pack_size_label' => (string) ($b['packSizeLabel'] ?? ($existing->pack_size_label ?? 'Pack / Size')),
                    'product_categories_json' => (string) ($b['productCategoriesJson'] ?? ($existing->product_categories_json ?? '')),
                    'expense_categories_json' => (string) ($b['expenseCategoriesJson'] ?? ($existing->expense_categories_json ?? '')),
                    'payment_methods_json' => (string) ($b['paymentMethodsJson'] ?? ($existing->payment_methods_json ?? '')),
                    'pricing_tier_labels_json' => (string) ($b['pricingTierLabelsJson'] ?? ($existing->pricing_tier_labels_json ?? '')),
                    'default_tax_rate' => (float) ($b['defaultTaxRate'] ?? 0),
                    'invoice_footer' => (string) ($b['invoiceFooter'] ?? ''),
                    'created_at_ms' => (int) ($b['createdAt'] ?? $serverTime),
                    'updated_at_ms' => $incomingUpdated,
                    'server_modified_at_ms' => $serverTime,
                    'is_deleted' => (bool) ($b['isDeleted'] ?? false),
                ];
                if ($hasTaxPresetsCol) {
                    $bizPayload['tax_presets_json'] = (string) ($b['taxPresetsJson'] ?? ($existing->tax_presets_json ?? ''));
                }
                if ($hasPricingRulesCol) {
                    $bizPayload['pricing_rules_json'] = (string) ($b['pricingRulesJson'] ?? ($existing->pricing_rules_json ?? ''));
                }

                DB::table('businesses')->updateOrInsert(['id' => $bizId], $bizPayload);
            }

            if ($hasCustomersTable) {
                foreach ($request->input('customers', []) as $c) {
                    $bizId = (string) ($c['businessId'] ?? '');
                    if (!$this->rbac->canAccessBusiness($user, $bizId)) {
                        continue;
                    }
                    $existing = DB::table('customers')->where('id', $c['id'])->first();
                    $incomingUpdated = (int) ($c['updatedAt'] ?? $serverTime);
                    if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                        continue;
                    }
                    $perfId = (string) (($c['performedById'] ?? '') ?: ($user['id'] ?? ''));
                    $perfName = (string) (($c['performedByName'] ?? '') ?: ($user['name'] ?? ''));

                    DB::table('customers')->updateOrInsert(
                        ['id' => (string) $c['id']],
                        [
                            'business_id' => $bizId,
                            'name' => (string) ($c['name'] ?? ''),
                            'phone' => (string) ($c['phone'] ?? ''),
                            'email' => (string) ($c['email'] ?? ''),
                            'address' => (string) ($c['address'] ?? ''),
                            'pricing_tier' => (string) ($c['pricingTier'] ?? 'RETAIL'),
                            'credit_limit' => (float) ($c['creditLimit'] ?? 0),
                            'opening_balance' => (float) ($c['openingBalance'] ?? 0),
                            'notes' => (string) ($c['notes'] ?? ''),
                            'performed_by_id' => $perfId,
                            'performed_by_name' => $perfName,
                            'created_at_ms' => (int) ($c['createdAt'] ?? $serverTime),
                            'updated_at_ms' => $incomingUpdated,
                            'server_modified_at_ms' => $serverTime,
                            'is_deleted' => (bool) ($c['isDeleted'] ?? false),
                        ]
                    );
                }
            }

            foreach ($request->input('products', []) as $p) {
                $bizId = (string) ($p['businessId'] ?? '');
                if (!$this->rbac->canAccessBusiness($user, $bizId)) {
                    continue;
                }
                $existing = DB::table('products')->where('id', $p['id'])->first();
                $incomingUpdated = (int) ($p['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                $perfId = (string) (($p['performedById'] ?? '') ?: ($user['id'] ?? ''));
                $perfName = (string) (($p['performedByName'] ?? '') ?: ($user['name'] ?? ''));

                DB::table('products')->updateOrInsert(
                    ['id' => (string) $p['id']],
                    [
                        'business_id' => $bizId,
                        'name' => (string) ($p['name'] ?? ''),
                        'brand' => (string) ($p['brand'] ?? ''),
                        'category' => (string) ($p['category'] ?? 'General'),
                        'pack_size' => (string) ($p['packSize'] ?? ''),
                        'barcode' => (string) ($p['barcode'] ?? ''),
                        'batch_number' => (string) ($p['batchNumber'] ?? ''),
                        'expiry_date_millis' => (int) ($p['expiryDateMillis'] ?? 0),
                        'stock_qty' => (int) ($p['stockQty'] ?? 0),
                        'low_stock_threshold' => (int) ($p['lowStockThreshold'] ?? 10),
                        'purchase_price' => (float) ($p['purchasePrice'] ?? 0),
                        'mrp_price' => (float) ($p['mrpPrice'] ?? 0),
                        'distributor_price' => (float) ($p['distributorPrice'] ?? 0),
                        'wholesaler_price' => (float) ($p['wholesalerPrice'] ?? 0),
                        'retail_price' => (float) ($p['retailPrice'] ?? 0),
                        'performed_by_id' => $perfId,
                        'performed_by_name' => $perfName,
                        'updated_at_ms' => $incomingUpdated,
                        'server_modified_at_ms' => $serverTime,
                        'is_deleted' => (bool) ($p['isDeleted'] ?? false),
                    ]
                );
            }

            foreach ($request->input('invoices', []) as $inv) {
                $bizId = (string) ($inv['businessId'] ?? '');
                if (!$this->rbac->canAccessBusiness($user, $bizId)) {
                    continue;
                }
                $existing = DB::table('invoices')->where('id', $inv['id'])->first();
                $incomingUpdated = (int) ($inv['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                $perfId = (string) (($inv['performedById'] ?? '') ?: ($user['id'] ?? ''));
                $perfName = (string) (($inv['performedByName'] ?? '') ?: ($user['name'] ?? ''));

                $invPayload = [
                    'business_id' => $bizId,
                    'invoice_number' => (string) ($inv['invoiceNumber'] ?? ''),
                    'customer_name' => (string) ($inv['customerName'] ?? 'Walk-in Customer'),
                    'customer_phone' => (string) ($inv['customerPhone'] ?? ''),
                    'customer_email' => (string) ($inv['customerEmail'] ?? ''),
                    'pricing_tier' => (string) ($inv['pricingTier'] ?? 'RETAIL'),
                    'subtotal' => (float) ($inv['subtotal'] ?? 0),
                    'discount_amount' => (float) ($inv['discountAmount'] ?? 0),
                    'tax_enabled' => (bool) ($inv['taxEnabled'] ?? false),
                    'tax_rate_percent' => (float) ($inv['taxRatePercent'] ?? 0),
                    'tax_amount' => (float) ($inv['taxAmount'] ?? 0),
                    'grand_total' => (float) ($inv['grandTotal'] ?? 0),
                    'payment_method' => (string) ($inv['paymentMethod'] ?? 'Cash'),
                    'payment_status' => (string) ($inv['paymentStatus'] ?? 'PAID'),
                    'amount_paid' => (float) ($inv['amountPaid'] ?? 0),
                    'balance_due' => (float) ($inv['balanceDue'] ?? 0),
                    'is_returned' => (bool) ($inv['isReturned'] ?? false),
                    'notes' => (string) ($inv['notes'] ?? ''),
                    'performed_by_id' => $perfId,
                    'performed_by_name' => $perfName,
                    'created_at_ms' => (int) ($inv['createdAt'] ?? $serverTime),
                    'updated_at_ms' => $incomingUpdated,
                    'server_modified_at_ms' => $serverTime,
                    'is_deleted' => (bool) ($inv['isDeleted'] ?? false),
                ];
                if ($hasInvoiceCustomerCol) {
                    $invPayload['customer_id'] = (string) ($inv['customerId'] ?? '');
                }

                DB::table('invoices')->updateOrInsert(['id' => (string) $inv['id']], $invPayload);

                $hasItemCostCol = Schema::hasColumn('invoice_items', 'cost_price');
                DB::table('invoice_items')->where('invoice_id', $inv['id'])->delete();
                foreach ($inv['items'] ?? [] as $item) {
                    $itemRow = [
                        'id' => (string) $item['id'],
                        'invoice_id' => (string) $inv['id'],
                        'product_id' => (string) ($item['productId'] ?? ''),
                        'product_name' => (string) ($item['productName'] ?? ''),
                        'pack_size' => (string) ($item['packSize'] ?? ''),
                        'batch_number' => (string) ($item['batchNumber'] ?? ''),
                        'quantity' => (int) ($item['quantity'] ?? 1),
                        'unit_price' => (float) ($item['unitPrice'] ?? 0),
                        'line_total' => (float) ($item['lineTotal'] ?? 0),
                    ];
                    if ($hasItemCostCol) {
                        $itemRow['cost_price'] = (float) ($item['costPrice'] ?? 0);
                    }
                    DB::table('invoice_items')->insert($itemRow);
                }
            }

            foreach ($request->input('expenses', []) as $e) {
                $bizId = (string) ($e['businessId'] ?? '');
                if (!$this->rbac->canAccessBusiness($user, $bizId)) {
                    continue;
                }
                $existing = DB::table('expenses')->where('id', $e['id'])->first();
                $incomingUpdated = (int) ($e['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                $perfId = (string) (($e['performedById'] ?? '') ?: ($user['id'] ?? ''));
                $perfName = (string) (($e['performedByName'] ?? '') ?: ($user['name'] ?? ''));

                DB::table('expenses')->updateOrInsert(
                    ['id' => (string) $e['id']],
                    [
                        'business_id' => $bizId,
                        'title' => (string) ($e['title'] ?? ''),
                        'category' => (string) ($e['category'] ?? 'Other'),
                        'amount' => (float) ($e['amount'] ?? 0),
                        'payment_method' => (string) ($e['paymentMethod'] ?? 'Cash'),
                        'payee' => (string) ($e['payee'] ?? ''),
                        'notes' => (string) ($e['notes'] ?? ''),
                        'performed_by_id' => $perfId,
                        'performed_by_name' => $perfName,
                        'expense_date_millis' => (int) ($e['expenseDateMillis'] ?? $serverTime),
                        'updated_at_ms' => $incomingUpdated,
                        'server_modified_at_ms' => $serverTime,
                        'is_deleted' => (bool) ($e['isDeleted'] ?? false),
                    ]
                );
            }
        });

        return response()->json([
            'accepted' => true,
            'serverTime' => $serverTime,
            'message' => 'Synced successfully',
        ]);
    }

    public function pull(Request $request): JsonResponse
    {
        $auth = $this->rbac->authenticateApiRequest($request);
        if (!$auth) {
            return response()->json([
                'status' => 'error',
                'message' => 'Unauthorized: session expired or invalid token',
            ], 401);
        }

        $user = $auth['user'];
        $allowed = $user ? ($user['allowedBusinessIds'] ?? ['*']) : ['*'];
        $restrictBusinesses = !in_array('*', $allowed, true);

        $since = (int) $request->query('since', 0);
        $serverTime = $this->nowMs();

        $timeFilter = fn ($query) => $since > 0
            ? $query->where('server_modified_at_ms', '>', $since)
            : $query;

        $bizQuery = $timeFilter(DB::table('businesses'));
        if ($restrictBusinesses) {
            $bizQuery->whereIn('id', $allowed);
        }
        $businesses = $bizQuery->get()->map(fn ($b) => [
            'id' => (string) $b->id,
            'name' => (string) $b->name,
            'tagline' => (string) ($b->tagline ?? ''),
            'phone' => (string) ($b->phone ?? ''),
            'email' => (string) ($b->email ?? ''),
            'address' => (string) ($b->address ?? ''),
            'taxId' => (string) ($b->tax_id ?? ''),
            'invoicePrefix' => (string) ($b->invoice_prefix ?? 'BM'),
            'themeKey' => (string) ($b->theme_key ?? 'OCEAN'),
            'businessType' => (string) ($b->business_type ?? 'GENERAL'),
            'currencySymbol' => (string) ($b->currency_symbol ?? 'Rs.'),
            'trackExpiry' => (bool) ($b->track_expiry ?? true),
            'trackBatch' => (bool) ($b->track_batch ?? true),
            'batchFieldLabel' => (string) ($b->batch_field_label ?? 'Batch No.'),
            'packSizeLabel' => (string) ($b->pack_size_label ?? 'Pack / Size'),
            'productCategoriesJson' => (string) ($b->product_categories_json ?? ''),
            'expenseCategoriesJson' => (string) ($b->expense_categories_json ?? ''),
            'paymentMethodsJson' => (string) ($b->payment_methods_json ?? ''),
            'pricingTierLabelsJson' => (string) ($b->pricing_tier_labels_json ?? ''),
            'taxPresetsJson' => (string) ($b->tax_presets_json ?? ''),
            'pricingRulesJson' => (string) ($b->pricing_rules_json ?? ''),
            'defaultTaxRate' => (float) $b->default_tax_rate,
            'invoiceFooter' => (string) ($b->invoice_footer ?? ''),
            'createdAt' => (int) $b->created_at_ms,
            'updatedAt' => (int) $b->updated_at_ms,
            'isDeleted' => (bool) $b->is_deleted,
        ])->values();

        $customers = collect();
        if (Schema::hasTable('customers')) {
            $custQuery = $timeFilter(DB::table('customers'));
            if ($restrictBusinesses) {
                $custQuery->whereIn('business_id', $allowed);
            }
            $customers = $custQuery->get()->map(fn ($c) => [
                'id' => (string) $c->id,
                'businessId' => (string) $c->business_id,
                'name' => (string) $c->name,
                'phone' => (string) ($c->phone ?? ''),
                'email' => (string) ($c->email ?? ''),
                'address' => (string) ($c->address ?? ''),
                'pricingTier' => (string) ($c->pricing_tier ?? 'RETAIL'),
                'creditLimit' => (float) $c->credit_limit,
                'openingBalance' => (float) $c->opening_balance,
                'notes' => (string) ($c->notes ?? ''),
                'performedById' => (string) ($c->performed_by_id ?? ''),
                'performedByName' => (string) ($c->performed_by_name ?? ''),
                'createdAt' => (int) $c->created_at_ms,
                'updatedAt' => (int) $c->updated_at_ms,
                'isDeleted' => (bool) $c->is_deleted,
            ])->values();
        }

        $prodQuery = $timeFilter(DB::table('products'));
        if ($restrictBusinesses) {
            $prodQuery->whereIn('business_id', $allowed);
        }
        $products = $prodQuery->get()->map(fn ($p) => [
            'id' => (string) $p->id,
            'businessId' => (string) $p->business_id,
            'name' => (string) $p->name,
            'brand' => (string) ($p->brand ?? ''),
            'category' => (string) ($p->category ?? 'General'),
            'packSize' => (string) ($p->pack_size ?? ''),
            'barcode' => (string) ($p->barcode ?? ''),
            'batchNumber' => (string) ($p->batch_number ?? ''),
            'expiryDateMillis' => (int) $p->expiry_date_millis,
            'stockQty' => (int) $p->stock_qty,
            'lowStockThreshold' => (int) $p->low_stock_threshold,
            'purchasePrice' => (float) $p->purchase_price,
            'mrpPrice' => (float) $p->mrp_price,
            'distributorPrice' => (float) $p->distributor_price,
            'wholesalerPrice' => (float) $p->wholesaler_price,
            'retailPrice' => (float) $p->retail_price,
            'performedById' => (string) ($p->performed_by_id ?? ''),
            'performedByName' => (string) ($p->performed_by_name ?? ''),
            'updatedAt' => (int) $p->updated_at_ms,
            'isDeleted' => (bool) $p->is_deleted,
        ])->values();

        $invQuery = $timeFilter(DB::table('invoices'));
        if ($restrictBusinesses) {
            $invQuery->whereIn('business_id', $allowed);
        }
        $invoiceRows = $invQuery->get();
        $invoiceIds = $invoiceRows->pluck('id')->all();
        $itemsByInvoice = empty($invoiceIds)
            ? collect()
            : DB::table('invoice_items')->whereIn('invoice_id', $invoiceIds)->get()->groupBy('invoice_id');

        $invoices = $invoiceRows->map(function ($inv) use ($itemsByInvoice) {
            $items = ($itemsByInvoice[$inv->id] ?? collect())->map(fn ($it) => [
                'id' => (string) $it->id,
                'invoiceId' => (string) $it->invoice_id,
                'productId' => (string) $it->product_id,
                'productName' => (string) $it->product_name,
                'packSize' => (string) ($it->pack_size ?? ''),
                'batchNumber' => (string) ($it->batch_number ?? ''),
                'quantity' => (int) $it->quantity,
                'unitPrice' => (float) $it->unit_price,
                'costPrice' => (float) ($it->cost_price ?? 0),
                'lineTotal' => (float) $it->line_total,
            ])->values();

            return [
                'id' => (string) $inv->id,
                'businessId' => (string) $inv->business_id,
                'invoiceNumber' => (string) $inv->invoice_number,
                'customerId' => (string) ($inv->customer_id ?? ''),
                'customerName' => (string) $inv->customer_name,
                'customerPhone' => (string) ($inv->customer_phone ?? ''),
                'customerEmail' => (string) ($inv->customer_email ?? ''),
                'pricingTier' => (string) ($inv->pricing_tier ?? 'RETAIL'),
                'subtotal' => (float) $inv->subtotal,
                'discountAmount' => (float) $inv->discount_amount,
                'taxEnabled' => (bool) $inv->tax_enabled,
                'taxRatePercent' => (float) $inv->tax_rate_percent,
                'taxAmount' => (float) $inv->tax_amount,
                'grandTotal' => (float) $inv->grand_total,
                'paymentMethod' => (string) ($inv->payment_method ?? 'Cash'),
                'paymentStatus' => (string) ($inv->payment_status ?? 'PAID'),
                'amountPaid' => (float) $inv->amount_paid,
                'balanceDue' => (float) $inv->balance_due,
                'isReturned' => (bool) $inv->is_returned,
                'notes' => (string) ($inv->notes ?? ''),
                'performedById' => (string) ($inv->performed_by_id ?? ''),
                'performedByName' => (string) ($inv->performed_by_name ?? ''),
                'createdAt' => (int) $inv->created_at_ms,
                'updatedAt' => (int) $inv->updated_at_ms,
                'isDeleted' => (bool) $inv->is_deleted,
                'items' => $items,
            ];
        })->values();

        $expQuery = $timeFilter(DB::table('expenses'));
        if ($restrictBusinesses) {
            $expQuery->whereIn('business_id', $allowed);
        }
        $expenses = $expQuery->get()->map(fn ($e) => [
            'id' => (string) $e->id,
            'businessId' => (string) $e->business_id,
            'title' => (string) $e->title,
            'category' => (string) ($e->category ?? 'Other'),
            'amount' => (float) $e->amount,
            'paymentMethod' => (string) ($e->payment_method ?? 'Cash'),
            'payee' => (string) ($e->payee ?? ''),
            'notes' => (string) ($e->notes ?? ''),
            'performedById' => (string) ($e->performed_by_id ?? ''),
            'performedByName' => (string) ($e->performed_by_name ?? ''),
            'expenseDateMillis' => (int) $e->expense_date_millis,
            'updatedAt' => (int) $e->updated_at_ms,
            'isDeleted' => (bool) $e->is_deleted,
        ])->values();

        return response()->json([
            'serverTime' => $serverTime,
            'user' => $user,
            'businesses' => $businesses,
            'customers' => $customers,
            'products' => $products,
            'invoices' => $invoices,
            'expenses' => $expenses,
        ]);
    }
}
