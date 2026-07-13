<?php

namespace App\Http\Controllers;

use App\Mail\GenericEmail;
use App\Model\User;
use App\Rules\BannedUsername;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Str;

class ClaimAccountController extends Controller
{
    public function showForm()
    {
        return view('claim-account.form');
    }

    public function submitClaim(Request $request)
    {
        $request->validate([
            'username' => ['required', 'string', 'max:255', new BannedUsername],
            'email' => 'required|email|max:255',
        ]);

        $username = $request->input('username');
        $claimEmail = $request->input('email');

        $user = User::where('username', $username)->first();

        if (!$user) {
            return back()->withErrors(['username' => 'Username not found in migrated accounts.']);
        }

        DB::table('claim_requests')->insert([
            'user_id' => $user->id,
            'username' => $username,
            'claim_email' => $claimEmail,
            'token' => Str::random(60),
            'claimed_at' => now(),
        ]);

        Mail::to('brandon@kkbuddy.com')->send(new GenericEmail([
            'subject' => 'New Account Claim Request - Journalog',
            'mailTitle' => 'Account Claim Request',
            'mailContent' => "A user has requested to claim their old account.<br><br>
                <strong>Username:</strong> {$username}<br>
                <strong>User ID:</strong> {$user->id}<br>
                <strong>Claimant Email:</strong> {$claimEmail}<br><br>
                Login to the admin panel to approve or reject this request.",
            'button' => [
                'url' => url('/admin'),
                'text' => 'Go to Admin Panel',
            ],
        ]));

        return back()->with('success', 'Your claim request has been submitted. You will be contacted at ' . e($claimEmail) . ' once approved.');
    }
}
