<?php

namespace App\Http\Controllers;

use App\Services\ChatbotService;
use App\Model\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class ChatbotController extends Controller
{
    public function send(Request $request)
    {
        $request->validate(['message' => 'required|string|max:2000']);

        $user = Auth::user();
        $botUser = User::where('is_bot', true)->firstOrFail();
        $service = new ChatbotService($user);

        $userMessage = $request->input('message');
        $userMsg = $service->saveMessage($user->id, $botUser->id, $userMessage);
        $service->broadcastMessage($userMsg);

        $fullResponse = '';

        return response()->stream(function () use ($service, $user, $botUser, $userMessage, &$fullResponse) {
            set_time_limit(120);
            $errorOccurred = false;

            try {
                foreach ($service->streamCompletion($userMessage) as $type => $data) {
                    if ($type === '[DONE]') {
                        if (!empty($fullResponse)) {
                            $botMsg = $service->saveMessage($botUser->id, $user->id, $fullResponse);
                            $service->broadcastMessage($botMsg);
                        }
                        echo "data: [DONE]\n\n";
                        ob_flush();
                        flush();
                        return;
                    }
                    if ($type === 'token') {
                        $fullResponse .= $data;
                        echo "data: " . json_encode(['token' => $data]) . "\n\n";
                        ob_flush();
                        flush();
                    }
                    if ($type === 'error') {
                        $errorOccurred = true;
                        echo "data: " . json_encode(['error' => $data]) . "\n\n";
                        ob_flush();
                        flush();
                        echo "data: [DONE]\n\n";
                        ob_flush();
                        flush();
                        return;
                    }
                }
            } catch (\Throwable $e) {
                $errorOccurred = true;
                echo "data: " . json_encode(['error' => $e->getMessage()]) . "\n\n";
                ob_flush();
                flush();
            }

            if (!$errorOccurred && !empty($fullResponse)) {
                $botMsg = $service->saveMessage($botUser->id, $user->id, $fullResponse);
                $service->broadcastMessage($botMsg);
            }
            echo "data: [DONE]\n\n";
            ob_flush();
            flush();
        }, 200, [
            'Content-Type' => 'text/event-stream',
            'Cache-Control' => 'no-cache',
            'X-Accel-Buffering' => 'no',
        ]);
    }
}
