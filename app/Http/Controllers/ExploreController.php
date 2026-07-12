<?php

namespace App\Http\Controllers;

use App\Model\Post;
use App\Model\User;
use App\Providers\SuggestionsServiceProvider;
use Illuminate\Support\Facades\DB;
use JavaScript;

class ExploreController extends Controller
{
    public function index()
    {
        $users = $this->getExploreUsers();

        $suggestions = SuggestionsServiceProvider::getSuggestedMembers();

        $nextPageUrl = null;
        if ($users->hasMorePages()) {
            $nextPageUrl = route('explore.users') . '?page=' . ($users->currentPage() + 1);
        }

        JavaScript::put([
            'paginatorConfig' => [
                'next_page_url' => $nextPageUrl,
                'prev_page_url' => null,
                'current_page'  => $users->currentPage(),
                'total'         => $users->total(),
                'per_page'      => $users->perPage(),
                'hasMore'       => $users->hasMorePages(),
            ],
            'initialUserIDs' => $users->pluck('id')->toArray(),
        ]);

        $additionalAssets = ['js' => [], 'css' => []];
        if (getSetting('stories.stories_enabled')) {
            $additionalAssets['js'][] = '/js/stories/stories-player.js';
            $additionalAssets['js'][] = '/js/stories/stories-swiper.js';
            $additionalAssets['css'][] = '/css/stories.css';
        }

        return view('pages.explore', [
            'users' => $users,
            'suggestions' => $suggestions,
            'additionalAssets' => $additionalAssets,
        ]);
    }

    public function getUsers()
    {
        $users = $this->getExploreUsers();

        $usersData = [];
        foreach ($users as $user) {
            $html = view('elements.feed.suggestion-card', [
                'profile' => $user,
                'isListMode' => true,
                'isListManageable' => false,
                'classes' => 'mb-3 w-100',
            ])->render();
            $usersData[] = ['id' => $user->id, 'html' => $html];
        }

        return response()->json([
            'success' => true,
            'data' => [
                'users' => $usersData,
                'next_page_url' => $users->nextPageUrl(),
                'hasMore' => $users->hasMorePages(),
            ],
        ]);
    }

    protected function getExploreUsers()
    {
        $users = User::where('public_profile', 1)
            ->select('users.*')
            ->selectSub(function ($query) {
                $query->from('posts')
                    ->selectRaw('MAX(created_at)')
                    ->whereColumn('user_id', 'users.id')
                    ->where('status', Post::APPROVED_STATUS);
            }, 'last_posted_at')
            ->orderBy('last_posted_at', 'DESC');

        $perPage = getSetting('feed.feed_posts_per_page') ?: 12;

        return $users->paginate($perPage)->appends(request()->query());
    }
}
