package com.android.challenge.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.challenge.fragments.CameraFragment
import com.android.challenge.fragments.FeedFragment
import com.android.challenge.fragments.GalleryFragment

class TabAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FeedFragment()
            1 -> CameraFragment()
            2 -> GalleryFragment()
            else -> Fragment()
        }
    }
}