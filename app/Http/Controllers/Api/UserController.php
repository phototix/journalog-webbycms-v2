<?php

namespace App\Http\Controllers\Api;

use App\Model\PostGift;
use App\Model\User;
use App\Model\UserList;
use App\Model\UserListMember;
use App\Model\Subscription;
use App\Providers\ListsHelperServiceProvider;
use Illuminate\Http\Request;

class UserController extends ApiController
{
    public function profile($username, Request $request)
    {
        $user = User::where('username', $username)
            ->withCount(['posts', 'subscribers'])
            ->firstOrFail();

        $authUser = $request->user();
        $hasSub = false;
        $viewerHasChatAccess = false;

        if ($authUser && $authUser->id !== $user->id) {
            $hasSub = Subscription::where('sender_user_id', $authUser->id)
                ->where('recipient_user_id', $user->id)
                ->whereIn('status', ['completed', 'active'])
                ->whereDate('expires_at', '>=', now())
                ->exists();

            $viewerHasChatAccess = $hasSub || $user->open_profile;
        }

        $isFollowing = false;
        if ($authUser && $authUser->id !== $user->id) {
            $isFollowing = ListsHelperServiceProvider::getUserFollowingType($user->id) !== null;
        }

        $receivedGifts = PostGift::where('recipient_user_id', $user->id)->count();
        $giftCredits = PostGift::where('recipient_user_id', $user->id)
            ->join('gifts', 'post_gifts.gift_id', '=', 'gifts.id')
            ->sum('gifts.credits');

        return $this->success([
            'user' => [
                'id' => $user->id,
                'name' => $user->name,
                'username' => $user->username,
                'bio' => $user->bio,
                'avatar' => $user->avatar,
                'cover' => $user->cover,
                'location' => $user->location,
                'website' => $user->website,
                'gender_pronoun' => $user->gender_pronoun,
                'birthdate' => $user->birthdate,
                'paid_profile' => (bool) $user->paid_profile,
                'profile_access_price' => (float) $user->profile_access_price,
                'profile_access_price_3_months' => (float) ($user->profile_access_price_3_months ?? 5),
                'profile_access_price_6_months' => (float) ($user->profile_access_price_6_months ?? 5),
                'profile_access_price_12_months' => (float) ($user->profile_access_price_12_months ?? 5),
                'public_profile' => (bool) $user->public_profile,
                'open_profile' => (bool) $user->open_profile,
                'posts_count' => (int) $user->posts_count,
                'subscribers_count' => (int) $user->subscribers_count,
                'created_at' => $user->created_at,
                'is_online' => \App\Providers\GenericHelperServiceProvider::isUserOnline($user->id),
                'is_verified' => $user->email_verified_at && $user->birthdate && ($user->verification && $user->verification->status === 'verified'),
                'has_subscribed' => $hasSub,
                'has_chat_access' => $viewerHasChatAccess,
                'is_following' => $isFollowing,
                'gifts_received_count' => $receivedGifts,
                'gifts_received_credits' => (int) $giftCredits,
            ],
        ]);
    }

    public function followers($username, Request $request)
    {
        $user = User::where('username', $username)->firstOrFail();
        $list = UserList::where('user_id', $user->id)->where('type', 'followers')->first();

        if (!$list) {
            return $this->success(['users' => []]);
        }

        $members = UserListMember::where('list_id', $list->id)
            ->with('member')
            ->paginate(20);

        return $this->success([
            'users' => $members->through(function ($member) {
                $u = $member->member;
                return [
                    'id' => $u->id,
                    'name' => $u->name,
                    'username' => $u->username,
                    'avatar' => $u->avatar,
                    'bio' => $u->bio,
                ];
            }),
            'has_more' => $members->hasMorePages(),
        ]);
    }

    public function follow($username, Request $request)
    {
        $target = User::where('username', $username)->firstOrFail();
        $authUser = $request->user();

        if ($target->id === $authUser->id) {
            return $this->error('Cannot follow yourself');
        }

        $list = ListsHelperServiceProvider::getUserFollowingType($target->id);

        if ($list) {
            \App\Model\UserListMember::where('list_id', $list)
                ->where('user_id', $target->id)
                ->delete();
            $following = false;
        } else {
            $userList = UserList::firstOrCreate([
                'user_id' => $authUser->id,
                'type' => 'following',
            ], [
                'name' => 'Following',
            ]);

            UserListMember::firstOrCreate([
                'list_id' => $userList->id,
                'user_id' => $target->id,
            ]);
            $following = true;
        }

        return $this->success([
            'following' => $following,
        ]);
    }

    public function userPosts($username, Request $request)
    {
        $user = User::where('username', $username)->firstOrFail();
        $paginated = $user->posts()
            ->with(['user', 'attachments', 'reactions', 'comments'])
            ->orderBy('created_at', 'desc')
            ->paginate((int) $request->get('per_page', 21));

        return $this->success([
            'posts' => array_map(function ($post) {
                return [
                    'id' => $post->id,
                    'text' => $post->text,
                    'price' => (float) $post->price,
                    'created_at' => $post->created_at,
                    'media' => ($post->attachments ?? collect())->map(function ($att) {
                        return [
                            'id' => $att->id,
                            'type' => $att->type,
                            'url' => $att->path,
                            'thumbnail' => $att->thumbnail,
                        ];
                    })->values()->toArray(),
                    'likes_count' => $post->reactions->count(),
                    'comments_count' => $post->comments->count(),
                    'has_liked' => $post->reactions->contains('user_id', auth()->id()),
                ];
            }, $paginated->items()),
            'has_more' => $paginated->hasMorePages(),
        ]);
    }
}
