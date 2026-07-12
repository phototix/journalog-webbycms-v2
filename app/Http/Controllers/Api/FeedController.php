<?php

namespace App\Http\Controllers\Api;

use App\Model\Post;
use App\Model\UserList;
use App\Providers\PostsHelperServiceProvider;
use Illuminate\Http\Request;

class FeedController extends ApiController
{
    public function index(Request $request)
    {
        $user = $request->user();
        $page = $request->get('page', 1);
        $perPage = (int) getSetting('feed.feed_items_per_page', 10);

        $followedUserIds = UserList::where('user_id', $user->id)
            ->where('type', 'following')
            ->with('members')
            ->first()?->members->pluck('user_id') ?? collect();

        $postIds = $followedUserIds->isNotEmpty()
            ? Post::whereIn('user_id', $followedUserIds)
                ->where(function ($q) {
                    $q->whereNull('price')->orWhere('price', 0);
                })
                ->pluck('id')
            : collect();

        $paginated = Post::withCount(['gifts', 'tips', 'reactions'])
            ->with(['user', 'reactions', 'attachments', 'comments'])
            ->whereIn('id', $postIds)
            ->orWhere('user_id', $user->id)
            ->orderBy('created_at', 'desc')
            ->paginate($perPage, ['*'], 'page', $page);

        return $this->success([
            'posts' => array_map(function ($post) {
                return $this->formatPost($post);
            }, $paginated->items()),
            'has_more' => $paginated->hasMorePages(),
            'next_page' => $paginated->currentPage() + 1,
        ]);
    }

    public function suggestions(Request $request)
    {
        $user = $request->user();
        $suggestions = \App\Model\User::where('id', '!=', $user->id)
            ->where('role_id', '!=', 1)
            ->inRandomOrder()
            ->limit(10)
            ->get();

        return $this->success([
            'suggestions' => $suggestions->map(function ($u) {
                return [
                    'id' => $u->id,
                    'name' => $u->name,
                    'username' => $u->username,
                    'avatar' => $u->avatar,
                    'bio' => $u->bio,
                ];
            }),
        ]);
    }

    private function formatPost($post): array
    {
        $attachments = $post->attachments ?? collect();

        return [
            'id' => $post->id,
            'text' => $post->text,
            'price' => (float) $post->price,
            'status' => $post->status,
            'is_pinned' => (bool) $post->is_pinned,
            'created_at' => $post->created_at,
            'user' => [
                'id' => $post->user->id,
                'name' => $post->user->name,
                'username' => $post->user->username,
                'avatar' => $post->user->avatar,
            ],
            'media' => $attachments->map(function ($att) {
                return [
                    'id' => $att->id,
                    'type' => $att->type,
                    'url' => $att->path,
                    'thumbnail' => $att->thumbnail,
                    'width' => $att->width,
                    'height' => $att->height,
                ];
            }),
            'gifts_count' => (int) ($post->gifts_count ?? 0),
            'likes_count' => $post->reactions->count(),
            'comments_count' => $post->comments->count(),
            'has_liked' => $post->reactions->contains('user_id', auth()->id()),
        ];
    }
}
