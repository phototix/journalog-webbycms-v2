<?php

namespace App\Providers;

use Illuminate\Support\Arr;
use Laravel\Socialite\Two\AbstractProvider;
use Laravel\Socialite\Two\ProviderInterface;
use Laravel\Socialite\Two\User;
use GuzzleHttp\RequestOptions;

class WebbyCloudProvider extends AbstractProvider implements ProviderInterface
{
    protected string $authUrl;
    protected string $tokenUrl;
    protected string $userInfoUrl;

    protected $scopeSeparator = ' ';

    public function __construct($request, $clientId, $clientSecret, $redirectUrl)
    {
        parent::__construct($request, $clientId, $clientSecret, $redirectUrl);

        $config = config('services.webbycloud');
        $this->authUrl = $config['auth_url'];
        $this->tokenUrl = $config['token_url'];
        $this->userInfoUrl = $config['user_info_url'];
        $this->scopes = $config['scopes'];
    }

    protected function getAuthUrl($state): string
    {
        return $this->buildAuthUrlFromBase($this->authUrl, $state);
    }

    protected function getTokenUrl(): string
    {
        return $this->tokenUrl;
    }

    protected function getUserByToken($token): array
    {
        $response = $this->getHttpClient()->get($this->userInfoUrl, [
            RequestOptions::HEADERS => [
                'Authorization' => 'Bearer ' . $token,
                'OCS-APIRequest' => 'true',
                'Accept' => 'application/json',
            ],
        ]);

        $body = json_decode((string) $response->getBody(), true);

        return Arr::get($body, 'ocs.data', []);
    }

    protected function mapUserToObject(array $user): User
    {
        return (new User)->setRaw($user)->map([
            'id' => $user['id'] ?? null,
            'name' => $user['display-name'] ?? $user['id'] ?? null,
            'email' => $user['email'] ?? null,
        ]);
    }

    protected function getTokenFields($code): array
    {
        return array_merge(parent::getTokenFields($code), [
            'grant_type' => 'authorization_code',
        ]);
    }
}
