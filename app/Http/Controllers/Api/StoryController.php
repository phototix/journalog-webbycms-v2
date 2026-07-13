<?php

namespace App\Http\Controllers\Api;

use App\Model\Story;
use App\Model\StoryView;
use App\Providers\StoriesServiceProvider;
use Illuminate\Http\Request;

class StoryController extends ApiController
{
    public function feed(Request $request)
    {
        $user = $request->user();
        $stories = StoriesServiceProvider::forFeed($user);
        $payload = StoriesServiceProvider::toFrontendPayload($stories);

        return $this->success($payload);
    }

    public function view($id, Request $request)
    {
        $story = Story::findOrFail($id);

        StoryView::firstOrCreate([
            'story_id' => $story->id,
            'user_id' => $request->user()->id,
        ]);

        return $this->success(null, 'Story viewed');
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'attachment_id' => 'required|exists:attachments,id',
        ]);

        $story = Story::create([
            'user_id' => $request->user()->id,
            'text' => $request->get('text'),
            'overlay' => $request->get('overlay'),
            'bg_preset' => $request->get('bg_preset'),
        ]);

        \App\Model\Attachment::where('id', $validated['attachment_id'])
            ->update(['story_id' => $story->id]);

        return $this->success([
            'story' => [
                'id' => $story->id,
                'created_at' => $story->created_at,
            ],
        ], 'Story created', 201);
    }

    public function destroy($id, Request $request)
    {
        $story = Story::where('user_id', $request->user()->id)->findOrFail($id);
        $story->delete();

        return $this->success(null, 'Story deleted');
    }
}
