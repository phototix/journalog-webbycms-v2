<?php

namespace App\Settings;

use Spatie\LaravelSettings\Settings;

class FreeCreditsSignupSettings extends Settings
{
    public bool $enabled = false;

    public ?int $amount = 500;

    public static function group(): string
    {
        return 'free-credits-signup';
    }
}
