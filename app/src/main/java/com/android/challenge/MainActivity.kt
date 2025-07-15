package com.android.challenge

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.android.challenge.fragments.CameraFragment
import com.android.challenge.fragments.CameraFragmentListener
import com.android.challenge.fragments.FeedFragment
import com.android.challenge.fragments.GalleryFragment
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() , CameraFragmentListener {

    private lateinit var tabLayout: TabLayout
    private var currentFragmentTag: String? = null

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

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Feed"))
        tabLayout.addTab(tabLayout.newTab().setText("Camera"))
        tabLayout.addTab(tabLayout.newTab().setText("Gallery"))

        // Default tab
        if (savedInstanceState == null) {
            switchToFragment(FeedFragment(), "FEED")
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> switchToFragment(FeedFragment(), "FEED")
                    1 -> switchToFragment(CameraFragment(), "CAMERA")
                    2 -> switchToFragment(GalleryFragment(), "GALLERY")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        requestAllFileAccessPermissionFirst()
    }

    private fun switchToFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        currentFragmentTag?.let {
            supportFragmentManager.findFragmentByTag(it)?.let { oldFragment ->
                transaction.remove(oldFragment)
            }
        }

        transaction.replace(R.id.fragmentContainer, fragment, tag)
        transaction.commit()
        currentFragmentTag = tag
    }

    override fun onResume() {
        super.onResume()
        if (!isStoragePermissionChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStoragePermissionChecked = Environment.isExternalStorageManager()
            if (isStoragePermissionChecked) {
                requestNextPermission()
            }
        }
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
            }
        }
    }

    override fun navigateToGallery() {
        val tab = tabLayout.getTabAt(2) // Index 2 = Gallery
        tab?.select()
    }
}
