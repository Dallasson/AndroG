package com.android.challenge.business

import com.android.challenge.models.JellyApiResponse
import kotlinx.coroutines.flow.Flow

interface JellyRepository {

    fun getFeed(): Flow<JellyApiResponse>
}
