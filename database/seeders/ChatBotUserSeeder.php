<?php

namespace Database\Seeders;

use App\Model\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class ChatBotUserSeeder extends Seeder
{
    public function run(): void
    {
        User::firstOrCreate(
            ['email' => 'assistant@localhost'],
            [
                'name' => 'Assistant',
                'username' => 'assistant',
                'password' => Hash::make(Str::random(40)),
                'is_bot' => true,
                'role_id' => 1,
                'email_verified_at' => now(),
                'public_profile' => true,
                'open_profile' => true,
            ]
        );
    }
}
