package com.android.challenge.fragments

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.challenge.R
import com.android.challenge.adapters.VideoAdapter
import com.android.challenge.databinding.GalleryLayoutBinding
import java.io.File

class GalleryFragment : Fragment() {

    private val videoList = mutableListOf<File>()
    private lateinit var binding: GalleryLayoutBinding
    private var fileObserver: FileObserver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = GalleryLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        loadVideos()
        setupRecyclerView()
        startObserving()
    }

    private fun setupRecyclerView() {
        if (videoList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.noVideosLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerView.adapter = VideoAdapter(videoList) { uri ->
                val dialog = VideoPlayerDialogFragment.newInstance(uri)
                dialog.show(parentFragmentManager, "fullscreen_video")
            }
            binding.recyclerView.visibility = View.VISIBLE
            binding.noVideosLayout.visibility = View.GONE
        }
    }

    private fun loadVideos() {
        val dir = File(Environment.getExternalStorageDirectory(), "Challenge")
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { it.name.endsWith(".mp4") }
                ?.sortedByDescending { it.lastModified() }
                ?.let {
                    videoList.clear()
                    videoList.addAll(it)
                }
        }
    }

    private fun startObserving() {
        val dirPath = File(Environment.getExternalStorageDirectory(), "Challenge").absolutePath

        fileObserver = object : FileObserver(dirPath, CREATE or DELETE or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && path.endsWith(".mp4")) {
                    requireActivity().runOnUiThread {
                        loadVideos()
                        setupRecyclerView()
                        binding.recyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        fileObserver?.startWatching()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fileObserver?.stopWatching()
        fileObserver = null
    }

    class VideoPlayerDialogFragment : androidx.fragment.app.DialogFragment() {

        companion object {
            private const val ARG_URI = "video_uri"

            fun newInstance(uri: Uri): VideoPlayerDialogFragment {
                val fragment = VideoPlayerDialogFragment()
                val args = Bundle()
                args.putParcelable(ARG_URI, uri)
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.dialog_fullscreen_video, container, false)
            val videoView = view.findViewById<VideoView>(R.id.fullscreenVideoView)
            val uri = arguments?.getParcelable<Uri>(ARG_URI)
            uri?.let {
                videoView.setVideoURI(it)
                videoView.setOnPreparedListener { player ->
                    player.isLooping = true
                    player.start()
                }
            }
            view.setOnClickListener { dismiss() }
            return view
        }

        override fun onStart() {
            super.onStart()
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}
