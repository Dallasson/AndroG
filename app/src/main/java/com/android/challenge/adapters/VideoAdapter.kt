package com.android.challenge.adapters

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.R
import java.io.File

class VideoAdapter(
    private val videos: List<File>,
    private val onClick: (uri: android.net.Uri) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val label: TextView = view.findViewById(R.id.videoLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoFile = videos[position]
        holder.label.text = videoFile.name

        val thumbnail: Bitmap? = ThumbnailUtils.createVideoThumbnail(
            videoFile.absolutePath,
            MediaStore.Video.Thumbnails.MINI_KIND
        )
        holder.thumbnail.setImageBitmap(thumbnail)

        val uri = FileProvider.getUriForFile(
            holder.itemView.context,
            "${holder.itemView.context.packageName}.provider",
            videoFile
        )

        holder.thumbnail.setOnClickListener {
            onClick(uri)
        }
    }

    override fun getItemCount(): Int = videos.size
}
