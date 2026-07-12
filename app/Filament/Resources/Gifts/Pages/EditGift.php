<?php

namespace App\Filament\Resources\Gifts\Pages;

use App\Filament\Resources\Gifts\GiftResource;
use Filament\Actions;
use Filament\Resources\Pages\EditRecord;

class EditGift extends EditRecord
{
    protected static string $resource = GiftResource::class;

    protected function getHeaderActions(): array
    {
        return [
            Actions\DeleteAction::make(),
        ];
    }
}
