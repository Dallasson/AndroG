package com.android.challenge.business

import com.android.challenge.models.JellyApiResponse
import kotlinx.coroutines.flow.Flow
import java.io.File

interface JellyRepository {

    fun getFeed(limit : Int , page : Int): Flow<JellyApiResponse>
    fun getLocalVideos(): Flow<List<File>>
}
