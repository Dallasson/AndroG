package com.android.challenge.business

import com.android.challenge.models.JellyApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class JellyRepositoryImpl @Inject constructor(
    private val api: JellyApi
) : JellyRepository {

    override fun getFeed(): Flow<JellyApiResponse> = flow {
        val response = api.getFeed()
        emit(response) // assuming JellyApiResponse.videos exists
    }
}
