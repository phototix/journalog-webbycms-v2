<?php

namespace App\Rules;

use Illuminate\Contracts\Validation\Rule;

class BannedUsername implements Rule
{
    protected array $extra = [];

    public function __construct(array $extra = [])
    {
        $this->extra = $extra;
    }

    public function passes($attribute, $value): bool
    {
        $banned = array_merge([
            'admin', 'root', 'system', 'moderator', 'mod', 'support',
            'help', 'official', 'staff', 'team', 'owner', 'administrator',
            'superadmin', 'webmaster', 'host', 'master', 'manager',
        ], $this->extra);

        return !in_array(strtolower($value), $banned);
    }

    public function message(): string
    {
        return __('The :attribute is reserved and cannot be used.');
    }
}
