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
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {

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

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CameraLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        binding.frontPreview.surfaceTextureListener = textureListener1
        binding.backPreview.surfaceTextureListener = textureListener2
        binding.recordButton.setOnClickListener {
            if (!isRecording) {
                startDualCameraRecording()
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

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Toast.makeText(requireContext(), "Failed to configure front camera", Toast.LENGTH_SHORT).show()
                        }
                    }, handler)
                }

                override fun onDisconnected(cam: CameraDevice) = cam.close()
                override fun onError(cam: CameraDevice, error: Int) {
                    cam.close()
                    Toast.makeText(requireContext(), "Front camera error: $error", Toast.LENGTH_SHORT).show()
                }
            })
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

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Toast.makeText(requireContext(), "Failed to configure back camera", Toast.LENGTH_SHORT).show()
                        }
                    }, handler)
                }

                override fun onDisconnected(cam: CameraDevice) = cam.close()
                override fun onError(cam: CameraDevice, error: Int) {
                    cam.close()
                    Toast.makeText(requireContext(), "Back camera error: $error", Toast.LENGTH_SHORT).show()
                }
            })
        }


    }

    private fun checkBothStarted() {
        if (isRecorder1Started && isRecorder2Started && !isRecording) {
            isRecording = true
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
        var timeLeft = 15
        binding.recordButton.text = timeLeft.toString()
        handler.postDelayed(object : Runnable {
            override fun run() {
                timeLeft--
                binding.recordButton.text = timeLeft.toString()
                if (timeLeft > 0) handler.postDelayed(this, 1000)
            }
        }, 1000)
        handler.postDelayed({ stopRecording() }, 15_000)
    }

    private fun stopRecording() {
        try {
            if (::recorder1.isInitialized && isRecorder1Started) recorder1.stop()
            if (::recorder2.isInitialized && isRecorder2Started) recorder2.stop()
        } catch (e: Exception) {
            Log.e("StopRecording", "Error stopping recorders", e)
        } finally {
            recorder1.release()
            recorder2.release()

            camera1?.close()
            camera2?.close()
            isRecording = false
            isRecorder1Started = false
            isRecorder2Started = false

            showProcessingDialog()
            mergeVideosVertically(frontOutputFile, backOutputFile, getMergedOutputFile())

        }
    }



    private fun mergeVideosVertically(topVideo: File, bottomVideo: File, finalOutput: File) {
        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics
        } else {
            TODO("VERSION.SDK_INT < R")
        }
        val screenWidth = metrics.bounds.width()
        val screenHeight = metrics.bounds.height()
        val targetWidth = screenWidth - (screenWidth % 2)
        val targetHeight = (screenHeight / 2) - ((screenHeight / 2) % 2)

        val command =
            "-i ${topVideo.absolutePath} -i ${bottomVideo.absolutePath} " +
                    "-filter_complex \"" +
                    "[0:v]transpose=2,hflip,scale=${targetWidth}:${targetHeight}[top];" +
                    "[1:v]transpose=1,scale=${targetWidth}:${targetHeight}[bottom];" +
                    "[top][bottom]vstack=inputs=2" +
                    ",format=yuv420p[v]\" " +
                    "-map \"[v]\" -map 0:a? -c:a copy " +
                    "-c:v h264_mediacodec -preset ultrafast ${finalOutput.absolutePath}"

        FFmpegKit.executeAsync(command) { session ->
            handler.post {
                dismissProcessingDialog()
                if (session.returnCode.isValueSuccess) {
                    Toast.makeText(requireContext(), "Video saved to gallery!", Toast.LENGTH_LONG).show()
                    topVideo.delete()
                    bottomVideo.delete()
                    navigateToGalleryTab()
                } else {
                    val detailedLogs = session.allLogsAsString
                    Log.e("FFmpegMerge", "FFmpeg failed. Full logs: $detailedLogs")
                    uploadErrorLogs(detailedLogs)
                    Toast.makeText(requireContext(), "Failed to create final video.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun navigateToGalleryTab() {
        requireActivity().runOnUiThread {
            activity?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
        }
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

    private fun uploadErrorLogs(value : String){
        FirebaseDatabase.getInstance().reference
            .child("Errors")
            .setValue(value)
    }
}

