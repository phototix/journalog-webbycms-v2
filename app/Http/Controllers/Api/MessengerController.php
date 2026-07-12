<?php

namespace App\Http\Controllers\Api;

use App\Model\User;
use App\Model\UserMessage;
use App\Providers\GenericHelperServiceProvider;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;

class MessengerController extends ApiController
{
    public function conversations(Request $request)
    {
        $userID = $request->user()->id;

        $contacts = DB::select('
        SELECT *
         FROM (
            SELECT
             t1.sender_id as lastMessageSenderID,
             t1.message as lastMessage,
             t1.isSeen,
             t1.created_at as messageDate,
             senderDetails.id as senderID,
             senderDetails.name as senderName,
             senderDetails.avatar as senderAvatar,
             senderDetails.role_id as senderRole,
             receiverDetails.id as receiverID,
             receiverDetails.name as receiverName,
             receiverDetails.avatar as receiverAvatar,
             receiverDetails.role_id as receiverRole,
             IF(receiverDetails.id = ?, senderDetails.id, receiverDetails.id) as contactID
            FROM user_messages AS t1
            INNER JOIN
            (
                SELECT
                    LEAST(receiver_id, sender_id) AS receiverID,
                    GREATEST(receiver_id, sender_id) AS senderID,
                    MAX(id) AS max_id
                FROM user_messages
                GROUP BY
                    LEAST(receiver_id, sender_id),
                    GREATEST(receiver_id, sender_id)
            ) AS t2
                ON LEAST(t1.receiver_id, t1.sender_id) = t2.receiverID AND
                   GREATEST(t1.receiver_id, t1.sender_id) = t2.senderID AND
                   t1.id = t2.max_id
            INNER JOIN users senderDetails ON t1.sender_id = senderDetails.id
            INNER JOIN users receiverDetails ON t1.receiver_id = receiverDetails.id
            WHERE  (t1.receiver_id = ? OR t1.sender_id = ?)
                ) as contactsData
                ORDER BY contactsData.messageDate DESC
        ', [$userID, $userID, $userID]);

        $conversations = array_map(function ($contact) use ($userID) {
            $contactId = (int) $contact->contactID;
            $name = $contact->senderID === $userID ? $contact->receiverName : $contact->senderName;
            $avatar = $contact->senderID === $userID ? $contact->receiverAvatar : $contact->senderAvatar;

            $unreadCount = UserMessage::where('sender_id', $contactId)
                ->where('receiver_id', $userID)
                ->where('isSeen', false)
                ->count();

            return [
                'contact_id' => $contactId,
                'name' => $name,
                'avatar' => GenericHelperServiceProvider::getStorageAvatarPath($avatar),
                'last_message' => $contact->lastMessage,
                'last_message_date' => $contact->messageDate,
                'unread_count' => $unreadCount,
            ];
        }, $contacts);

        return $this->success([
            'conversations' => array_values($conversations),
        ]);
    }

    public function messages($userId, Request $request)
    {
        $userID = $request->user()->id;

        $messages = UserMessage::where(function ($q) use ($userID, $userId) {
            $q->where('sender_id', $userID)->where('receiver_id', $userId);
        })->orWhere(function ($q) use ($userID, $userId) {
            $q->where('sender_id', $userId)->where('receiver_id', $userID);
        })->orderBy('created_at', 'desc')->paginate(50);

        UserMessage::where('sender_id', $userId)
            ->where('receiver_id', $userID)
            ->where('isSeen', false)
            ->update(['isSeen' => true]);

        return $this->success([
            'messages' => $messages->through(function ($msg) {
                return [
                    'id' => $msg->id,
                    'text' => $msg->message,
                    'sender_id' => $msg->sender_id,
                    'receiver_id' => $msg->receiver_id,
                    'is_mine' => $msg->sender_id === auth()->id(),
                    'is_seen' => (bool) $msg->isSeen,
                    'price' => (float) $msg->price,
                    'created_at' => $msg->created_at,
                    'attachments' => ($msg->attachments ?? collect())->map(function ($att) {
                        return [
                            'id' => $att->id,
                            'type' => $att->type,
                            'url' => $att->path,
                        ];
                    }),
                ];
            }),
            'has_more' => $messages->hasMorePages(),
        ]);
    }

    public function sendMessage(Request $request)
    {
        $validated = $request->validate([
            'receiver_id' => 'required|exists:users,id',
            'message' => 'required_without:attachment_ids|string',
            'attachment_ids' => 'nullable|array',
            'attachment_ids.*' => 'exists:attachments,id',
            'price' => 'nullable|numeric|min:0',
        ]);

        $message = UserMessage::create([
            'sender_id' => $request->user()->id,
            'receiver_id' => $validated['receiver_id'],
            'message' => $validated['message'] ?? '',
            'price' => $validated['price'] ?? 0,
        ]);

        if (!empty($validated['attachment_ids'])) {
            \App\Model\Attachment::whereIn('id', $validated['attachment_ids'])
                ->update(['message_id' => $message->id]);
        }

        return $this->success([
            'message' => [
                'id' => $message->id,
                'text' => $message->message,
                'sender_id' => $message->sender_id,
                'receiver_id' => $message->receiver_id,
                'is_mine' => true,
                'created_at' => $message->created_at,
            ],
        ], 'Message sent', 201);
    }
}
