<?php

namespace App\Filament\Resources\Gifts;

use App\Filament\Resources\Gifts\Pages\CreateGift;
use App\Filament\Resources\Gifts\Pages\EditGift;
use App\Filament\Resources\Gifts\Pages\ListGifts;
use App\Filament\Traits\ResolvesRecordUrl;
use App\Model\Gift;
use Filament\Actions\DeleteAction;
use Filament\Actions\DeleteBulkAction;
use Filament\Forms;
use Filament\Forms\Components\FileUpload;
use Filament\Resources\Resource;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;
use Filament\Tables;
use Filament\Tables\Filters\QueryBuilder;
use Filament\Tables\Filters\QueryBuilder\Constraints\TextConstraint;
use Filament\Tables\Table;
use UnitEnum;

class GiftResource extends Resource
{
    use ResolvesRecordUrl;

    protected static ?string $model = Gift::class;

    protected static UnitEnum|string|null $navigationGroup = 'Finances';

    protected static ?int $navigationSort = 10;

    public static function getModelLabel(): string
    {
        return __('Gift');
    }

    public static function getPluralModelLabel(): string
    {
        return __('Gifts');
    }

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            Section::make(__('Gift Details'))
                ->columnSpanFull()
                ->schema([
                    Forms\Components\TextInput::make('name')
                        ->label(__('Name'))
                        ->required()
                        ->maxLength(255)
                        ->columnSpanFull(),

                    Forms\Components\TextInput::make('icon')
                        ->label(__('Icon (CSS class)'))
                        ->required()
                        ->maxLength(255)
                        ->columnSpanFull()
                        ->helperText(__('e.g. heart-outline, star, cash, etc. Uses Ionicons.')),

                    FileUpload::make('gif_effect')
                        ->label(__('GIF Effect'))
                        ->directory('gifts/effects')
                        ->acceptedFileTypes(['image/gif', 'image/webp'])
                        ->maxSize(2048)
                        ->columnSpanFull(),

                    Forms\Components\TextInput::make('credits')
                        ->label(__('Credits'))
                        ->required()
                        ->numeric()
                        ->minValue(1)
                        ->default(1),

                    Forms\Components\Select::make('category')
                        ->label(__('Category'))
                        ->required()
                        ->options([
                            Gift::CATEGORY_ROMANTIC => __('Romantic'),
                            Gift::CATEGORY_FUNNY => __('Funny'),
                            Gift::CATEGORY_PREMIUM => __('Premium'),
                            Gift::CATEGORY_LIMITED_EDITION => __('Limited-Edition'),
                        ])
                        ->default(Gift::CATEGORY_FUNNY),

                    Forms\Components\Toggle::make('is_active')
                        ->label(__('Active'))
                        ->default(true),

                    Forms\Components\TextInput::make('sort_order')
                        ->label(__('Sort Order'))
                        ->numeric()
                        ->default(0)
                        ->integer(),
                ]),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('name')
                    ->label(__('Name'))
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('icon')
                    ->label(__('Icon'))
                    ->searchable(),

                Tables\Columns\TextColumn::make('credits')
                    ->label(__('Credits'))
                    ->numeric()
                    ->sortable(),

                Tables\Columns\TextColumn::make('category')
                    ->label(__('Category'))
                    ->badge()
                    ->color(fn (string $state): string => match ($state) {
                        'Romantic' => 'danger',
                        'Funny' => 'warning',
                        'Premium' => 'success',
                        'Limited-Edition' => 'info',
                        default => 'gray',
                    })
                    ->sortable(),

                Tables\Columns\IconColumn::make('is_active')
                    ->label(__('Active'))
                    ->boolean()
                    ->sortable(),

                Tables\Columns\TextColumn::make('sort_order')
                    ->label(__('Sort Order'))
                    ->numeric()
                    ->sortable(),

                Tables\Columns\TextColumn::make('created_at')
                    ->label(__('Created'))
                    ->dateTime()
                    ->sortable()
                    ->toggleable(isToggledHiddenByDefault: true),
            ])
            ->filters([
                QueryBuilder::make()
                    ->constraints([
                        TextConstraint::make('name')->label(__('Name')),
                        TextConstraint::make('category')->label(__('Category')),
                    ])
                    ->constraintPickerColumns(2),
            ], layout: Tables\Enums\FiltersLayout::Dropdown)
            ->deferFilters()
            ->actions([
                DeleteAction::make(),
            ])
            ->toolbarActions([
                DeleteBulkAction::make(),
            ])
            ->recordUrl(fn ($record) => static::resolveRecordUrl($record))
            ->defaultSort('sort_order')
            ->reorderable('sort_order')
            ->paginated([10, 25, 50]);
    }

    public static function getRelations(): array
    {
        return [];
    }

    public static function getPages(): array
    {
        return [
            'index' => ListGifts::route('/'),
            'create' => CreateGift::route('/create'),
            'edit' => EditGift::route('/{record}/edit'),
        ];
    }
}
