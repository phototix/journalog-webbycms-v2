<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Spatie\LaravelSettings\Migrations\SettingsMigration;

return new class extends SettingsMigration
{
    public function up(): void
    {
        // 1. Update Spatie-style settings (active table after v9.0.0 rename)
        //    This is the primary data source since ADMIN_VERSION=v2
        $this->migrator->update('site.name', fn () => 'Journalog');
        try {
            $this->migrator->update('admin.title', fn () => 'Journalog Admin');
        } catch (\Throwable $e) {
            // Admin title may not be in Spatie format yet
        }

        // 2. Direct DB update for the Spatie settings table (group/name/payload)
        if (Schema::hasTable('settings') && Schema::hasColumn('settings', 'payload')) {
            DB::table('settings')
                ->where('group', 'site')
                ->where('name', 'name')
                ->update(['payload' => json_encode('Journalog'), 'updated_at' => now()]);
        }

        // 3. Also update old Voyager table if it still exists (settings_old)
        foreach (['settings_old', 'settings'] as $table) {
            if (Schema::hasTable($table) && Schema::hasColumn($table, 'key')) {
                DB::table($table)->where('key', 'site.name')->update(['value' => 'Journalog']);
                DB::table($table)->where('key', 'admin.title')->update(['value' => 'Journalog Admin']);
                DB::table($table)->where('key', 'admin.description')
                    ->update(['value' => 'Welcome to Journalog Admin Panel. Log in to manage and customize your site!']);
            }
        }

        // 4. Clear ALL caches that might hold the old value
        try {
            Cache::tags(['settings'])->flush();
        } catch (\Throwable $e) {
            // Tags not supported
        }
        Cache::forget('settings::site.name');
        Cache::forget('settings::admin.title');
        Cache::forget('settings::admin.description');

        // 5. Force the Spatie settings repository to update the in-memory value
        try {
            app(\App\Settings\GeneralSettings::class)->refresh();
        } catch (\Throwable $e) {
            // Settings class may not be registered yet
        }

        // 6. Clear Laravel caches
        Artisan::call('view:clear');
        Artisan::call('config:clear');
    }

    public function down(): void
    {
    }
};
