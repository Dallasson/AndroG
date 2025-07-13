package com.android.challenge.business

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.challenge.models.JellyApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JellyViewModel @Inject constructor(
    private val repository: JellyRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<JellyApiResponse?>(null)
    val videos: StateFlow<JellyApiResponse?> = _videos.asStateFlow()



    fun loadVideos(limit : Int , page : Int) {
        viewModelScope.launch {
            repository.getFeed(limit,page)
                .catch { e ->
                    _videos.value = null
                }
                .collect { response ->
                    _videos.value = response
                }
        }
    }
}
