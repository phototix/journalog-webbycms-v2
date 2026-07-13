<?php

namespace App\Filament\Pages\Settings;

use App\Settings\AiSettings;
use BackedEnum;
use BezhanSalleh\FilamentShield\Traits\HasPageShield;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Pages\SettingsPage;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;
use Illuminate\Support\Facades\Crypt;

class ManageAiSettings extends SettingsPage
{
    use HasPageShield;

    protected static BackedEnum|string|null $navigationIcon = 'heroicon-o-cpu-chip';

    protected static ?string $slug = 'settings/ai';

    protected static string $settings = AiSettings::class;

    protected static ?string $title = 'AI Settings';

    public function form(Schema $schema): Schema
    {
        return $schema->components([
            Section::make()
                ->columnSpanFull()
                ->columns(2)
                ->schema([
                    Toggle::make('open_ai_enabled')
                        ->label('Enable OpenAI')
                        ->columnSpanFull(),

                    Select::make('open_ai_model')
                        ->label('OpenAI model')
                        ->options([
                            'gpt-5-chat-latest' => 'GPT 5',
                            'gpt-4o' => 'GPT 4o',
                            'gpt-4o-mini' => 'GPT 4o-mini',
                            'o3' => 'GPT 3',
                        ])
                        ->required()
                        ->helperText("Select the OpenAI model to be used. For more details and pricing, check OpenAi docs."),

                    TextInput::make('open_ai_api_key')
                        ->label('OpenAI API key')
                        ->password()
                        ->revealable()
                        ->required()
                        ->helperText('Your API key is encrypted at rest in the database. Use the eye icon to toggle visibility.'),

                    TextInput::make('open_ai_completion_max_tokens')
                        ->label('Max tokens')
                        ->numeric()
                        ->required()
                        ->helperText("Dictates how long the suggestion should be. E.g. 1000 tokens is about 750 words. (shouldn`t exceed 2048 tokens)."),

                    TextInput::make('open_ai_completion_temperature')
                        ->label('Temperature')
                        ->numeric()
                        ->minValue(0)
                        ->maxValue(2)
                        ->step(0.1)
                        ->required()
                        ->helperText("What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic."),
                ]),
        ]);
    }

    protected function mutateFormDataBeforeFill(array $data): array
    {
        if (!empty($data['open_ai_api_key'])) {
            try {
                $data['open_ai_api_key'] = Crypt::decryptString($data['open_ai_api_key']);
            } catch (\Exception $e) {
                // Value is not encrypted yet (e.g., from migration seed), keep as-is
            }
        }

        return $data;
    }

    protected function mutateFormDataBeforeSave(array $data): array
    {
        if (!empty($data['open_ai_api_key'])) {
            $data['open_ai_api_key'] = Crypt::encryptString($data['open_ai_api_key']);
        }

        return $data;
    }
}
