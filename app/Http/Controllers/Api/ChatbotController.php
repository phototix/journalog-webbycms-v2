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

        // Get full bot response
        $fullResponse = '';
        $error = null;
        try {
            foreach ($service->streamCompletion($userMessage) as $chunk) {
                if (isset($chunk['error'])) {
                    $error = $chunk['error'];
                    break;
                }
                if (isset($chunk['token'])) {
                    $fullResponse .= $chunk['token'];
                }
                if (isset($chunk['done']) && $chunk['done'] === true) {
                    break;
                }
            }
        } catch (\Exception $e) {
            $error = $e->getMessage();
        }

        // Save bot response
        if ($fullResponse && !$error) {
            $botMsg = $service->saveMessage($botUser->id, $user->id, $fullResponse);
        }

        return $this->success([
            'user_message' => [
                'id' => $userMsg->id,
                'text' => $userMsg->message,
                'sender_id' => $user->id,
                'receiver_id' => $botUser->id,
                'is_mine' => true,
                'created_at' => $userMsg->created_at,
            ],
            'bot_message' => isset($botMsg) ? [
                'id' => $botMsg->id,
                'text' => $botMsg->message,
                'sender_id' => $botUser->id,
                'receiver_id' => $user->id,
                'is_mine' => false,
                'created_at' => $botMsg->created_at,
            ] : null,
            'response' => $fullResponse,
            'error' => $error,
        ], $error ? 'Error generating response' : 'OK');
    }
}
