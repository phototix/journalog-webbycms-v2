<?php

namespace App\Filament\Pages\Settings;

use App\Model\User;
use App\Settings\FreeCreditsSignupSettings;
use BackedEnum;
use BezhanSalleh\FilamentShield\Traits\HasPageShield;
use Filament\Actions\Action;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Notifications\Notification;
use Filament\Pages\SettingsPage;
use Filament\Schemas\Schema;

class ManageFreeCreditsSignupSettings extends SettingsPage
{
    use HasPageShield;

    protected static string | BackedEnum | null $navigationIcon = 'heroicon-o-banknotes';

    protected static ?string $slug = 'settings/free-credits-signup';

    protected static string $settings = FreeCreditsSignupSettings::class;

    protected static ?string $title = 'Free Credits Signup';

    public function form(Schema $schema): Schema
    {
        return $schema->components([
            Toggle::make('enabled')
                ->label('Enable Free Credits on Signup')
                ->helperText('When enabled, new users will receive free credits upon registration. Disable to stop the campaign.'),

            TextInput::make('amount')
                ->label('Free Credits Amount ($)')
                ->numeric()
                ->required(fn ($get) => $get('enabled'))
                ->helperText('Set the amount of free credits new users receive upon registration.'),
        ]);
    }

    protected function getHeaderActions(): array
    {
        return [
            Action::make('topUpZeroBalance')
                ->label(fn () => 'Top Up $0 Balance Users ($' . (app(FreeCreditsSignupSettings::class)->amount ?? 500) . ' each)')
                ->icon('heroicon-o-currency-dollar')
                ->color('success')
                ->requiresConfirmation()
                ->modalHeading('Top Up Zero Balance Users')
                ->modalDescription('This will add the configured free credits amount to all users who currently have a $0 wallet balance. This action cannot be undone.')
                ->action(function () {
                    $amount = app(FreeCreditsSignupSettings::class)->amount ?? 500;

                    $users = User::whereHas('wallet', fn ($q) => $q->where('total', 0))->get();
                    $count = 0;

                    foreach ($users as $user) {
                        if ($user->wallet) {
                            $user->wallet->increment('total', $amount);
                            $count++;
                        }
                    }

                    Notification::make()
                        ->title("Topped up {$count} users with \${$amount} each")
                        ->success()
                        ->send();
                }),
        ];
    }
}
