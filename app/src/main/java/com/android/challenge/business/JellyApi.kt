package com.android.challenge.business

import com.android.challenge.models.JellyApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JellyApi {
    @GET("feed")
    suspend fun getFeed(
        @Query("limit") limit: Int,
        @Query("page") page: Int
    ): JellyApiResponse
}
