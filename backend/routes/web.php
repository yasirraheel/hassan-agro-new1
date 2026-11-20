<?php

use App\Http\Controllers\AdminController;
use App\Http\Middleware\AdminAuth;
use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return response()->json([
        'service' => 'Hassan Agro Sync API',
        'status' => 'running',
        'endpoints' => [
            'GET /api/v1/health',
            'POST /api/v1/sync/push',
            'GET /api/v1/sync/pull?since=0',
        ],
    ]);
});

Route::prefix('admin')->name('admin.')->group(function () {
    Route::get('/login', [AdminController::class, 'showLogin'])->name('login');
    Route::post('/login', [AdminController::class, 'login'])->name('login.submit');

    Route::middleware(AdminAuth::class)->group(function () {
        Route::get('/', [AdminController::class, 'dashboard'])->name('dashboard');
        Route::post('/logout', [AdminController::class, 'logout'])->name('logout');

        Route::get('/businesses', [AdminController::class, 'businesses'])->name('businesses');
        Route::post('/businesses', [AdminController::class, 'storeBusiness'])->name('businesses.store');
        Route::put('/businesses/{id}', [AdminController::class, 'updateBusiness'])->name('businesses.update');
        Route::delete('/businesses/{id}', [AdminController::class, 'deleteBusiness'])->name('businesses.delete');

        Route::get('/products', [AdminController::class, 'products'])->name('products');
        Route::get('/products/export', [AdminController::class, 'exportProductsCsv'])->name('products.export');
        Route::post('/products', [AdminController::class, 'storeProduct'])->name('products.store');
        Route::put('/products/{id}', [AdminController::class, 'updateProduct'])->name('products.update');
        Route::post('/products/{id}/stock', [AdminController::class, 'adjustStock'])->name('products.stock');
        Route::delete('/products/{id}', [AdminController::class, 'deleteProduct'])->name('products.delete');

        Route::get('/invoices', [AdminController::class, 'invoices'])->name('invoices');
        Route::get('/invoices/export', [AdminController::class, 'exportInvoicesCsv'])->name('invoices.export');
        Route::get('/invoices/{id}', [AdminController::class, 'showInvoice'])->name('invoices.show');
        Route::post('/invoices/{id}/collect', [AdminController::class, 'collectCredit'])->name('invoices.collect');
        Route::post('/invoices/{id}/return', [AdminController::class, 'returnInvoice'])->name('invoices.return');

        Route::get('/expenses', [AdminController::class, 'expenses'])->name('expenses');
        Route::get('/expenses/export', [AdminController::class, 'exportExpensesCsv'])->name('expenses.export');
        Route::post('/expenses', [AdminController::class, 'storeExpense'])->name('expenses.store');
        Route::put('/expenses/{id}', [AdminController::class, 'updateExpense'])->name('expenses.update');
        Route::delete('/expenses/{id}', [AdminController::class, 'deleteExpense'])->name('expenses.delete');

        Route::get('/reports', [AdminController::class, 'reports'])->name('reports');
        Route::get('/settings', [AdminController::class, 'settings'])->name('settings');
    });
});
