<?php

use App\Http\Controllers\AdminController;
use App\Http\Middleware\AdminAuth;
use Illuminate\Support\Facades\Route;

Route::get('/', fn () => redirect()->route('admin.dashboard'));

Route::prefix('admin')->name('admin.')->group(function () {
    Route::get('/login', [AdminController::class, 'showLogin'])->name('login');
    Route::post('/login', [AdminController::class, 'login'])->name('login.submit');

    Route::middleware(AdminAuth::class)->group(function () {
        Route::post('/logout', [AdminController::class, 'logout'])->name('logout');

        Route::get('/', [AdminController::class, 'dashboard'])->name('dashboard');

        // Businesses (with custom color theme picker)
        Route::get('/businesses', [AdminController::class, 'businesses'])->name('businesses');
        Route::post('/businesses', [AdminController::class, 'storeBusiness'])->name('businesses.store');
        Route::put('/businesses/{id}', [AdminController::class, 'updateBusiness'])->name('businesses.update');
        Route::delete('/businesses/{id}', [AdminController::class, 'deleteBusiness'])->name('businesses.delete');

        // Inventory / Products
        Route::get('/products', [AdminController::class, 'products'])->name('products');
        Route::post('/products', [AdminController::class, 'storeProduct'])->name('products.store');
        Route::put('/products/{id}', [AdminController::class, 'updateProduct'])->name('products.update');
        Route::post('/products/{id}/stock', [AdminController::class, 'adjustStock'])->name('products.stock');
        Route::delete('/products/{id}', [AdminController::class, 'deleteProduct'])->name('products.delete');
        Route::get('/products/export/csv', [AdminController::class, 'exportProductsCsv'])->name('products.export');

        // POS Sales & Invoices
        Route::get('/invoices', [AdminController::class, 'invoices'])->name('invoices');
        Route::get('/invoices/export/csv', [AdminController::class, 'exportInvoicesCsv'])->name('invoices.export');
        Route::get('/invoices/{id}', [AdminController::class, 'showInvoice'])->name('invoices.show');
        Route::post('/invoices/{id}/collect', [AdminController::class, 'collectCredit'])->name('invoices.collect');
        Route::post('/invoices/{id}/return', [AdminController::class, 'returnInvoice'])->name('invoices.return');

        // Expenses
        Route::get('/expenses', [AdminController::class, 'expenses'])->name('expenses');
        Route::post('/expenses', [AdminController::class, 'storeExpense'])->name('expenses.store');
        Route::put('/expenses/{id}', [AdminController::class, 'updateExpense'])->name('expenses.update');
        Route::delete('/expenses/{id}', [AdminController::class, 'deleteExpense'])->name('expenses.delete');
        Route::get('/expenses/export/csv', [AdminController::class, 'exportExpensesCsv'])->name('expenses.export');

        // Roles & Staff / Admin Users (RBAC)
        Route::get('/roles', [AdminController::class, 'roles'])->name('roles');
        Route::post('/roles', [AdminController::class, 'storeRole'])->name('roles.store');
        Route::put('/roles/{id}', [AdminController::class, 'updateRole'])->name('roles.update');
        Route::delete('/roles/{id}', [AdminController::class, 'deleteRole'])->name('roles.delete');

        Route::get('/users', [AdminController::class, 'users'])->name('users');
        Route::post('/users', [AdminController::class, 'storeUser'])->name('users.store');
        Route::put('/users/{id}', [AdminController::class, 'updateUser'])->name('users.update');
        Route::post('/users/{id}/toggle', [AdminController::class, 'toggleUser'])->name('users.toggle');
        Route::post('/users/{id}/revoke-tokens', [AdminController::class, 'revokeUserTokens'])->name('users.revoke');
        Route::delete('/users/{id}', [AdminController::class, 'deleteUser'])->name('users.delete');

        // Reports & App Sync Settings
        Route::get('/reports', [AdminController::class, 'reports'])->name('reports');
        Route::get('/settings', [AdminController::class, 'settings'])->name('settings');
        Route::put('/settings/account', [AdminController::class, 'updateAdminAccount'])->name('settings.account');
    });
});
