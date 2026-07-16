<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\SettingsController as WebSettingsController;
use App\Model\Country;
use App\Model\UserGender;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;
use Intervention\Image\Facades\Image;
use Ramsey\Uuid\Uuid;

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
        $request->validate([
            'file' => 'required|file|mimes:jpg,jpeg,png,gif|max:' . (getSetting('media.max_avatar_cover_file_size') ? (int) getSetting('media.max_avatar_cover_file_size') * 1000 : 4000),
        ]);

        $file = $request->file('file');
        if (!$file) {
            return $this->error('No file uploaded', 400);
        }

        try {
            $directory = 'users/' . $type;
            $fileId = Uuid::uuid4()->getHex();
            $filePath = $directory . '/' . $fileId . '.jpg';

            $img = Image::make($file);

            if ($type == 'cover') {
                $img->fit(599, 180)->orientate();
                $data = ['cover' => $filePath];
            } else {
                $img->fit(96, 96)->orientate();
                $data = ['avatar' => $filePath];
            }

            $img->encode('jpg', 100);
            $encoded = (string) $img;
            Storage::disk('public')->put($filePath, $encoded);
            Storage::disk(config('filesystems.defaultFilesystemDriver'))->put($filePath, $encoded, 'public');
            Storage::disk('public')->delete($filePath);
            $request->user()->update($data);
            $assetPath = \App\Providers\GenericHelperServiceProvider::getStorageAvatarPath($filePath);
            if ($type == 'cover') {
                $assetPath = \App\Providers\GenericHelperServiceProvider::getStorageCoverPath($filePath);
            }

            return $this->success(['assetSrc' => $assetPath], 'Upload successful');
        } catch (\Exception $e) {
            return $this->error($e->getMessage(), 500);
        }
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
