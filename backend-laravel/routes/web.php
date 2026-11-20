<?php

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
