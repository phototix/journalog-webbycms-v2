package com.journalog.app.core.common

import com.journalog.app.data.remote.dto.PostDto
import com.journalog.app.data.remote.dto.StoryGroupDto

object FeedCache {
    var posts: List<PostDto> = emptyList()
    var likedPosts: Map<Int, Boolean> = emptyMap()
    var stories: List<StoryGroupDto> = emptyList()
    var lastRefreshed: Long = 0L
    var refreshTrigger: Int = 0

    const val STALE_MS = 5 * 60 * 1000L

    val isStale: Boolean
        get() = System.currentTimeMillis() - lastRefreshed > STALE_MS
}
