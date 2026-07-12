<?php

namespace App\Model;

use Illuminate\Database\Eloquent\Model;

class Gift extends Model
{
    public const CATEGORY_ROMANTIC = 'Romantic';
    public const CATEGORY_FUNNY = 'Funny';
    public const CATEGORY_PREMIUM = 'Premium';
    public const CATEGORY_LIMITED_EDITION = 'Limited-Edition';

    public const CATEGORIES = [
        self::CATEGORY_ROMANTIC,
        self::CATEGORY_FUNNY,
        self::CATEGORY_PREMIUM,
        self::CATEGORY_LIMITED_EDITION,
    ];

    protected $fillable = [
        'name',
        'icon',
        'gif_effect',
        'credits',
        'category',
        'is_active',
        'sort_order',
    ];

    protected $casts = [
        'is_active' => 'boolean',
        'credits' => 'integer',
        'sort_order' => 'integer',
    ];

    public function postGifts()
    {
        return $this->hasMany(PostGift::class);
    }

    public function scopeActive($query)
    {
        return $query->where('is_active', true);
    }

    public function scopeOrdered($query)
    {
        return $query->orderBy('sort_order')->orderBy('name');
    }
}
