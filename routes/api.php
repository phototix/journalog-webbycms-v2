<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| is assigned the "api" middleware group. Enjoy building your API!
|
*/

Route::prefix('v1')->group(function () {

    // Public auth routes
    Route::post('/auth/register', [\App\Http\Controllers\Api\AuthController::class, 'register']);
    Route::post('/auth/login', [\App\Http\Controllers\Api\AuthController::class, 'login']);

    // Public search & trending
    Route::get('/search', [\App\Http\Controllers\Api\SearchController::class, 'search']);
    Route::get('/trending', [\App\Http\Controllers\Api\SearchController::class, 'trending']);

    // Public APK version check
    Route::get('/apk/version', [\App\Http\Controllers\Api\ApkController::class, 'version']);

    // Public explore (creators with public profiles)
    Route::get('/explore/users', [\App\Http\Controllers\Api\ExploreController::class, 'users']);

    // Public gifts
    Route::get('/gifts', [\App\Http\Controllers\Api\GiftController::class, 'listGifts']);
    Route::get('/posts/{id}/gifts', [\App\Http\Controllers\Api\GiftController::class, 'postGifts']);
    Route::get('/users/{username}/gift-stats', [\App\Http\Controllers\Api\GiftController::class, 'userGiftStats']);

    // Authenticated routes
    Route::middleware('auth:sanctum')->group(function () {

        // Auth
        Route::get('/auth/user', [\App\Http\Controllers\Api\AuthController::class, 'user']);
        Route::post('/auth/logout', [\App\Http\Controllers\Api\AuthController::class, 'logout']);

        // Feed
        Route::get('/feed', [\App\Http\Controllers\Api\FeedController::class, 'index']);
        Route::get('/feed/suggestions', [\App\Http\Controllers\Api\FeedController::class, 'suggestions']);

        // Posts
        Route::get('/posts/{id}', [\App\Http\Controllers\Api\PostController::class, 'show']);
        Route::post('/posts', [\App\Http\Controllers\Api\PostController::class, 'store']);
        Route::delete('/posts/{id}', [\App\Http\Controllers\Api\PostController::class, 'destroy']);
        Route::post('/posts/{id}/like', [\App\Http\Controllers\Api\PostController::class, 'like']);
        Route::post('/posts/{id}/bookmark', [\App\Http\Controllers\Api\PostController::class, 'bookmark']);
        Route::get('/posts/{id}/comments', [\App\Http\Controllers\Api\PostController::class, 'comments']);
        Route::post('/posts/{id}/comments', [\App\Http\Controllers\Api\PostController::class, 'addComment']);
        Route::delete('/comments/{id}', [\App\Http\Controllers\Api\PostController::class, 'deleteComment']);
        Route::post('/posts/{id}/poll-vote', [\App\Http\Controllers\Api\PostController::class, 'votePoll']);

        // Gifts (auth-only actions)
        Route::post('/gifts/send', [\App\Http\Controllers\Api\GiftController::class, 'sendGift']);

        // Users
        Route::get('/users/{username}', [\App\Http\Controllers\Api\UserController::class, 'profile']);
        Route::get('/users/{username}/posts', [\App\Http\Controllers\Api\UserController::class, 'userPosts']);
        Route::get('/users/{username}/followers', [\App\Http\Controllers\Api\UserController::class, 'followers']);
        Route::post('/users/{username}/follow', [\App\Http\Controllers\Api\UserController::class, 'follow']);

        // Stories
        Route::get('/stories/feed', [\App\Http\Controllers\Api\StoryController::class, 'feed']);
        Route::post('/stories', [\App\Http\Controllers\Api\StoryController::class, 'store']);
        Route::post('/stories/{id}/view', [\App\Http\Controllers\Api\StoryController::class, 'view']);
        Route::delete('/stories/{id}', [\App\Http\Controllers\Api\StoryController::class, 'destroy']);

        // Messenger
        Route::get('/conversations', [\App\Http\Controllers\Api\MessengerController::class, 'conversations']);
        Route::get('/conversations/{userId}/messages', [\App\Http\Controllers\Api\MessengerController::class, 'messages']);
        Route::post('/messages', [\App\Http\Controllers\Api\MessengerController::class, 'sendMessage']);

        // Notifications
        Route::get('/notifications', [\App\Http\Controllers\Api\NotificationController::class, 'index']);
        Route::post('/notifications/{id}/read', [\App\Http\Controllers\Api\NotificationController::class, 'markRead']);
        Route::post('/notifications/read-all', [\App\Http\Controllers\Api\NotificationController::class, 'markAllRead']);

        // Settings
        Route::get('/settings/profile', [\App\Http\Controllers\Api\SettingsController::class, 'profile']);
        Route::put('/settings/profile', [\App\Http\Controllers\Api\SettingsController::class, 'updateProfile']);
        Route::put('/settings/password', [\App\Http\Controllers\Api\SettingsController::class, 'updatePassword']);

    });
});
