package com.android.challenge.business

import android.annotation.SuppressLint
import android.os.Environment
import android.os.FileObserver
import com.android.challenge.models.JellyApiResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class JellyRepositoryImpl @Inject constructor(
    private val api: JellyApi
) : JellyRepository {

    private val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Challenge")


    override fun getFeed(limit : Int , page : Int): Flow<JellyApiResponse> = flow {
        val response = api.getFeed(limit,page)
        emit(response)
    }

    @SuppressLint("NewApi")
    override fun getLocalVideos(): Flow<List<File>> = callbackFlow {
        if (!dir.exists() || !dir.isDirectory) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val sendFiles = {
            val files = dir.listFiles()
                ?.filter { it.name.endsWith(".mp4") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            trySend(files)
        }

        val observer = object : FileObserver(dir, CREATE or DELETE or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                if (path?.endsWith(".mp4") == true) sendFiles()
            }
        }

        sendFiles()
        observer.startWatching()

        awaitClose {
            observer.stopWatching()
        }
    }
}
