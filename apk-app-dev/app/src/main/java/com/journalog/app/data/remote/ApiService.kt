package com.journalog.app.data.remote

import com.journalog.app.core.network.ApiResponse
import com.journalog.app.data.remote.dto.*
import kotlin.jvm.JvmSuppressWildcards
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<AuthData>>

    @GET("auth/user")
    suspend fun getUser(): Response<ApiResponse<Map<String, UserDto>>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // Feed
    @GET("feed")
    suspend fun getFeed(@Query("page") page: Int = 1): Response<ApiResponse<FeedData>>

    @GET("feed/suggestions")
    suspend fun getSuggestions(): Response<ApiResponse<Map<String, List<UserBriefDto>>>>

    // Posts
    @POST("posts")
    suspend fun createPost(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Map<String, PostDto>>>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Response<ApiResponse<Map<String, PostDto>>>

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @POST("posts/{id}/like")
    suspend fun toggleLike(@Path("id") id: Int): Response<ApiResponse<Map<String, Any>>>

    @POST("posts/{id}/bookmark")
    suspend fun toggleBookmark(@Path("id") id: Int): Response<ApiResponse<Map<String, Boolean>>>

    @GET("posts/{id}/comments")
    suspend fun getComments(@Path("id") id: Int): Response<ApiResponse<CommentsResponse>>

    @POST("posts/{id}/comments")
    suspend fun addComment(@Path("id") id: Int, @Body body: Map<String, String>): Response<ApiResponse<Map<String, CommentDto>>>

    // Users
    @GET("users/{username}")
    suspend fun getProfile(@Path("username") username: String): Response<ApiResponse<Map<String, UserDto>>>

    @GET("users/{username}/posts")
    suspend fun getUserPosts(@Path("username") username: String, @Query("page") page: Int = 1): Response<ApiResponse<FeedData>>

    @POST("users/{username}/follow")
    suspend fun toggleFollow(@Path("username") username: String): Response<ApiResponse<Map<String, Boolean>>>

    // Stories
    @GET("stories/feed")
    suspend fun getStoriesFeed(): Response<ApiResponse<Map<String, List<Any>>>>

    @POST("stories")
    suspend fun createStory(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Map<String, Any>>>

    @POST("stories/{id}/view")
    suspend fun viewStory(@Path("id") id: String): Response<ApiResponse<Unit>>

    // Messenger
    @GET("conversations")
    suspend fun getConversations(): Response<ApiResponse<Map<String, List<ConversationDto>>>>

    @GET("conversations/{userId}/messages")
    suspend fun getMessages(
        @Path("userId") userId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<ApiResponse<MessagesResponse>>

    @POST("messages")
    suspend fun sendMessage(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Map<String, MessageDto>>>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<Map<String, PaginatedNotifications>>>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Unit>>

    // Search
    @GET("search")
    suspend fun search(@Query("q") query: String, @Query("type") type: String = "all"): Response<ApiResponse<Map<String, Any>>>

    @GET("trending")
    suspend fun getTrending(): Response<ApiResponse<Map<String, List<TrendingUserDto>>>>

    // Explore
    @GET("explore/users")
    suspend fun getExploreUsers(@Query("page") page: Int = 1): Response<ApiResponse<ExploreUsersData>>

    // Gifts
    @GET("gifts")
    suspend fun getGifts(): Response<ApiResponse<GiftListData>>

    @POST("gifts/send")
    suspend fun sendGift(@Body body: Map<String, Int>): Response<ApiResponse<SendGiftResponse>>

    @GET("posts/{id}/gifts")
    suspend fun getPostGifts(@Path("id") id: Int): Response<ApiResponse<Map<String, List<PostGiftDto>>>>

    @GET("users/{username}/gift-stats")
    suspend fun getUserGiftStats(@Path("username") username: String): Response<ApiResponse<GiftStatsData>>

    // Polls
    @POST("posts/{id}/poll-vote")
    suspend fun votePoll(@Path("id") id: Int, @Body body: Map<String, Int>): Response<ApiResponse<Map<String, PollDto>>>

    // APK Version
    @GET("apk/version")
    suspend fun checkApkVersion(): Response<ApiResponse<ApkVersionDto>>

    // Settings
    @GET("settings/profile")
    suspend fun getProfileSettings(): Response<ApiResponse<Map<String, UserDto>>>

    @PUT("settings/profile")
    suspend fun updateProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Unit>>

    // Attachment upload
    @Multipart
    @POST("attachment/upload/{type}")
    suspend fun uploadAttachment(
        @Path("type") type: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<ApiResponse<UploadResponse>>

    // Wallet
    @GET("wallet/balance")
    suspend fun getWalletBalance(): Response<ApiResponse<WalletBalance>>

    @POST("wallet/deposit")
    suspend fun initiateDeposit(@Body body: DepositRequest): Response<ApiResponse<DepositResponse>>

    @POST("wallet/withdrawal")
    suspend fun requestWithdrawal(@Body body: WithdrawalRequest): Response<ApiResponse<WithdrawalResponse>>

    @GET("wallet/transactions")
    suspend fun getWalletTransactions(@Query("page") page: Int = 1): Response<ApiResponse<TransactionsData>>

    // Chatbot
    @POST("chatbot/send")
    suspend fun sendChatbotMessage(@Body body: Map<String, String>): Response<ApiResponse<ChatbotResponse>>

    // Subscriptions
    @GET("subscriptions/plans/{username}")
    suspend fun getSubscriptionPlans(@Path("username") username: String): Response<ApiResponse<SubscriptionPlan>>

    @POST("subscriptions/subscribe")
    suspend fun subscribe(@Body body: SubscribeRequest): Response<ApiResponse<SubscriptionData>>

    @POST("subscriptions/cancel")
    suspend fun cancelSubscription(@Body body: Map<String, Int>): Response<ApiResponse<Unit>>

    // Profile settings
    @Multipart
    @POST("settings/profile/upload/{type}")
    suspend fun uploadProfileAsset(
        @Path("type") type: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<ApiResponse<Map<String, String>>>

    // Genders & Countries
    @GET("genders")
    suspend fun getGenders(): Response<ApiResponse<List<GenderOption>>>

    @GET("countries")
    suspend fun getCountries(): Response<ApiResponse<List<CountryOption>>>
}
