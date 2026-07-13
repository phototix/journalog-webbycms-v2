<?php

namespace App\Http\Controllers\Api;

use Illuminate\Http\Request;

class WalletController extends ApiController
{
    public function balance()
    {
        $user = auth()->user();
        $wallet = $user->wallet;

        return $this->success([
            'total' => (float) ($wallet->total ?? 0),
            'pendingBalance' => (float) ($wallet->pendingBalance ?? 0),
        ]);
    }
}
