package com.android.challenge.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.challenge.adapters.FeedVideoAdapter
import com.android.challenge.databinding.FeedLayoutBinding
import com.android.challenge.models.JellyVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


class FeedFragment : Fragment() {

    private lateinit var binding: FeedLayoutBinding
    private val videoList = mutableListOf<JellyVideo>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FeedLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        scrapeFeed()
    }

    private fun scrapeFeed() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect("https://jellyjelly.com/feed").get()

                val videos = doc.select("div.video-card") // adjust based on real structure
                for (element in videos) {
                    val title = element.selectFirst("h3")?.text() ?: "Untitled"
                    val videoUrl = element.selectFirst("video source")?.attr("src") ?: ""
                    val thumb = element.selectFirst("img")?.attr("src") ?: ""

                    if (videoUrl.isNotEmpty()) {
                        videoList.add(JellyVideo(title, videoUrl, thumb))
                    }
                }

                withContext(Dispatchers.Main) {
                    if (videoList.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.noVideosLayout.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.adapter = FeedVideoAdapter(videoList)
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.noVideosLayout.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load feed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
