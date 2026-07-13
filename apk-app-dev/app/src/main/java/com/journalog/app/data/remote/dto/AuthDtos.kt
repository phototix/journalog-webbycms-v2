package com.journalog.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val birthdate: String
)

data class UserDto(
    val id: Int,
    val name: String,
    val username: String,
    val email: String?,
    val bio: String?,
    val avatar: String?,
    val cover: String?,
    val location: String?,
    val website: String?,
    @SerializedName("paid_profile") val paidProfile: Boolean,
    @SerializedName("profile_access_price") val profileAccessPrice: Double,
    @SerializedName("profile_access_price_3_months") val profileAccessPrice3Months: Double?,
    @SerializedName("profile_access_price_6_months") val profileAccessPrice6Months: Double?,
    @SerializedName("profile_access_price_12_months") val profileAccessPrice12Months: Double?,
    @SerializedName("public_profile") val publicProfile: Boolean,
    @SerializedName("open_profile") val openProfile: Boolean,
    @SerializedName("email_verified_at") val emailVerifiedAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("posts_count") val postsCount: Int?,
    @SerializedName("subscribers_count") val subscribersCount: Int?,
    @SerializedName("is_online") val isOnline: Boolean?,
    @SerializedName("is_verified") val isVerified: Boolean?,
    @SerializedName("has_subscribed") val hasSubscribed: Boolean?,
    @SerializedName("has_chat_access") val hasChatAccess: Boolean?,
    @SerializedName("role_id") val roleId: Int?,
    @SerializedName("is_following") val isFollowing: Boolean?,
    @SerializedName("gifts_received_count") val giftsReceivedCount: Int?,
    @SerializedName("gifts_received_credits") val giftsReceivedCredits: Int?,
)

data class AuthData(
    val user: UserDto,
    val token: String
)

data class PostDto(
    val id: Int,
    val text: String?,
    val price: Double,
    val status: Int?,
    @SerializedName("is_pinned") val isPinned: Boolean?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("expire_date") val expireDate: String?,
    @SerializedName("created_at") val createdAt: String?,
    val user: UserBriefDto?,
    val media: List<MediaDto>?,
    val poll: PollDto?,
    val gifts: List<PostGiftDto>?,
    @SerializedName("gifts_count") val giftsCount: Int?,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("comments_count") val commentsCount: Int,
    @SerializedName("has_liked") val hasLiked: Boolean,
    @SerializedName("has_bookmarked") val hasBookmarked: Boolean?,
)

data class UserBriefDto(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: String?
)

data class MediaDto(
    val id: String,
    val type: String,
    val url: String,
    val thumbnail: String?,
    val width: Int?,
    val height: Int?
)

data class FeedData(
    val posts: List<PostDto>,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_page") val nextPage: Int?
)

data class StoryGroupDto(
    val user: UserBriefDto,
    @SerializedName("has_unseen") val hasUnseen: Boolean,
    val stories: List<StoryItemDto>
)

data class StoryItemDto(
    val id: String,
    val type: String,
    val url: String,
    val thumbnail: String?,
    val text: String?,
    val length: Int?,
    val overlay: Map<String, Double>?,
    @SerializedName("bg_preset") val bgPreset: String?,
    val rawTime: Long?,
    @SerializedName("created_at") val createdAt: String?
)

data class CommentDto(
    val id: Int,
    val text: String,
    @SerializedName("created_at") val createdAt: String?,
    val user: UserBriefDto
)

data class ConversationDto(
    @SerializedName("contact_id") val contactId: Int,
    val name: String,
    val avatar: String,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_date") val lastMessageDate: String?,
    @SerializedName("unread_count") val unreadCount: Int
)

data class MessageDto(
    val id: Int,
    val text: String?,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("is_mine") val isMine: Boolean,
    @SerializedName("is_seen") val isSeen: Boolean?,
    val price: Double?,
    @SerializedName("created_at") val createdAt: String?,
    val attachments: List<MediaDto>?
)

data class NotificationDto(
    val id: String,
    val type: String,
    val message: String?,
    val read: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    val actor: UserBriefDto?
)

data class SearchResultDto(
    val id: Int,
    val name: String?,
    val username: String?,
    val avatar: String?,
    val bio: String?,
    val text: String?,
    val thumbnail: String?,
    val type: String
)

data class TrendingUserDto(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: String?,
    val bio: String?,
    @SerializedName("subscribers_count") val subscribersCount: Int
)

data class ApkVersionDto(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("changelog") val changelog: String?
)

data class GiftDto(
    val id: Int,
    val name: String,
    val icon: String,
    @SerializedName("gif_effect") val gifEffect: String?,
    val credits: Int,
    val category: String,
    @SerializedName("sort_order") val sortOrder: Int
)

data class PostGiftDto(
    @SerializedName("gift_id") val giftId: Int,
    val count: Int,
    val gift: GiftDto?
)

data class GiftListData(
    val gifts: Map<String, List<GiftDto>>,
    val balance: Double
)

data class SendGiftResponse(
    val balance: Double,
    @SerializedName("post_gifts") val postGifts: List<PostGiftDto>?,
    val gift: GiftDto?
)

data class PollDto(
    val id: Int,
    val answers: List<PollAnswerDto>?,
    @SerializedName("total_votes") val totalVotes: Int
)

data class PollAnswerDto(
    val id: Int,
    val answer: String,
    @SerializedName("votes_count") val votesCount: Int,
    val percentage: Int
)

data class ExploreData(
    val posts: List<PostDto>,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_page") val nextPage: Int?
)

data class GiftStatsData(
    val gifts: List<PostGiftDto>?,
    @SerializedName("total_gifts") val totalGifts: Int,
    @SerializedName("total_credits") val totalCredits: Int
)

data class ExploreUserDto(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: String?,
    val bio: String?,
    @SerializedName("last_posted_at") val lastPostedAt: String?
)

data class ExploreUsersData(
    val users: List<ExploreUserDto>,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_page") val nextPage: Int?
)

data class PaginatedMessages(
    val data: List<MessageDto>
)

data class PaginatedNotifications(
    val data: List<NotificationDto>
)

data class UploadResponse(
    @SerializedName("attachmentID") val attachmentID: String,
    val path: String,
    val type: String,
    val thumbnail: String?
)

data class SubscriptionPlan(
    val price: Double,
    @SerializedName("price_3_months") val price3Months: Double,
    @SerializedName("price_6_months") val price6Months: Double,
    @SerializedName("price_12_months") val price12Months: Double,
    @SerializedName("has_subscribed") val hasSubscribed: Boolean
)

data class SubscribeRequest(
    @SerializedName("recipient_user_id") val recipientUserId: Int,
    val plan: String
)

data class SubscriptionData(
    val id: Int?,
    val status: String?,
    @SerializedName("expires_at") val expiresAt: String?
)

data class WalletBalance(
    val total: Double,
    @SerializedName("pendingBalance") val pendingBalance: Double?
)
