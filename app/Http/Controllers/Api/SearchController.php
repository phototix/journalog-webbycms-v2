<?php

namespace App\Http\Controllers\Api;

use App\Model\User;
use App\Model\Post;
use Illuminate\Http\Request;

class SearchController extends ApiController
{
    public function search(Request $request)
    {
        $query = $request->get('q', '');
        $type = $request->get('type', 'all');

        $results = [];

        if ($type === 'all' || $type === 'users') {
            $users = User::where('name', 'like', "%{$query}%")
                ->orWhere('username', 'like', "%{$query}%")
                ->limit(10)
                ->get()
                ->map(function ($u) {
                    return [
                        'id' => $u->id,
                        'name' => $u->name,
                        'username' => $u->username,
                        'avatar' => $u->avatar,
                        'bio' => $u->bio,
                        'type' => 'user',
                    ];
                });
            $results = array_merge($results, $users->toArray());
        }

        if ($type === 'all' || $type === 'posts') {
            $posts = Post::where('text', 'like', "%{$query}%")
                ->with(['user', 'attachments'])
                ->where(function ($q) {
                    $q->whereNull('price')->orWhere('price', 0);
                })
                ->limit(20)
                ->get()
                ->map(function ($p) {
                    $firstMedia = $p->attachments->first();
                    return [
                        'id' => $p->id,
                        'text' => substr($p->text, 0, 100),
                        'created_at' => $p->created_at,
                        'user' => [
                            'id' => $p->user->id,
                            'name' => $p->user->name,
                            'username' => $p->user->username,
                            'avatar' => $p->user->avatar,
                        ],
                        'thumbnail' => $firstMedia?->thumbnail ?? null,
                        'type' => 'post',
                    ];
                });
            $results = array_merge($results, $posts->toArray());
        }

        return $this->success([
            'results' => $results,
            'query' => $query,
        ]);
    }

    public function trending()
    {
        $creators = User::where('role_id', '!=', 1)
            ->withCount('subscribers')
            ->orderBy('subscribers_count', 'desc')
            ->limit(20)
            ->get()
            ->map(function ($u) {
                return [
                    'id' => $u->id,
                    'name' => $u->name,
                    'username' => $u->username,
                    'avatar' => $u->avatar,
                    'bio' => $u->bio,
                    'subscribers_count' => (int) $u->subscribers_count,
                ];
            });

        return $this->success([
            'trending' => $creators,
        ]);
    }
}
