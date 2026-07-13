<?php

namespace App\Services;

use App\Model\Notification;
use App\Model\Post;
use App\Model\Story;
use App\Model\User;
use App\Model\UserMessage;
use Illuminate\Support\Str;

class ChatbotContextBuilder
{
    protected User $user;
    protected User $botUser;

    public function __construct(User $user)
    {
        $this->user = $user;
        $this->botUser = User::where('is_bot', true)->firstOrFail();
    }

    public function build(): array
    {
        return [
            'public' => $this->buildPublicContext(),
            'personal' => $this->buildPersonalContext(),
        ];
    }

    protected function buildPublicContext(): array
    {
        return [
            'latest_posts' => $this->getLatestPosts(),
            'latest_stories' => $this->getLatestStories(),
            'top_users' => $this->getTopUsers(),
        ];
    }

    protected function buildPersonalContext(): array
    {
        return [
            'chat_history' => $this->getChatHistory(),
            'notifications' => $this->getNotifications(),
            'user_profile' => $this->getUserProfile(),
        ];
    }

    protected function getLatestPosts(): array
    {
        return Post::with('user')
            ->whereHas('user', fn($q) => $q->notBot())
            ->notExpiredAndReleased()
            ->orderBy('created_at', 'desc')
            ->limit(5)
            ->get()
            ->map(fn($p) => [
                'id' => $p->id,
                'text' => Str::limit(strip_tags($p->text), 200),
                'author' => $p->user->username,
                'created_at' => $p->created_at->toDateTimeString(),
            ])
            ->toArray();
    }

    protected function getLatestStories(): array
    {
        return Story::with('user')
            ->whereHas('user', fn($q) => $q->notBot())
            ->active()
            ->where('is_highlight', false)
            ->orderBy('created_at', 'desc')
            ->limit(5)
            ->get()
            ->map(fn($s) => [
                'id' => $s->id,
                'author' => $s->user->username,
                'created_at' => $s->created_at->toDateTimeString(),
            ])
            ->toArray();
    }

    protected function getTopUsers(): array
    {
        $publicUsers = User::notBot()
            ->where('public_profile', true)
            ->where('paid_profile', false)
            ->withCount('subscribers')
            ->orderBy('subscribers_count', 'desc')
            ->limit(3)
            ->get()
            ->map(fn($u) => $this->formatUserSummary($u));

        $paidUsers = User::notBot()
            ->where('paid_profile', true)
            ->withCount('subscribers')
            ->orderBy('subscribers_count', 'desc')
            ->limit(3)
            ->get()
            ->map(fn($u) => $this->formatUserSummary($u));

        return [
            'public' => $publicUsers,
            'paid' => $paidUsers,
        ];
    }

    protected function formatUserSummary(User $user): array
    {
        return [
            'id' => $user->id,
            'username' => $user->username,
            'name' => $user->name,
            'bio' => Str::limit(strip_tags($user->bio), 150),
            'subscribers_count' => $user->subscribers_count ?? $user->fans_count,
            'profile_type' => $user->paid_profile ? 'paid' : 'public',
        ];
    }

    protected function getChatHistory(): array
    {
        $botId = $this->botUser->id;
        return UserMessage::where(function ($q) use ($botId) {
            $q->where('sender_id', $this->user->id)->where('receiver_id', $botId);
        })->orWhere(function ($q) use ($botId) {
            $q->where('sender_id', $botId)->where('receiver_id', $this->user->id);
        })
        ->orderBy('created_at', 'desc')
        ->limit(5)
        ->get()
        ->reverse()
        ->values()
        ->map(fn($m) => [
            'role' => $m->sender_id === $botId ? 'assistant' : 'user',
            'content' => $m->message,
        ])
        ->toArray();
    }

    protected function getNotifications(): array
    {
        return Notification::where('to_user_id', $this->user->id)
            ->orderBy('created_at', 'desc')
            ->limit(5)
            ->get()
            ->map(fn($n) => [
                'type' => $n->type,
                'message' => $n->message,
                'created_at' => $n->created_at->toDateTimeString(),
            ])
            ->toArray();
    }

    protected function getUserProfile(): array
    {
        return [
            'name' => $this->user->name,
            'username' => $this->user->username,
            'bio' => strip_tags($this->user->bio),
            'location' => $this->user->location,
            'website' => $this->user->website,
            'is_paid_profile' => (bool) $this->user->paid_profile,
            'is_public_profile' => (bool) $this->user->public_profile,
            'follower_count' => $this->user->fans_count,
            'following_count' => $this->user->following_count,
        ];
    }
}
