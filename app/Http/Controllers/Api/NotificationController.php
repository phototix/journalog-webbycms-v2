<?php

namespace App\Http\Controllers\Api;

use App\Model\Notification;
use Illuminate\Http\Request;

class NotificationController extends ApiController
{
    public function index(Request $request)
    {
        $userId = $request->user()->id;

        $notifications = Notification::where('to_user_id', $userId)
            ->with('fromUser')
            ->orderBy('created_at', 'desc')
            ->paginate(20);

        return $this->success([
            'notifications' => $notifications->through(function ($n) {
                return [
                    'id' => $n->id,
                    'type' => $n->type,
                    'message' => $n->message,
                    'read' => (bool) $n->read,
                    'created_at' => $n->created_at,
                    'actor' => $n->fromUser ? [
                        'id' => $n->fromUser->id,
                        'name' => $n->fromUser->name,
                        'username' => $n->fromUser->username,
                        'avatar' => $n->fromUser->avatar,
                    ] : null,
                ];
            }),
            'unread_count' => Notification::where('to_user_id', $userId)
                ->where('read', false)
                ->count(),
            'has_more' => $notifications->hasMorePages(),
        ]);
    }

    public function markRead($id, Request $request)
    {
        $notification = Notification::where('to_user_id', $request->user()->id)
            ->where('id', $id)
            ->firstOrFail();

        $notification->update(['read' => true]);

        return $this->success(null, 'Marked as read');
    }

    public function markAllRead(Request $request)
    {
        Notification::where('to_user_id', $request->user()->id)
            ->where('read', false)
            ->update(['read' => true]);

        return $this->success(null, 'All marked as read');
    }
}
