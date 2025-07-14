package com.android.challenge.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.android.challenge.R

class VideoPlayerDialogFragment : DialogFragment() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}