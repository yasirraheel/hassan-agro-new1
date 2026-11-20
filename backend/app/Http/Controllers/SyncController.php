<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class SyncController extends Controller
{
    private function nowMs(): int
    {
        return (int) round(microtime(true) * 1000);
    }

    private function unauthorized(Request $request): ?JsonResponse
    {
        $expected = (string) config('sync.api_token', '');
        if ($expected === '') {
            return null;
        }
        $provided = (string) $request->bearerToken();
        if (!hash_equals($expected, $provided)) {
            return response()->json([
                'status' => 'error',
                'message' => 'Unauthorized: invalid Bearer token',
            ], 401);
        }
        return null;
    }

    public function health(Request $request): JsonResponse
    {
        if ($err = $this->unauthorized($request)) {
            return $err;
        }

        return response()->json([
            'status' => 'ok',
            'serverTime' => $this->nowMs(),
            'message' => 'Hassan Agro Laravel Sync API ready',
        ]);
    }

    public function push(Request $request): JsonResponse
    {
        if ($err = $this->unauthorized($request)) {
            return $err;
        }

        $serverTime = $this->nowMs();

        DB::transaction(function () use ($request, $serverTime) {
            foreach ($request->input('businesses', []) as $b) {
                $existing = DB::table('businesses')->where('id', $b['id'])->first();
                $incomingUpdated = (int) ($b['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                DB::table('businesses')->updateOrInsert(
                    ['id' => (string) $b['id']],
                    [
                        'name' => (string) ($b['name'] ?? ''),
                        'tagline' => (string) ($b['tagline'] ?? ''),
                        'phone' => (string) ($b['phone'] ?? ''),
                        'email' => (string) ($b['email'] ?? ''),
                        'address' => (string) ($b['address'] ?? ''),
                        'tax_id' => (string) ($b['taxId'] ?? ''),
                        'invoice_prefix' => (string) ($b['invoicePrefix'] ?? 'HA'),
                        'default_tax_rate' => (float) ($b['defaultTaxRate'] ?? 0),
                        'invoice_footer' => (string) ($b['invoiceFooter'] ?? ''),
                        'created_at_ms' => (int) ($b['createdAt'] ?? $serverTime),
                        'updated_at_ms' => $incomingUpdated,
                        'server_modified_at_ms' => $serverTime,
                        'is_deleted' => (bool) ($b['isDeleted'] ?? false),
                    ]
                );
            }

            foreach ($request->input('products', []) as $p) {
                $existing = DB::table('products')->where('id', $p['id'])->first();
                $incomingUpdated = (int) ($p['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                DB::table('products')->updateOrInsert(
                    ['id' => (string) $p['id']],
                    [
                        'business_id' => (string) $p['businessId'],
                        'name' => (string) ($p['name'] ?? ''),
                        'brand' => (string) ($p['brand'] ?? ''),
                        'category' => (string) ($p['category'] ?? 'Insecticide'),
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
                        'updated_at_ms' => $incomingUpdated,
                        'server_modified_at_ms' => $serverTime,
                        'is_deleted' => (bool) ($p['isDeleted'] ?? false),
                    ]
                );
            }

            foreach ($request->input('invoices', []) as $inv) {
                $existing = DB::table('invoices')->where('id', $inv['id'])->first();
                $incomingUpdated = (int) ($inv['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                DB::table('invoices')->updateOrInsert(
                    ['id' => (string) $inv['id']],
                    [
                        'business_id' => (string) $inv['businessId'],
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
                        'created_at_ms' => (int) ($inv['createdAt'] ?? $serverTime),
                        'updated_at_ms' => $incomingUpdated,
                        'server_modified_at_ms' => $serverTime,
                        'is_deleted' => (bool) ($inv['isDeleted'] ?? false),
                    ]
                );

                DB::table('invoice_items')->where('invoice_id', $inv['id'])->delete();
                foreach ($inv['items'] ?? [] as $item) {
                    DB::table('invoice_items')->insert([
                        'id' => (string) $item['id'],
                        'invoice_id' => (string) $inv['id'],
                        'product_id' => (string) ($item['productId'] ?? ''),
                        'product_name' => (string) ($item['productName'] ?? ''),
                        'pack_size' => (string) ($item['packSize'] ?? ''),
                        'batch_number' => (string) ($item['batchNumber'] ?? ''),
                        'quantity' => (int) ($item['quantity'] ?? 1),
                        'unit_price' => (float) ($item['unitPrice'] ?? 0),
                        'line_total' => (float) ($item['lineTotal'] ?? 0),
                    ]);
                }
            }

            foreach ($request->input('expenses', []) as $e) {
                $existing = DB::table('expenses')->where('id', $e['id'])->first();
                $incomingUpdated = (int) ($e['updatedAt'] ?? $serverTime);
                if ($existing && (int) $existing->updated_at_ms > $incomingUpdated) {
                    continue;
                }
                DB::table('expenses')->updateOrInsert(
                    ['id' => (string) $e['id']],
                    [
                        'business_id' => (string) $e['businessId'],
                        'title' => (string) ($e['title'] ?? ''),
                        'category' => (string) ($e['category'] ?? 'Other'),
                        'amount' => (float) ($e['amount'] ?? 0),
                        'payment_method' => (string) ($e['paymentMethod'] ?? 'Cash'),
                        'payee' => (string) ($e['payee'] ?? ''),
                        'notes' => (string) ($e['notes'] ?? ''),
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
        if ($err = $this->unauthorized($request)) {
            return $err;
        }

        $since = (int) $request->query('since', 0);
        $serverTime = $this->nowMs();

        $filter = fn ($query) => $since > 0
            ? $query->where('server_modified_at_ms', '>', $since)
            : $query;

        $businesses = $filter(DB::table('businesses'))->get()->map(fn ($b) => [
            'id' => (string) $b->id,
            'name' => (string) $b->name,
            'tagline' => (string) ($b->tagline ?? ''),
            'phone' => (string) ($b->phone ?? ''),
            'email' => (string) ($b->email ?? ''),
            'address' => (string) ($b->address ?? ''),
            'taxId' => (string) ($b->tax_id ?? ''),
            'invoicePrefix' => (string) ($b->invoice_prefix ?? 'HA'),
            'defaultTaxRate' => (float) $b->default_tax_rate,
            'invoiceFooter' => (string) ($b->invoice_footer ?? ''),
            'createdAt' => (int) $b->created_at_ms,
            'updatedAt' => (int) $b->updated_at_ms,
            'isDeleted' => (bool) $b->is_deleted,
        ])->values();

        $products = $filter(DB::table('products'))->get()->map(fn ($p) => [
            'id' => (string) $p->id,
            'businessId' => (string) $p->business_id,
            'name' => (string) $p->name,
            'brand' => (string) ($p->brand ?? ''),
            'category' => (string) ($p->category ?? 'Insecticide'),
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
            'updatedAt' => (int) $p->updated_at_ms,
            'isDeleted' => (bool) $p->is_deleted,
        ])->values();

        $invoiceRows = $filter(DB::table('invoices'))->get();
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
                'lineTotal' => (float) $it->line_total,
            ])->values();

            return [
                'id' => (string) $inv->id,
                'businessId' => (string) $inv->business_id,
                'invoiceNumber' => (string) $inv->invoice_number,
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
                'createdAt' => (int) $inv->created_at_ms,
                'updatedAt' => (int) $inv->updated_at_ms,
                'isDeleted' => (bool) $inv->is_deleted,
                'items' => $items,
            ];
        })->values();

        $expenses = $filter(DB::table('expenses'))->get()->map(fn ($e) => [
            'id' => (string) $e->id,
            'businessId' => (string) $e->business_id,
            'title' => (string) $e->title,
            'category' => (string) ($e->category ?? 'Other'),
            'amount' => (float) $e->amount,
            'paymentMethod' => (string) ($e->payment_method ?? 'Cash'),
            'payee' => (string) ($e->payee ?? ''),
            'notes' => (string) ($e->notes ?? ''),
            'expenseDateMillis' => (int) $e->expense_date_millis,
            'updatedAt' => (int) $e->updated_at_ms,
            'isDeleted' => (bool) $e->is_deleted,
        ])->values();

        return response()->json([
            'serverTime' => $serverTime,
            'businesses' => $businesses,
            'products' => $products,
            'invoices' => $invoices,
            'expenses' => $expenses,
        ]);
    }
}
