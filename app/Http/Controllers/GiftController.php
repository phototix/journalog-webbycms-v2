<?php

namespace App\Http\Controllers;

use App\Helpers\PaymentHelper;
use App\Model\Gift;
use App\Model\Post;
use App\Model\PostGift;
use App\Model\Transaction;
use App\Providers\NotificationServiceProvider;
use App\Providers\PaymentsServiceProvider;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Cache;

class GiftController extends Controller
{
    protected PaymentHelper $paymentHandler;

    public function __construct(PaymentHelper $paymentHandler)
    {
        $this->paymentHandler = $paymentHandler;
    }

    public function listGifts(): JsonResponse
    {
        $gifts = Gift::active()->ordered()->get()->groupBy('category');

        return response()->json([
            'gifts' => $gifts,
            'balance' => Auth::check() ? (Auth::user()->wallet->total ?? 0) : 0,
        ]);
    }

    public function sendGift(Request $request): JsonResponse
    {
        $request->validate([
            'gift_id' => 'required|exists:gifts,id',
            'post_id' => 'required|exists:posts,id',
        ]);

        $idempotencyKey = $request->input('idempotency_key');
        if ($idempotencyKey) {
            $cacheKey = 'gift_send_' . $idempotencyKey;
            $cached = Cache::get($cacheKey);
            if ($cached) {
                $cached['_idempotent'] = true;
                return response()->json($cached);
            }
        }

        $user = Auth::user();
        if (!$user) {
            return response()->json(['error' => __('You must be logged in.')], 401);
        }

        $gift = Gift::active()->find($request->gift_id);
        if (!$gift) {
            return response()->json(['error' => __('Gift not found or inactive.')], 404);
        }

        $post = Post::find($request->post_id);
        if (!$post) {
            return response()->json(['error' => __('Post not found.')], 404);
        }

        if ($post->user_id === $user->id) {
            return response()->json(['error' => __('Cannot send a gift to yourself.')], 422);
        }

        $wallet = $user->wallet;
        if (!$wallet || $wallet->total < $gift->credits) {
            return response()->json([
                'error' => __('Insufficient credits. Please top up your wallet.'),
                'balance' => $wallet->total ?? 0,
                'needed' => $gift->credits,
            ], 422);
        }

        $transaction = new Transaction();
        $transaction->sender_user_id = $user->id;
        $transaction->recipient_user_id = $post->user_id;
        $transaction->post_id = $post->id;
        $transaction->type = Transaction::GIFT_TYPE;
        $transaction->status = Transaction::APPROVED_STATUS;
        $transaction->amount = $gift->credits;
        $transaction->currency = config('app.site.currency_code');
        $transaction->payment_provider = Transaction::CREDIT_PROVIDER;
        $transaction->save();

        $this->paymentHandler->deductMoneyFromUserWalletForCreditTransaction($transaction, $wallet);
        $this->paymentHandler->creditReceiverForTransaction($transaction);

        $postGift = new PostGift();
        $postGift->post_id = $post->id;
        $postGift->gift_id = $gift->id;
        $postGift->sender_user_id = $user->id;
        $postGift->recipient_user_id = $post->user_id;
        $postGift->transaction_id = $transaction->id;
        $postGift->save();

        $this->sendGiftNotification($postGift);

        $postGiftsCounts = PostGift::where('post_id', $post->id)
            ->selectRaw('gift_id, count(*) as count')
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        $responseData = [
            'success' => true,
            'message' => __('Gift sent successfully!'),
            'balance' => $wallet->fresh()->total,
            'post_gifts' => $postGiftsCounts,
            'gift' => [
                'name' => $gift->name,
                'icon' => $gift->icon,
                'gif_effect' => $gift->gif_effect ? asset('storage/' . $gift->gif_effect) : null,
            ],
        ];

        if ($idempotencyKey) {
            Cache::put($cacheKey, $responseData, 3600);
        }

        return response()->json($responseData);
    }

    public function postGifts(Request $request): JsonResponse
    {
        $request->validate(['post_id' => 'required|exists:posts,id']);

        $gifts = PostGift::where('post_id', $request->post_id)
            ->selectRaw('gift_id, count(*) as count')
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        return response()->json(['gifts' => $gifts]);
    }

    public function userGiftStats(Request $request, $userId = null): JsonResponse
    {
        $userId = $userId ?? Auth::id();
        if (!$userId) {
            return response()->json(['error' => __('User not found.')], 404);
        }

        $gifts = PostGift::where('recipient_user_id', $userId)
            ->selectRaw('gift_id, count(*) as count, sum(?) as total_credits', [0])
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        $totalCredits = 0;
        foreach ($gifts as $pg) {
            $totalCredits += $pg->count * ($pg->gift->credits ?? 0);
        }

        $totalGifts = $gifts->sum('count');

        return response()->json([
            'gifts' => $gifts,
            'total_gifts' => $totalGifts,
            'total_credits' => $totalCredits,
        ]);
    }

    private function sendGiftNotification(PostGift $postGift): void
    {
        try {
            $transaction = $postGift->transaction;
            if ($transaction) {
                $transaction->refresh();
            }
            NotificationServiceProvider::createNewTipNotification($transaction);
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error('Failed to send gift notification: ' . $e->getMessage());
        }
    }
}
