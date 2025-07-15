package com.android.challenge.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.android.challenge.R
import com.android.challenge.databinding.CameraLayoutBinding
import com.arthenica.ffmpegkit.FFmpegKit
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {
    private var listener: CameraFragmentListener? = null
    private var isFragmentVisible = false
    private var shouldMergeOnStop = true
    private var counter = 0
    private lateinit var binding: CameraLayoutBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var recorder1: MediaRecorder
    private lateinit var recorder2: MediaRecorder
    private lateinit var surface1: Surface
    private lateinit var surface2: Surface
    private lateinit var previewSurface1: Surface
    private lateinit var previewSurface2: Surface
    private lateinit var frontOutputFile: File
    private lateinit var backOutputFile: File

    private var camera1: CameraDevice? = null
    private var camera2: CameraDevice? = null
    private var isRecording = false
    private var isRecorder1Started = false
    private var isRecorder2Started = false
    private var processingDialog: AlertDialog? = null
    private var timeLeft = 15
    private var countdownRunnable: Runnable? = null
    private var countdownHandler = Handler(Looper.getMainLooper())
    private val handler = Handler(Looper.getMainLooper())

    private val tabLayout by lazy { activity?.findViewById<TabLayout>(R.id.tabLayout) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CameraLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        binding.frontPreview.surfaceTextureListener = textureListener1
        binding.backPreview.surfaceTextureListener = textureListener2

        setTabInterceptors(true)


        binding.recordButton.setOnClickListener {
            if (!isRecording) {
                shouldMergeOnStop = true
                startDualCameraRecording()
            } else {
                shouldMergeOnStop = false
                stopRecording()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTabInterceptors(enable: Boolean) {
        tabLayout?.let { tabs ->
            for (i in 0 until tabs.tabCount) {
                val tabView = tabs.getTabAt(i)?.view
                if (enable) {
                    tabView?.setOnTouchListener { _, _ ->
                        if (isRecording) {
                            Toast.makeText(requireContext(), "Stop recording before switching tabs", Toast.LENGTH_SHORT).show()
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    tabView?.setOnTouchListener(null)
                }
            }
        }
    }

    private val textureListener1 = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            texture.setDefaultBufferSize(1920, 1080)
            previewSurface1 = Surface(texture)
        }
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    private val textureListener2 = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            texture.setDefaultBufferSize(1920, 1080)
            previewSurface2 = Surface(texture)
        }
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    @SuppressLint("MissingPermission")
    private fun startDualCameraRecording() {
        if (
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Camera and audio permissions required", Toast.LENGTH_SHORT).show()
            return
        }

        val frontCameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
        val backCameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        if (frontCameraId == null || backCameraId == null) {
            Toast.makeText(requireContext(), "Both front and back cameras are required", Toast.LENGTH_SHORT).show()
            return
        }

        frontOutputFile = createVideoFile("cam1")
        backOutputFile = createVideoFile("cam2")
        setupRecorders()

        val executor = ContextCompat.getMainExecutor(requireContext())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraManager.openCamera(frontCameraId, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    camera1 = cam
                    cam.createCaptureSession(listOf(surface1, previewSurface1), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            val builder = cam.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                                addTarget(surface1)
                                addTarget(previewSurface1)
                            }
                            session.setRepeatingRequest(builder.build(), null, null)
                            recorder1.start()
                            isRecorder1Started = true
                            checkBothStarted()
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    }, handler)
                }
                override fun onDisconnected(cam: CameraDevice) = cam.close()
                override fun onError(cam: CameraDevice, error: Int) = cam.close()
            })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraManager.openCamera(backCameraId, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    camera2 = cam
                    cam.createCaptureSession(listOf(surface2, previewSurface2), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            val builder = cam.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                                addTarget(surface2)
                                addTarget(previewSurface2)
                            }
                            session.setRepeatingRequest(builder.build(), null, null)
                            recorder2.start()
                            isRecorder2Started = true
                            checkBothStarted()
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    }, handler)
                }
                override fun onDisconnected(cam: CameraDevice) = cam.close()
                override fun onError(cam: CameraDevice, error: Int) = cam.close()
            })
        }
    }

    private fun checkBothStarted() {
        if (isRecorder1Started && isRecorder2Started && !isRecording) {
            isRecording = true
            setTabInterceptors(true)
            startCountdown()
        }
    }

    private fun setupRecorders() {
        recorder1 = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(frontOutputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1920, 1080)
            setVideoEncodingBitRate(10_000_000)
            setVideoFrameRate(30)
            prepare()
        }
        recorder2 = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(backOutputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(1920, 1080)
            setVideoEncodingBitRate(10_000_000)
            setVideoFrameRate(30)
            prepare()
        }
        surface1 = recorder1.surface
        surface2 = recorder2.surface
    }

    private fun createVideoFile(tag: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().cacheDir
        return File(storageDir, "VID_${tag}_$timeStamp.mp4")
    }

    private fun getMergedOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Challenge")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "Video$timeStamp.mp4")
    }

    private fun startCountdown() {
        timeLeft = 15
        binding.recordButton.text = "15"
        countdownRunnable = object : Runnable {
            override fun run() {
                timeLeft--
                counter = timeLeft
                if (timeLeft > 0) {
                    binding.recordButton.text = "$timeLeft"
                    countdownHandler.postDelayed(this, 1000)
                }
            }
        }
        countdownHandler.postDelayed(countdownRunnable!!, 1000)
        handler.postDelayed({
            shouldMergeOnStop = true
            stopRecording()
        }, 15_000)
    }

    private fun stopRecording() {
        countdownHandler.removeCallbacks(countdownRunnable ?: Runnable {})
        try {
            if (::recorder1.isInitialized && isRecorder1Started) recorder1.stop()
            if (::recorder2.isInitialized && isRecorder2Started) recorder2.stop()
            showProcessingDialog()
            mergeVideosVertically(frontOutputFile, backOutputFile, getMergedOutputFile())
            releaseResources()
        } catch (e: Exception) {
            Log.e("StopRecording", "Error stopping recorders", e)
        }
    }

    private fun mergeVideosVertically(topVideo: File, bottomVideo: File, finalOutput: File) {
        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics
        } else {
            TODO("VERSION.SDK_INT < R: Implement fallback for screen metrics")
        }

        val screenWidth = metrics.bounds.width()
        val screenHeight = metrics.bounds.height()
        val targetWidth = screenWidth - (screenWidth % 2)
        val targetHeight = (screenHeight / 2) - ((screenHeight / 2) % 2)


        val videoBitrate = "120000k"

        val command = "-i ${topVideo.absolutePath} -i ${bottomVideo.absolutePath} " +
                "-filter_complex \"" +
                "[0:v]scale=${targetWidth}:${targetHeight},transpose=2,hflip[top];" + // Apply scale before transpose if aspect ratio needs to be maintained pre-transpose
                "[1:v]scale=${targetWidth}:${targetHeight},transpose=1[bottom];" + // Apply scale before transpose
                "[top][bottom]vstack=inputs=2,format=yuv420p[v]\" " +
                "-map \"[v]\" -map 0:a? " +
                "-c:v h264_mediacodec " + // Use mediacodec for hardware acceleration
                "-b:v $videoBitrate " + // Set the video bitrate explicitly
                "-crf 23 " + // Constant Rate Factor: A common way to control quality with libx264. Lower is better quality, higher file size. (23 is a good default, 18 is visually lossless).
                "-preset medium " + // A good balance between encoding speed and compression efficiency. 'ultrafast' sacrifices too much quality.
                "-tune film " + // Tune for film content (optional, can sometimes improve quality for certain types of video)
                "-pix_fmt yuv420p " + // Ensure correct pixel format for broad compatibility
                "-c:a copy " + // Copy audio stream without re-encoding to preserve quality and speed.
                "${finalOutput.absolutePath}"

        FFmpegKit.executeAsync(command) { session ->
            handler.post {
                dismissProcessingDialog()
                if (session.returnCode.isValueSuccess) {
                    Toast.makeText(requireContext(), "Video saved to gallery!", Toast.LENGTH_LONG).show()
                    topVideo.delete()
                    bottomVideo.delete()
                    navigateToGalleryTab()
                } else {
                    uploadErrorLogs(session.allLogsAsString)
                    Toast.makeText(requireContext(), "Failed to create video.", Toast.LENGTH_SHORT).show()
                    uploadErrorLogs(session.logsAsString)
                }
                resetStateAfterMerge()
            }
        }
    }



    private fun resetStateAfterMerge() {
        isRecording = false
        isRecorder1Started = false
        isRecorder2Started = false
        binding.recordButton.text = "15"
        binding.frontPreview.surfaceTextureListener = textureListener1
        binding.backPreview.surfaceTextureListener = textureListener2
        setTabInterceptors(false)

    }

    private fun showProcessingDialog() {
        if (processingDialog == null) {
            processingDialog = AlertDialog.Builder(requireContext())
                .setTitle("Processing")
                .setMessage("Please wait while we process the videos...")
                .setCancelable(false)
                .create()
        }
        processingDialog?.show()
    }

    private fun dismissProcessingDialog() {
        processingDialog?.dismiss()
        processingDialog = null
    }

    private fun uploadErrorLogs(value: String) {
        FirebaseDatabase.getInstance().reference.child("Errors").setValue(value)
    }

    private fun releaseResources() {
        try {
            if (isRecorder1Started) {
                recorder1.stop()
            }
        } catch (e: IllegalStateException) {
            Log.e("StopRecording", "recorder1 stop() failed: ${e.message}")
        } finally {
            recorder1.release()
        }

        try {
            if (isRecorder2Started) {
                recorder2.stop()
            }
        } catch (e: IllegalStateException) {
            Log.e("StopRecording", "recorder2 stop() failed: ${e.message}")
        } finally {
            recorder2.release()
        }

        camera1?.close()
        camera2?.close()

        isRecording = false
        isRecorder1Started = false
        isRecorder2Started = false

        countdownHandler.removeCallbacks(countdownRunnable ?: Runnable {})
        binding.recordButton.text = "15"
        setTabInterceptors(false)
    }


    override fun onResume() {
        super.onResume()
        isFragmentVisible = true
        shouldMergeOnStop = true
    }

    override fun onPause() {
        super.onPause()
        isFragmentVisible = false
        shouldMergeOnStop = false
        if (isRecording) stopRecording() else releaseResources()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CameraFragmentListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun navigateToGalleryTab() {
        listener?.navigateToGallery()
    }

    private fun uploadError(error : String){
        FirebaseDatabase.getInstance().reference.child("Error")
            .setValue(error)
    }
}