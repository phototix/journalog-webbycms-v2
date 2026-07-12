<?php

namespace App\Http\Controllers\Api;

use App\Model\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Auth;
use Illuminate\Validation\ValidationException;

class AuthController extends ApiController
{
    public function register(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'username' => 'required|string|max:255|unique:users,username',
            'email' => 'required|string|email|max:255|unique:users,email',
            'password' => 'required|string|min:8',
            'birthdate' => 'required|date',
        ]);

        $user = User::create([
            'name' => $validated['name'],
            'username' => $validated['username'],
            'email' => $validated['email'],
            'password' => Hash::make($validated['password']),
            'birthdate' => $validated['birthdate'],
            'role_id' => 2,
        ]);

        $token = $user->createToken('api-token')->plainTextToken;

        return $this->success([
            'user' => $this->formatUser($user),
            'token' => $token,
        ], 'Registration successful', 201);
    }

    public function login(Request $request)
    {
        $validated = $request->validate([
            'email' => 'required|string|email',
            'password' => 'required|string',
        ]);

        if (!Auth::attempt(['email' => $validated['email'], 'password' => $validated['password']])) {
            throw ValidationException::withMessages([
                'email' => ['The provided credentials are incorrect.'],
            ]);
        }

        $user = Auth::user();
        $token = $user->createToken('api-token')->plainTextToken;

        return $this->success([
            'user' => $this->formatUser($user),
            'token' => $token,
        ], 'Login successful');
    }

    public function user(Request $request)
    {
        return $this->success([
            'user' => $this->formatUser($request->user()),
        ]);
    }

    public function logout(Request $request)
    {
        $request->user()->currentAccessToken()->delete();

        return $this->success(null, 'Logged out successfully');
    }

    private function formatUser($user): array
    {
        return [
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
            'email_verified_at' => $user->email_verified_at,
            'created_at' => $user->created_at,
        ];
    }
}
