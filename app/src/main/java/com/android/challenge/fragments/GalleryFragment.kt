package com.android.challenge.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.android.challenge.adapters.VideoAdapter
import com.android.challenge.business.JellyViewModel
import com.android.challenge.databinding.GalleryLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private lateinit var binding: GalleryLayoutBinding
    private val viewModel: JellyViewModel by viewModels()
    private lateinit var adapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = GalleryLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        lifecycleScope.launch {
            viewModel.getLocalVideos()
            viewModel.local.collectLatest { files ->
                files?.let {
                    adapter.updateVideos(it)
                    binding.noVideosLayout.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(emptyList()) { uri ->
            val dialog = VideoPlayerDialogFragment.newInstance(uri)
            dialog.show(parentFragmentManager, "fullscreen_video")
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@GalleryFragment.adapter
        }
    }
}
