<?php

namespace App\Http\Controllers\Api;

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
                'paid_profile' => (bool) $user->paid_profile,
                'profile_access_price' => (float) $user->profile_access_price,
                'public_profile' => (bool) $user->public_profile,
                'open_profile' => (bool) $user->open_profile,
                'first_name' => $user->first_name,
                'last_name' => $user->last_name,
                'billing_address' => $user->billing_address,
                'city' => $user->city,
                'country' => $user->country,
                'state' => $user->state,
                'postcode' => $user->postcode,
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
            'gender_pronoun' => 'nullable|string|max:50',
            'profile_access_price' => 'nullable|numeric|min:0',
            'paid_profile' => 'nullable|boolean',
            'public_profile' => 'nullable|boolean',
            'open_profile' => 'nullable|boolean',
            'first_name' => 'nullable|string|max:255',
            'last_name' => 'nullable|string|max:255',
            'billing_address' => 'nullable|string|max:500',
            'city' => 'nullable|string|max:255',
            'country' => 'nullable|string|max:255',
            'state' => 'nullable|string|max:255',
            'postcode' => 'nullable|string|max:20',
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
}
