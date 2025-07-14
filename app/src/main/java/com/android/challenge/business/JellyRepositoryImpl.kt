package com.android.challenge.business

import com.android.challenge.models.JellyApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class JellyRepositoryImpl @Inject constructor(
    private val api: JellyApi
) : JellyRepository {

    override fun getFeed(limit : Int , page : Int): Flow<JellyApiResponse> = flow {
        val response = api.getFeed(limit,page)
        emit(response)
    }
}
