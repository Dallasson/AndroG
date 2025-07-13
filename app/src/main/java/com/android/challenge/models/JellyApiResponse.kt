package com.android.challenge.models

data class JellyApiResponse(
    val status: String,
    val last_updated: String,
    val feed: List<FeedItem>
)

data class FeedItem(
    val content_type: String,
    val jelly: JellyVideo
)

data class JellyParticipant(
    val userid: String,
    val username: String,
    val full_name: String,
    val avatar_url: String
)

data class JellyVideo(
    val id: String,
    val started_by: String,
    val participants: List<JellyParticipant>,
    val video_url: String,
    val thumbnail_url: String,
    val likes_count: Int,
    val comments_count: Int,
    val all_views: Int,
    val distinct_views: Int,
    val anon_views: Int,
    val tips_total: Double,
    val created_at: String,
    val updated_at: String,
    val posted_at: String
)

