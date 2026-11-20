<?php

return [
    // Cached safely via `php artisan config:cache`
    'api_token' => env('SYNC_API_TOKEN', ''),
    'admin_email' => env('ADMIN_EMAIL', ''),
    'admin_password' => env('ADMIN_PASSWORD', ''),
];
