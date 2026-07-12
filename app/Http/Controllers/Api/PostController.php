<?php

namespace App\Http\Controllers\Api;

use App\Model\Poll;
use App\Model\PollAnswer;
use App\Model\PollUserAnswer;
use App\Model\Post;
use App\Model\PostComment;
use App\Model\PostGift;
use App\Model\Reaction;
use App\Model\UserBookmark;
use Illuminate\Http\Request;

class PostController extends ApiController
{
    public function show($id)
    {
        $post = Post::with([
            'user', 'attachments', 'reactions', 'comments.user',
            'poll.answers', 'gifts.gift',
        ])->withCount(['gifts', 'tips'])->findOrFail($id);

        return $this->success([
            'post' => $this->formatPostDetail($post),
        ]);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'text' => 'required|string|min:' . (int) getSetting('feed.min_post_description', 1),
            'price' => 'nullable|numeric|min:0',
            'attachment_ids' => 'nullable|array',
            'attachment_ids.*' => 'exists:attachments,id',
            'release_date' => 'nullable|date',
            'expire_date' => 'nullable|date|after:release_date',
            'poll_answers' => 'nullable|array|min:2|max:10',
            'poll_answers.*' => 'required|string|max:255',
        ]);

        $post = Post::create([
            'user_id' => $request->user()->id,
            'text' => $validated['text'],
            'price' => $validated['price'] ?? 0,
            'status' => Post::APPROVED_STATUS,
            'release_date' => $validated['release_date'] ?? null,
            'expire_date' => $validated['expire_date'] ?? null,
        ]);

        if (!empty($validated['attachment_ids'])) {
            \App\Model\Attachment::whereIn('id', $validated['attachment_ids'])
                ->update(['post_id' => $post->id]);
        }

        if (!empty($validated['poll_answers'])) {
            $poll = Poll::create(['post_id' => $post->id]);
            foreach ($validated['poll_answers'] as $answerText) {
                PollAnswer::create([
                    'poll_id' => $poll->id,
                    'answer' => $answerText,
                ]);
            }
        }

        return $this->success([
            'post' => $this->formatPostDetail($post->fresh()->load([
                'user', 'attachments', 'reactions', 'comments.user',
                'poll.answers', 'gifts.gift',
            ])),
        ], 'Post created', 201);
    }

    public function destroy($id, Request $request)
    {
        $post = Post::where('user_id', $request->user()->id)->findOrFail($id);
        $post->delete();

        return $this->success(null, 'Post deleted');
    }

    public function like($id, Request $request)
    {
        $post = Post::findOrFail($id);
        $existing = Reaction::where('user_id', $request->user()->id)
            ->where('post_id', $id)
            ->first();

        if ($existing) {
            $existing->delete();
            $liked = false;
        } else {
            Reaction::create([
                'user_id' => $request->user()->id,
                'post_id' => $id,
                'type' => 'like',
            ]);
            $liked = true;
        }

        return $this->success([
            'liked' => $liked,
            'likes_count' => $post->reactions()->count(),
        ]);
    }

    public function comments($id)
    {
        $post = Post::findOrFail($id);
        $comments = $post->comments()->with('user')->orderBy('created_at', 'desc')->paginate(20);

        return $this->success([
            'comments' => $comments->through(function ($comment) {
                return [
                    'id' => $comment->id,
                    'text' => $comment->text,
                    'created_at' => $comment->created_at,
                    'user' => [
                        'id' => $comment->user->id,
                        'name' => $comment->user->name,
                        'username' => $comment->user->username,
                        'avatar' => $comment->user->avatar,
                    ],
                ];
            }),
            'has_more' => $comments->hasMorePages(),
        ]);
    }

    public function addComment($id, Request $request)
    {
        $validated = $request->validate([
            'text' => 'required|string|max:2000',
        ]);

        $post = Post::findOrFail($id);
        $comment = PostComment::create([
            'user_id' => $request->user()->id,
            'post_id' => $post->id,
            'text' => $validated['text'],
        ]);

        $comment->load('user');

        return $this->success([
            'comment' => [
                'id' => $comment->id,
                'text' => $comment->text,
                'created_at' => $comment->created_at,
                'user' => [
                    'id' => $comment->user->id,
                    'name' => $comment->user->name,
                    'username' => $comment->user->username,
                    'avatar' => $comment->user->avatar,
                ],
            ],
        ], 'Comment added', 201);
    }

    public function deleteComment($id, Request $request)
    {
        $comment = PostComment::where('user_id', $request->user()->id)->findOrFail($id);
        $comment->delete();

        return $this->success(null, 'Comment deleted');
    }

    public function bookmark($id, Request $request)
    {
        $existing = UserBookmark::where('user_id', $request->user()->id)
            ->where('post_id', $id)
            ->first();

        if ($existing) {
            $existing->delete();
            $bookmarked = false;
        } else {
            UserBookmark::create([
                'user_id' => $request->user()->id,
                'post_id' => $id,
            ]);
            $bookmarked = true;
        }

        return $this->success([
            'bookmarked' => $bookmarked,
        ]);
    }

    public function votePoll($id, Request $request)
    {
        $post = Post::findOrFail($id);
        $poll = $post->poll;

        if (!$poll) {
            return $this->error('This post has no poll', 404);
        }

        $validated = $request->validate([
            'poll_answer_id' => 'required|exists:poll_answers,id',
        ]);

        $answer = PollAnswer::where('poll_id', $poll->id)
            ->where('id', $validated['poll_answer_id'])
            ->first();

        if (!$answer) {
            return $this->error('Invalid poll answer', 422);
        }

        $existing = PollUserAnswer::where('poll_answer_id', $answer->id)
            ->where('user_id', $request->user()->id)
            ->first();

        if ($existing) {
            return $this->error('You have already voted', 422);
        }

        // Check if user voted any answer in this poll
        $allAnswers = PollAnswer::where('poll_id', $poll->id)->pluck('id');
        $alreadyVoted = PollUserAnswer::whereIn('poll_answer_id', $allAnswers)
            ->where('user_id', $request->user()->id)
            ->exists();

        if ($alreadyVoted) {
            return $this->error('You have already voted on this poll', 422);
        }

        PollUserAnswer::create([
            'poll_answer_id' => $answer->id,
            'user_id' => $request->user()->id,
        ]);

        return $this->success([
            'poll' => $this->formatPoll($poll),
        ], 'Vote recorded');
    }

    private function formatPoll($poll): ?array
    {
        if (!$poll) return null;

        $answers = $poll->answers;
        $totalVotes = $answers->sum('votes_count');

        return [
            'id' => $poll->id,
            'answers' => $answers->map(function ($a) use ($totalVotes) {
                return [
                    'id' => $a->id,
                    'answer' => $a->answer,
                    'votes_count' => (int) $a->votes_count,
                    'percentage' => $totalVotes > 0 ? round(($a->votes_count / $totalVotes) * 100) : 0,
                ];
            }),
            'total_votes' => $totalVotes,
        ];
    }

    private function formatPostDetail($post): array
    {
        $attachments = $post->attachments ?? collect();
        $poll = $post->poll;

        return [
            'id' => $post->id,
            'text' => $post->text,
            'price' => (float) $post->price,
            'status' => $post->status,
            'is_pinned' => (bool) $post->is_pinned,
            'release_date' => $post->release_date,
            'expire_date' => $post->expire_date,
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
            'poll' => $this->formatPoll($poll),
            'gifts' => ($post->gifts ?? collect())->groupBy('gift_id')->map(function ($gifts, $giftId) {
                $first = $gifts->first();
                return [
                    'gift_id' => (int) $giftId,
                    'count' => $gifts->count(),
                    'gift' => $first->gift ? [
                        'id' => $first->gift->id,
                        'name' => $first->gift->name,
                        'icon' => $first->gift->icon,
                        'credits' => $first->gift->credits,
                    ] : null,
                ];
            })->values(),
            'gifts_count' => (int) ($post->gifts_count ?? $post->gifts->count()),
            'likes_count' => $post->reactions->count(),
            'comments_count' => $post->comments->count(),
            'has_liked' => $post->reactions->contains('user_id', auth()->id()),
            'has_bookmarked' => $post->bookmarks->contains('user_id', auth()->id()),
        ];
    }
}
