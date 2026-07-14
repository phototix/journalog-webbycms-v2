<?php

namespace App\Http\Controllers\Api;

use App\Model\Transaction;
use App\Model\Withdrawal;
use App\Providers\PaymentsServiceProvider;
use App\Providers\SettingsServiceProvider;
use App\Providers\WithdrawalsServiceProvider;
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

    public function deposit(Request $request)
    {
        $validated = $request->validate([
            'amount' => 'required|numeric|min:0.01',
            'provider' => 'required|string|in:paypal,stripe,coinbase,credit',
        ]);

        $user = $request->user();
        $amount = (float) $validated['amount'];
        $minAmount = PaymentsServiceProvider::getDepositMinimumAmount();
        $maxAmount = PaymentsServiceProvider::getDepositMaximumAmount();

        if ($amount < $minAmount) {
            return $this->error('Minimum deposit amount: ' . $minAmount, 422);
        }
        if ($amount > $maxAmount) {
            return $this->error('Maximum deposit amount: ' . $maxAmount, 422);
        }

        $transaction = Transaction::create([
            'sender_user_id' => $user->id,
            'recipient_user_id' => $user->id,
            'type' => Transaction::DEPOSIT_TYPE,
            'status' => Transaction::INITIATED_STATUS,
            'amount' => $amount,
            'payment_provider' => $validated['provider'],
            'currency' => config('app.site.currency_code'),
        ]);

        return $this->success([
            'transaction_id' => $transaction->id,
            'amount' => $amount,
            'provider' => $validated['provider'],
            'status' => $transaction->status,
        ], 'Deposit initiated');
    }

    public function withdrawal(Request $request)
    {
        $validated = $request->validate([
            'amount' => 'required|numeric|min:0.01',
            'payment_method' => 'required|string',
            'payment_identifier' => 'required|string|max:500',
            'message' => 'nullable|string|max:2000',
        ]);

        $user = $request->user();

        // Check if user can withdraw
        $settingService = app(SettingsServiceProvider::class);
        if (method_exists($settingService, 'allowWithdrawals') && !$settingService->allowWithdrawals($user)) {
            return $this->error('Withdrawals not allowed. Verify your account first.', 422);
        }

        $wallet = $user->wallet;
        $amount = (float) $validated['amount'];
        $minAmount = PaymentsServiceProvider::getWithdrawalMinimumAmount();
        $maxAmount = PaymentsServiceProvider::getWithdrawalMaximumAmount();

        if ($amount < $minAmount) {
            return $this->error('Minimum withdrawal amount: ' . $minAmount, 422);
        }
        if ($amount > $maxAmount) {
            return $this->error('Maximum withdrawal amount: ' . $maxAmount, 422);
        }

        if (!$wallet || (float) $wallet->total < $amount) {
            return $this->error('Insufficient balance', 422);
        }

        $fee = Withdrawal::calculateFee($amount);

        $withdrawal = Withdrawal::create([
            'user_id' => $user->id,
            'status' => Withdrawal::REQUESTED_STATUS,
            'amount' => $amount,
            'fee' => $fee,
            'payment_method' => $validated['payment_method'],
            'payment_identifier' => $validated['payment_identifier'],
            'message' => $validated['message'] ?? null,
        ]);

        $wallet->decrement('total', $amount);

        return $this->success([
            'withdrawal_id' => $withdrawal->id,
            'amount' => $amount,
            'fee' => $fee,
            'net_amount' => $amount - $fee,
            'new_balance' => (float) $wallet->fresh()->total,
            'pending_balance' => (float) $wallet->fresh()->pendingBalance,
        ], 'Withdrawal requested');
    }

    public function transactions(Request $request)
    {
        $user = $request->user();
        $page = (int) $request->get('page', 1);
        $perPage = (int) $request->get('per_page', 20);

        $transactions = Transaction::where(function ($q) use ($user) {
            $q->where('sender_user_id', $user->id)
              ->orWhere('recipient_user_id', $user->id);
        })->orderBy('created_at', 'desc')->paginate($perPage, ['*'], 'page', $page);

        return $this->success([
            'transactions' => $transactions->through(function ($t) {
                return [
                    'id' => $t->id,
                    'type' => $t->type,
                    'status' => $t->status,
                    'amount' => (float) $t->amount,
                    'currency' => $t->currency,
                    'payment_provider' => $t->payment_provider,
                    'is_incoming' => $t->recipient_user_id === auth()->id(),
                    'created_at' => $t->created_at,
                ];
            }),
            'has_more' => $transactions->hasMorePages(),
            'current_page' => $transactions->currentPage(),
        ]);
    }
}
