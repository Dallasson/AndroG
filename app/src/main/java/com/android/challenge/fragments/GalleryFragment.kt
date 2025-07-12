package com.android.challenge.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.challenge.R
import com.android.challenge.databinding.GalleryLayoutBinding
import java.io.File

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val videoList = mutableListOf<File>()
    private lateinit var binding : GalleryLayoutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = GalleryLayoutBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        loadVideos()
        recyclerView.adapter = VideoAdapter(videoList)

    }

    private fun loadVideos() {
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        dir?.listFiles()?.filter { it.name.endsWith(".mp4") }?.sortedByDescending { it.lastModified() }?.let {
            videoList.clear()
            videoList.addAll(it)
        }
    }

    inner class VideoAdapter(private val videos: List<File>) :
        RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

        inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val videoView: VideoView = view.findViewById(R.id.videoItem)
            val label: TextView = view.findViewById(R.id.videoLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video, parent, false)
            return VideoViewHolder(view)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val videoFile = videos[position]
            holder.label.text = videoFile.name

            val uri = Uri.fromFile(videoFile)
            holder.videoView.setVideoURI(uri)

            val controller = MediaController(requireContext())
            controller.setAnchorView(holder.videoView)
            holder.videoView.setMediaController(controller)

            holder.videoView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = videos.size
    }
}