<?php

namespace App\Http\Controllers\Api;

use App\Helpers\PaymentHelper;
use App\Model\Gift;
use App\Model\Post;
use App\Model\PostGift;
use App\Model\Transaction;
use App\Providers\NotificationServiceProvider;
use App\Model\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class GiftController extends ApiController
{
    protected PaymentHelper $paymentHandler;

    public function __construct(PaymentHelper $paymentHandler)
    {
        $this->paymentHandler = $paymentHandler;
    }

    public function listGifts(Request $request)
    {
        $gifts = Gift::active()->ordered()->get()->groupBy('category');
        $balance = Auth::check() ? (Auth::user()->wallet->total ?? 0) : 0;

        return $this->success([
            'gifts' => $gifts,
            'balance' => (float) $balance,
        ]);
    }

    public function sendGift(Request $request)
    {
        $validated = $request->validate([
            'gift_id' => 'required|exists:gifts,id',
            'post_id' => 'required|exists:posts,id',
        ]);

        $user = $request->user();
        $gift = Gift::active()->findOrFail($validated['gift_id']);
        $post = Post::findOrFail($validated['post_id']);

        if ($post->user_id === $user->id) {
            return $this->error('Cannot send a gift to yourself', 422);
        }

        $wallet = $user->wallet;
        if (!$wallet || $wallet->total < $gift->credits) {
            return $this->error('Insufficient credits', 422);
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

        $postGift = PostGift::create([
            'post_id' => $post->id,
            'gift_id' => $gift->id,
            'sender_user_id' => $user->id,
            'recipient_user_id' => $post->user_id,
            'transaction_id' => $transaction->id,
        ]);

        try {
            NotificationServiceProvider::createNewTipNotification($transaction->fresh());
        } catch (\Exception $e) {
        }

        $postGiftsCounts = PostGift::where('post_id', $post->id)
            ->selectRaw('gift_id, count(*) as count')
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        return $this->success([
            'balance' => (float) $wallet->fresh()->total,
            'post_gifts' => $postGiftsCounts,
            'gift' => [
                'id' => $gift->id,
                'name' => $gift->name,
                'icon' => $gift->icon,
                'credits' => $gift->credits,
                'gif_effect' => $gift->gif_effect ? asset('storage/' . $gift->gif_effect) : null,
            ],
        ], 'Gift sent successfully');
    }

    public function postGifts(Request $request)
    {
        $validated = $request->validate(['post_id' => 'required|exists:posts,id']);

        $gifts = PostGift::where('post_id', $validated['post_id'])
            ->selectRaw('gift_id, count(*) as count')
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        return $this->success([
            'gifts' => $gifts,
        ]);
    }

    public function userGiftStats(Request $request, $username)
    {
        $user = User::where('username', $username)->firstOrFail();

        $postGifts = PostGift::where('recipient_user_id', $user->id)
            ->selectRaw('gift_id, count(*) as count')
            ->groupBy('gift_id')
            ->with('gift')
            ->get();

        $totalCredits = 0;
        foreach ($postGifts as $pg) {
            $totalCredits += $pg->count * ($pg->gift->credits ?? 0);
        }

        return $this->success([
            'gifts' => $postGifts,
            'total_gifts' => $postGifts->sum('count'),
            'total_credits' => $totalCredits,
        ]);
    }
}
