<?php

namespace App\Model;

use Illuminate\Database\Eloquent\Model;

class PostGift extends Model
{
    protected $fillable = [
        'post_id',
        'gift_id',
        'sender_user_id',
        'recipient_user_id',
        'transaction_id',
    ];

    public function post()
    {
        return $this->belongsTo(Post::class);
    }

    public function gift()
    {
        return $this->belongsTo(Gift::class);
    }

    public function sender()
    {
        return $this->belongsTo(User::class, 'sender_user_id');
    }

    public function recipient()
    {
        return $this->belongsTo(User::class, 'recipient_user_id');
    }

    public function transaction()
    {
        return $this->belongsTo(Transaction::class);
    }
}
