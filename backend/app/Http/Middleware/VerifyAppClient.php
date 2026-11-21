<?php

namespace App\Http\Middleware;

use App\Services\AppClientAuthService;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class VerifyAppClient
{
    public function __construct(
        private readonly AppClientAuthService $appAuth
    ) {
    }

    public function handle(Request $request, Closure $next): Response
    {
        $result = $this->appAuth->verifyRequest($request);
        if (!($result['allowed'] ?? false)) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized application client. Please install the official app.',
            ], 403);
        }

        return $next($request);
    }
}
