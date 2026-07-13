<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    public function up(): void
    {
        DB::statement("
            UPDATE users
            SET settings = JSON_SET(
                COALESCE(settings, '{}'),
                '$.notification_email_new_message',
                'true'
            )
            WHERE JSON_EXTRACT(settings, '$.notification_email_new_message') = true
        ");
    }

    public function down(): void
    {
        DB::statement("
            UPDATE users
            SET settings = JSON_SET(
                COALESCE(settings, '{}'),
                '$.notification_email_new_message',
                CAST('true' AS JSON)
            )
            WHERE JSON_UNQUOTE(JSON_EXTRACT(settings, '$.notification_email_new_message')) = 'true'
        ");
    }
};
