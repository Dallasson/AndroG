package com.android.challenge

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.android.challenge.adapters.TabAdapter
import com.android.challenge.fragments.FeedFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var currentFragment: Fragment? = null

    private val requiredPermissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var currentPermissionIndex = 0
    private var isStoragePermissionChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        val adapter = TabAdapter(this)
        viewPager.adapter = adapter

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
            ) {
                if (f is FeedFragment) {
                    currentFragment = f
                }
            }
        }, true)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val fragment = supportFragmentManager.findFragmentByTag("f$position")
                if (position == 0 && fragment is FeedFragment) {
                    currentFragment = fragment
                    fragment.resumeCurrentVideo()
                } else {
                    val feedFragment = supportFragmentManager.findFragmentByTag("f0") as? FeedFragment
                    feedFragment?.pauseCurrentVideo()
                }
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Feed"
                1 -> "Camera"
                2 -> "Gallery"
                else -> ""
            }
        }.attach()

        requestAllFileAccessPermissionFirst()
    }

    override fun onResume() {
        super.onResume()
        if (!isStoragePermissionChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStoragePermissionChecked = Environment.isExternalStorageManager()
            if (isStoragePermissionChecked) {
                requestNextPermission()
            }
        }

        if (viewPager.currentItem == 0) {
            (currentFragment as? FeedFragment)?.resumeCurrentVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        (currentFragment as? FeedFragment)?.pauseCurrentVideo()
    }

    private fun requestAllFileAccessPermissionFirst() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val uri = "package:$packageName".toUri()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(this, "Unable to open settings for All Files Access.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            isStoragePermissionChecked = true
            requestNextPermission()
        }
    }

    private fun requestNextPermission() {
        if (!isStoragePermissionChecked) return
        if (currentPermissionIndex >= requiredPermissions.size) return

        val permission = requiredPermissions[currentPermissionIndex]
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100 + currentPermissionIndex)
        } else {
            currentPermissionIndex++
            requestNextPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty() && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentPermissionIndex++
                requestNextPermission()
            } else {
               // Toast.makeText(this, "Permission ${permissions[0]} is required.", Toast.LENGTH_SHORT).show()
            }
        } else {
          //  Toast.makeText(this, "Permission request interrupted.", Toast.LENGTH_SHORT).show()
        }
    }
}
