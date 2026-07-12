<?php

namespace App\Filament\Resources\Gifts\Pages;

use App\Filament\Resources\Gifts\GiftResource;
use Filament\Actions;
use Filament\Resources\Pages\ListRecords;

class ListGifts extends ListRecords
{
    protected static string $resource = GiftResource::class;

    protected function getHeaderActions(): array
    {
        return [
            Actions\CreateAction::make(),
        ];
    }
}
