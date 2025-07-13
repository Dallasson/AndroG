package com.android.challenge.fragments

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper.prepare
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.challenge.R
import com.android.challenge.business.JellyViewModel
import com.android.challenge.databinding.FeedLayoutBinding
import com.android.challenge.models.FeedItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class FeedFragment : Fragment() {

    private lateinit var binding: FeedLayoutBinding
    private val viewModel: JellyViewModel by viewModels()
    private var currentIndex = 0
    private var player: ExoPlayer? = null
    private var feedList: List<FeedItem> = emptyList()
    private var isSoundOn = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FeedLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(requireContext()).build().also {
            binding.playerView.player = it
            binding.playerView.useController = false
        }

        // Navigation buttons
        binding.btnNext.setOnClickListener {
            if (currentIndex < feedList.lastIndex) {
                currentIndex++
                bindVideo()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                bindVideo()
            }
        }

        // 🔇 Volume toggle
        binding.btnVolume.setOnClickListener {
            isSoundOn = !isSoundOn
            player?.volume = if (isSoundOn) 1f else 0f
            binding.btnVolume.setImageResource(
                if (isSoundOn) R.drawable.sound_on else R.drawable.sound_off
            )
        }

        // 🔗 Copy link
        binding.btnCopyLink.setOnClickListener {
            val url = feedList.getOrNull(currentIndex)?.jelly?.video_url ?: return@setOnClickListener
            copyToClipboard(url)
            Toast.makeText(requireContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // ❌ Twitter share
        binding.btnX.setOnClickListener {
            val url = feedList.getOrNull(currentIndex)?.jelly?.video_url ?: return@setOnClickListener
            copyToClipboard(url)

            val tweetIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://twitter.com/intent/tweet?url=${Uri.encode(url)}")
            }

            try {
                startActivity(tweetIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Twitter not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Load videos
        lifecycleScope.launch {
            viewModel.loadVideos(limit = 52, page = 1)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { response ->
                    response?.let {
                        feedList = it.feed
                        bindVideo()
                    }
                }
            }
        }
    }

    private fun bindVideo() {
        val item = feedList[currentIndex].jelly
        val participant = item.participants.firstOrNull()

        val mediaItem = MediaItem.fromUri(item.video_url)
        player?.apply {
            setMediaItem(mediaItem)
            volume = if (isSoundOn) 1f else 0f
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }

        binding.viewsText.text = "${item.all_views} views"

        binding.btnBack.visibility = if (currentIndex == 0) View.INVISIBLE else View.VISIBLE
        binding.btnNext.visibility = if (currentIndex == feedList.lastIndex) View.INVISIBLE else View.VISIBLE
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Video URL", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
    }
}
