<?php

namespace App\Http\Controllers\Api;

use App\Model\Post;
use App\Model\User;
use Carbon\Carbon;
use Illuminate\Http\Request;

class ExploreController extends ApiController
{
    public function users(Request $request)
    {
        $page = $request->get('page', 1);
        $perPage = (int) (getSetting('feed.feed_posts_per_page') ?: 12);

        $users = User::where('public_profile', 1)
            ->select('users.*')
            ->selectSub(function ($q) {
                $q->from('posts')
                    ->selectRaw('MAX(created_at)')
                    ->whereColumn('user_id', 'users.id')
                    ->where('status', Post::APPROVED_STATUS);
            }, 'last_posted_at')
            ->orderBy('last_posted_at', 'DESC')
            ->paginate($perPage, ['*'], 'page', $page);

        return $this->success([
            'users' => array_map(function ($u) {
                return [
                    'id' => $u->id,
                    'name' => $u->name,
                    'username' => $u->username,
                    'avatar' => $u->avatar,
                    'bio' => $u->bio,
                    'last_posted_at' => $u->last_posted_at,
                ];
            }, $users->items()),
            'has_more' => $users->hasMorePages(),
            'next_page' => $users->currentPage() + 1,
        ]);
    }
}
