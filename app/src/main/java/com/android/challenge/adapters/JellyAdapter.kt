package com.android.challenge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.R
import com.android.challenge.models.FeedItem
import com.android.challenge.models.JellyApiResponse

class JellyAdapter(response: JellyApiResponse) :
    RecyclerView.Adapter<JellyAdapter.VideoViewHolder>() {

    private val feedItems = response.feed

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val playerView: PlayerView = view.findViewById(R.id.playerView)
        private val title: TextView = view.findViewById(R.id.title)
        private val viewsText: TextView = view.findViewById(R.id.viewsText)
        private val username: TextView = view.findViewById(R.id.username)
        private val handle: TextView = view.findViewById(R.id.handle)
        private val transcript: TextView = view.findViewById(R.id.transcript)

        private var player: ExoPlayer? = null

        fun bind(item: FeedItem) {
            val video = item.jelly

            // Release any previous player
            player?.release()

            // Set up new player
            player = ExoPlayer.Builder(playerView.context).build().also { exoPlayer ->
                playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(video.video_url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.volume = 0f
                exoPlayer.playWhenReady = true
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.prepare()
            }

            // Set UI content
            val participant = video.participants.firstOrNull()
            title.text = "@" + (participant?.username ?: "unknown")
            username.text = participant?.full_name ?: "Unknown User"
            handle.text = "@" + (participant?.username ?: "")
            transcript.text = "Video ID: ${video.id}" // Replace with actual transcript if available
            viewsText.text = "${video.all_views} views"
        }

        fun release() {
            player?.release()
            player = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(feedItems[position])
    }

    override fun getItemCount(): Int = feedItems.size

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.release()
    }
}

