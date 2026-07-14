<?php

namespace App\Console\Commands;

use App\Model\User;
use App\Model\UserMessage;
use App\Services\ChatbotService;
use Illuminate\Console\Command;

class GenerateChatbotResponse extends Command
{
    protected $signature = 'chatbot:generate {userId} {userMessageId} {botMessageId}';
    protected $description = 'Generate AI chatbot response in background';

    public function handle()
    {
        $userId = $this->argument('userId');
        $userMessageId = $this->argument('userMessageId');
        $botMessageId = $this->argument('botMessageId');

        set_time_limit(120);

        $user = User::find($userId);
        $userMsg = UserMessage::find($userMessageId);
        $botMsg = UserMessage::find($botMessageId);

        if (!$user || !$userMsg || !$botMsg) {
            $this->error('Required records not found');
            return 1;
        }

        $service = new ChatbotService($user);
        $fullResponse = '';

        try {
            foreach ($service->streamCompletion($userMsg->message) as $chunk) {
                if (isset($chunk['token'])) {
                    $fullResponse .= $chunk['token'];
                }
                if (isset($chunk['done'])) break;
                if (isset($chunk['error'])) {
                    $botMsg->update(['message' => 'Error: ' . $chunk['error']]);
                    return 1;
                }
            }
        } catch (\Exception $e) {
            $botMsg->update(['message' => 'Error: ' . $e->getMessage()]);
            return 1;
        }

        if ($fullResponse) {
            $botMsg->update(['message' => $fullResponse]);
            $this->info('Bot response generated successfully');
        }

        return 0;
    }
}
