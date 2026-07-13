<?php

use Spatie\LaravelSettings\Migrations\SettingsMigration;

return new class extends SettingsMigration
{
    public function up(): void
    {
        $this->migrator->add('free-credits-signup.enabled', false);
        $this->migrator->add('free-credits-signup.amount', 500);
    }

    public function down(): void
    {
        $this->migrator->delete('free-credits-signup.enabled');
        $this->migrator->delete('free-credits-signup.amount');
    }
};
