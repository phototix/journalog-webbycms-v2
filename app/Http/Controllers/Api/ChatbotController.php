<?php

namespace App\Http\Controllers\Api;

use App\Model\User;
use App\Services\ChatbotService;
use Illuminate\Http\Request;

class ChatbotController extends ApiController
{
    public function send(Request $request)
    {
        $request->validate([
            'message' => 'required|string|max:2000',
        ]);

        $user = $request->user();
        $botUser = User::where('is_bot', true)->firstOrFail();

        $service = new ChatbotService($user);
        $userMessage = $request->get('message');

        // Save user message
        $userMsg = $service->saveMessage($user->id, $botUser->id, $userMessage);

        // Save placeholder bot message
        $botMsg = $service->saveMessage($botUser->id, $user->id, '...');

        // Dispatch background command to generate AI response
        $artisan = base_path('artisan');
        $logFile = storage_path('logs/chatbot-' . $botMsg->id . '.log');
        exec("php {$artisan} chatbot:generate {$user->id} {$userMsg->id} {$botMsg->id} > {$logFile} 2>&1 &");

        return $this->success([
            'user_message' => [
                'id' => $userMsg->id,
                'text' => $userMsg->message,
                'sender_id' => $user->id,
                'receiver_id' => $botUser->id,
                'is_mine' => true,
                'created_at' => $userMsg->created_at,
            ],
            'bot_message' => [
                'id' => $botMsg->id,
                'text' => $botMsg->message,
                'sender_id' => $botUser->id,
                'receiver_id' => $user->id,
                'is_mine' => false,
                'created_at' => $botMsg->created_at,
            ],
        ]);
    }
}
