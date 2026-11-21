<?php

use App\Http\Controllers\AuthController;
use App\Http\Controllers\SyncController;
use App\Http\Controllers\TeamController;
use App\Http\Middleware\VerifyAppClient;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->group(function () {
    // Public health probe for server uptime monitoring
    Route::get('/health', [SyncController::class, 'health']);

    // All mobile app endpoints protected by Package + HMAC Secret + Signing Cert attestation
    Route::middleware(VerifyAppClient::class)->group(function () {
        // Server-backed Auth & Self-Signup for Business Manager Android app
        Route::post('/auth/login', [AuthController::class, 'login']);
        Route::post('/auth/register', [AuthController::class, 'register']);
        Route::get('/auth/me', [AuthController::class, 'me']);
        Route::post('/auth/logout', [AuthController::class, 'logout']);

        // Business Owner In-App Team & Custom Roles Management
        Route::get('/team', [TeamController::class, 'overview']);
        Route::post('/team/roles', [TeamController::class, 'storeRole']);
        Route::put('/team/roles/{id}', [TeamController::class, 'updateRole']);
        Route::delete('/team/roles/{id}', [TeamController::class, 'deleteRole']);
        Route::post('/team/users', [TeamController::class, 'storeUser']);
        Route::put('/team/users/{id}', [TeamController::class, 'updateUser']);
        Route::post('/team/users/{id}/toggle', [TeamController::class, 'toggleUser']);
        Route::delete('/team/users/{id}', [TeamController::class, 'deleteUser']);

        // Bidirectional Sync Endpoints
        Route::post('/sync/push', [SyncController::class, 'push']);
        Route::get('/sync/pull', [SyncController::class, 'pull']);
    });
});
