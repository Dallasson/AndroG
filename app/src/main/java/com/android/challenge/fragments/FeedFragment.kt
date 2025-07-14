
package com.android.challenge.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.adapters.FeedPagerAdapter
import com.android.challenge.business.JellyViewModel
import com.android.challenge.databinding.FeedLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeedFragment : Fragment(), LifecycleEventObserver {

    private lateinit var binding: FeedLayoutBinding
    private val viewModel: JellyViewModel by viewModels()
    private lateinit var adapter: FeedPagerAdapter
    private lateinit var sharedPlayer: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FeedLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(this)

        sharedPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
        }

        adapter = FeedPagerAdapter(requireContext(), sharedPlayer)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter
        PagerSnapHelper().attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = rv.layoutManager as? LinearLayoutManager
                    val visiblePosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: -1
                    if (visiblePosition != RecyclerView.NO_POSITION) {
                        adapter.bindPlayerTo(visiblePosition)
                    }
                }
            }
        })

        lifecycleScope.launch {
            viewModel.loadVideos(limit = 52, page = 1)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { response ->
                    response?.let {
                        val shuffledFeed = it.feed.shuffled()
                        if (shuffledFeed.isEmpty()) {
                            showEmptyView(true)
                        } else {
                            showEmptyView(false)
                            adapter.setItems(shuffledFeed)
                            binding.recyclerView.post {
                                adapter.bindPlayerTo(0)
                            }
                        }
                    } ?: showEmptyView(true)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        resumeCurrentVideo()
    }

    override fun onStop() {
        super.onStop()
        pauseCurrentVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycle.removeObserver(this)
        adapter.releasePlayer()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> pauseCurrentVideo()
            Lifecycle.Event.ON_START -> resumeCurrentVideo()
            else -> {}
        }
    }

    fun pauseCurrentVideo() {
        adapter.pauseCurrentPlayer()
    }

    fun resumeCurrentVideo() {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
        val position = layoutManager?.findFirstCompletelyVisibleItemPosition()
            ?: layoutManager?.findFirstVisibleItemPosition()
        if (position != null && position != RecyclerView.NO_POSITION) {
            adapter.bindPlayerTo(position)
        }
    }

    private fun showEmptyView(show: Boolean) {
        binding.emptyView.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
}

