<?php

namespace App\Http\Controllers\Api;

use App\Model\Subscription;
use App\Model\User;
use App\Model\Transaction;
use Carbon\Carbon;
use Illuminate\Http\Request;

class SubscriptionController extends ApiController
{
    public function index(Request $request)
    {
        $user = $request->user();
        $page = (int) $request->get('page', 1);
        $perPage = (int) $request->get('per_page', 20);

        $subscriptions = Subscription::where(function ($q) use ($user) {
            $q->where('sender_user_id', $user->id)
              ->orWhere('recipient_user_id', $user->id);
        })->with(['creator', 'subscriber'])->orderBy('created_at', 'desc')
            ->paginate($perPage, ['*'], 'page', $page);

        return $this->success([
            'subscriptions' => collect($subscriptions->items())->map(function ($s) {
                return [
                    'id' => $s->id,
                    'sender' => $s->subscriber ? [
                        'id' => $s->subscriber->id, 'name' => $s->subscriber->name,
                        'username' => $s->subscriber->username, 'avatar' => $s->subscriber->avatar,
                    ] : null,
                    'recipient' => $s->creator ? [
                        'id' => $s->creator->id, 'name' => $s->creator->name,
                        'username' => $s->creator->username, 'avatar' => $s->creator->avatar,
                    ] : null,
                    'status' => $s->status,
                    'amount' => (float) $s->amount,
                    'provider' => $s->provider,
                    'type' => $s->type,
                    'expires_at' => $s->expires_at,
                    'created_at' => $s->created_at,
                ];
            }),
            'has_more' => $subscriptions->hasMorePages(),
        ]);
    }

    public function plans($username)
    {
        $user = User::where('username', $username)->firstOrFail();

        $authUser = auth()->user();
        $hasSub = false;
        if ($authUser) {
            $hasSub = Subscription::where('sender_user_id', $authUser->id)
                ->where('recipient_user_id', $user->id)
                ->whereIn('status', [Subscription::ACTIVE_STATUS, Subscription::CANCELED_STATUS])
                ->where('expires_at', '>', now())
                ->exists();
        }

        return $this->success([
            'price' => (float) ($user->profile_access_price ?? 5),
            'price_3_months' => (float) ($user->profile_access_price_3_months ?? 5),
            'price_6_months' => (float) ($user->profile_access_price_6_months ?? 5),
            'price_12_months' => (float) ($user->profile_access_price_12_months ?? 5),
            'has_subscribed' => $hasSub,
        ]);
    }

    public function subscribe(Request $request)
    {
        $validated = $request->validate([
            'recipient_user_id' => 'required|exists:users,id',
            'plan' => 'required|in:one-month,three-months,six-months,yearly',
        ]);

        $user = auth()->user();
        $creator = User::findOrFail($validated['recipient_user_id']);

        $planDurations = [
            'one-month' => ['months' => 1, 'priceField' => 'profile_access_price'],
            'three-months' => ['months' => 3, 'priceField' => 'profile_access_price_3_months'],
            'six-months' => ['months' => 6, 'priceField' => 'profile_access_price_6_months'],
            'yearly' => ['months' => 12, 'priceField' => 'profile_access_price_12_months'],
        ];

        $plan = $planDurations[$validated['plan']];
        $priceField = $plan['priceField'];
        $unitPrice = (float) ($creator->$priceField ?? 5);
        $totalPrice = $unitPrice * $plan['months'];

        $wallet = $user->wallet;
        if (!$wallet || (float) $wallet->total < $totalPrice) {
            return $this->error('Insufficient wallet balance', 400);
        }

        $expiresAt = Carbon::now()->addMonths($plan['months']);

        $subscription = Subscription::create([
            'sender_user_id' => $user->id,
            'recipient_user_id' => $creator->id,
            'status' => Subscription::ACTIVE_STATUS,
            'amount' => $totalPrice,
            'type' => $validated['plan'],
            'provider' => 'wallet',
            'expires_at' => $expiresAt,
        ]);

        $wallet->decrement('total', $totalPrice);

        $typeMap = [
            'one-month' => Transaction::ONE_MONTH_SUBSCRIPTION,
            'three-months' => Transaction::THREE_MONTHS_SUBSCRIPTION,
            'six-months' => Transaction::SIX_MONTHS_SUBSCRIPTION,
            'yearly' => Transaction::YEARLY_SUBSCRIPTION,
        ];

        Transaction::create([
            'sender_user_id' => $user->id,
            'recipient_user_id' => $creator->id,
            'subscription_id' => $subscription->id,
            'type' => $typeMap[$validated['plan']],
            'status' => Transaction::APPROVED_STATUS,
            'amount' => $totalPrice,
            'payment_method' => 'wallet',
        ]);

        return $this->success([
            'id' => $subscription->id,
            'status' => $subscription->status,
            'expires_at' => $subscription->expires_at,
        ], 'Subscription successful');
    }

    public function cancel(Request $request)
    {
        $validated = $request->validate([
            'creator_user_id' => 'required|exists:users,id',
        ]);

        $user = auth()->user();

        $subscription = Subscription::where('sender_user_id', $user->id)
            ->where('recipient_user_id', $validated['creator_user_id'])
            ->whereIn('status', [Subscription::ACTIVE_STATUS])
            ->first();

        if (!$subscription) {
            return $this->error('No active subscription found', 404);
        }

        $subscription->update([
            'status' => Subscription::CANCELED_STATUS,
            'canceled_at' => now(),
        ]);

        return $this->success(null, 'Subscription canceled');
    }
}
