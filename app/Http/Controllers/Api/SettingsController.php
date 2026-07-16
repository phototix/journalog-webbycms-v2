<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\SettingsController as WebSettingsController;
use App\Model\Country;
use App\Model\UserGender;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rule;

class SettingsController extends ApiController
{
    public function profile(Request $request)
    {
        $user = $request->user();

        return $this->success([
            'user' => [
                'id' => $user->id,
                'name' => $user->name,
                'username' => $user->username,
                'email' => $user->email,
                'bio' => $user->bio,
                'avatar' => $user->avatar,
                'cover' => $user->cover,
                'location' => $user->location,
                'website' => $user->website,
                'birthdate' => $user->birthdate,
                'gender_pronoun' => $user->gender_pronoun,
                'gender_id' => $user->gender_id,
                'country_id' => $user->country_id,
                'paid_profile' => (bool) $user->paid_profile,
                'profile_access_price' => (float) $user->profile_access_price,
                'public_profile' => (bool) $user->public_profile,
                'open_profile' => (bool) $user->open_profile,
            ],
        ]);
    }

    public function updateProfile(Request $request)
    {
        $user = $request->user();

        $validated = $request->validate([
            'name' => 'sometimes|string|max:255',
            'bio' => 'nullable|string|max:2000',
            'location' => 'nullable|string|max:255',
            'website' => 'nullable|url|max:255',
            'birthdate' => 'nullable|date',
            'gender_id' => 'nullable|exists:user_genders,id',
            'gender_pronoun' => 'nullable|string|max:50',
            'country_id' => 'nullable|exists:countries,id',
            'profile_access_price' => 'nullable|numeric|min:0',
            'profile_access_price_3_months' => 'nullable|numeric|min:0',
            'profile_access_price_6_months' => 'nullable|numeric|min:0',
            'profile_access_price_12_months' => 'nullable|numeric|min:0',
            'paid_profile' => 'nullable|boolean',
            'public_profile' => 'nullable|boolean',
            'open_profile' => 'nullable|boolean',
        ]);

        $user->update($validated);

        return $this->success(null, 'Profile updated');
    }

    public function updatePassword(Request $request)
    {
        $validated = $request->validate([
            'current_password' => 'required|string',
            'new_password' => 'required|string|min:8|confirmed',
        ]);

        $user = $request->user();

        if (!Hash::check($validated['current_password'], $user->password)) {
            return $this->error('Current password is incorrect', 422);
        }

        $user->update([
            'password' => Hash::make($validated['new_password']),
        ]);

        return $this->success(null, 'Password updated');
    }

    public function uploadProfileAsset(Request $request, $type)
    {
        $request->route()->setParameter('uploadType', $type);
        $controller = new WebSettingsController();
        $result = $controller->uploadProfileAsset($request);

        $decoded = json_decode($result->getContent(), true);

        if (isset($decoded['success']) && $decoded['success']) {
            return $this->success([
                'assetSrc' => $decoded['assetSrc'],
            ], 'Upload successful');
        }

        return $this->error($decoded['message'] ?? 'Upload failed', 400);
    }

    public function genders()
    {
        $genders = UserGender::all(['id', 'gender_name']);
        return $this->success($genders);
    }

    public function countries()
    {
        $countries = Country::all(['id', 'name']);
        return $this->success($countries);
    }
}
