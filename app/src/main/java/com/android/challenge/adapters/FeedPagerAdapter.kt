
package com.android.challenge.adapters

import android.content.*
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.R
import com.android.challenge.databinding.FeedItemLayoutBinding
import com.android.challenge.models.FeedItem
import androidx.core.net.toUri

class FeedPagerAdapter(
    private val context: Context,
    private val sharedPlayer: ExoPlayer
) : RecyclerView.Adapter<FeedPagerAdapter.FeedViewHolder>() {

    private val items = mutableListOf<FeedItem>()
    private var currentBinding: FeedItemLayoutBinding? = null
    private var recyclerView: RecyclerView? = null

    fun setItems(newItems: List<FeedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun bindPlayerTo(index: Int) {
        val vh = recyclerView?.findViewHolderForAdapterPosition(index) as? FeedViewHolder ?: return

        currentBinding?.playerView?.player = null
        currentBinding = vh.binding

        currentBinding?.apply {
            playerView.useController = false
            playerView.player = sharedPlayer
        }

        val item = items[index]
        sharedPlayer.setMediaItem(MediaItem.fromUri(item.jelly.video_url))
        sharedPlayer.prepare()
        sharedPlayer.playWhenReady = true
    }

    fun pauseCurrentPlayer() {
        sharedPlayer.playWhenReady = false
    }

    fun releasePlayer() {
        sharedPlayer.release()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        if (recyclerView == null && parent is RecyclerView) {
            recyclerView = parent
        }
        val binding = FeedItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FeedViewHolder(val binding: FeedItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeedItem) {
            "${item.jelly.all_views} views".also { binding.viewsText.text = it }
            "@${item.jelly.participants.firstOrNull()?.username ?: "Unknown"}".also { binding.username.text = it }

            binding.btnVolume.setOnClickListener {
                val isMuted = sharedPlayer.volume == 0f
                sharedPlayer.volume = if (isMuted) 1f else 0f
                binding.btnVolume.setImageResource(if (isMuted) R.drawable.sound_on else R.drawable.sound_off)
            }

            binding.btnCopyLink.setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Video URL", item.jelly.video_url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
            }

            binding.btnX.setOnClickListener {
                val url = item.jelly.video_url
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://twitter.com/intent/tweet?url=${Uri.encode(url)}".toUri())
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Twitter not installed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
