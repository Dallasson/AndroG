package com.android.challenge.adapters

import com.android.challenge.models.JellyVideo



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.R
import com.bumptech.glide.Glide

class FeedVideoAdapter(
    private val videos: List<JellyVideo>
) : RecyclerView.Adapter<FeedVideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.title.text = video.title

        Glide.with(holder.thumbnail.context)
            .load(video.thumbnailUrl)
            .into(holder.thumbnail)
    }

    override fun getItemCount(): Int = videos.size

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnailImage)
        val title: TextView = view.findViewById(R.id.titleText)
    }
}
