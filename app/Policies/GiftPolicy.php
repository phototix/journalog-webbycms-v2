<?php

declare(strict_types=1);

namespace App\Policies;

use App\Model\Gift;
use Illuminate\Auth\Access\HandlesAuthorization;
use Illuminate\Foundation\Auth\User as AuthUser;

class GiftPolicy
{
    use HandlesAuthorization;

    public function viewAny(AuthUser $authUser): bool
    {
        return $authUser->can('ViewAny:Gift');
    }

    public function view(AuthUser $authUser, Gift $gift): bool
    {
        return $authUser->can('View:Gift');
    }

    public function create(AuthUser $authUser): bool
    {
        return $authUser->can('Create:Gift');
    }

    public function update(AuthUser $authUser, Gift $gift): bool
    {
        return $authUser->can('Update:Gift');
    }

    public function delete(AuthUser $authUser, Gift $gift): bool
    {
        return $authUser->can('Delete:Gift');
    }

    public function deleteAny(AuthUser $authUser): bool
    {
        return $authUser->can('Delete:Gift');
    }
}
