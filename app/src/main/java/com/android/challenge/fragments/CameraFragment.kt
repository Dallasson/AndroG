package com.android.challenge.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.android.challenge.R
import com.android.challenge.databinding.CameraLayoutBinding
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.Rotation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

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

    @SuppressLint("MissingPermission", "NewApi")
    private fun startDualCameraRecording() {
        if (
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Camera and audio permissions required", Toast.LENGTH_SHORT).show()
            return
        }

        val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }

        val backCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        if (frontCameraId == null || backCameraId == null) {
            Toast.makeText(requireContext(), "Both front and back cameras are required", Toast.LENGTH_SHORT).show()
            return
        }

        frontOutputFile = createVideoFile("cam1")
        backOutputFile = createVideoFile("cam2")
        setupRecorders()

        val executor: Executor = ContextCompat.getMainExecutor(requireContext())

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
                        checkBothStarted()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(requireContext(), "Failed to configure session for camera 1", Toast.LENGTH_SHORT).show()
                    }
                }, handler)
            }

            override fun onDisconnected(cam: CameraDevice) = cam.close()
            override fun onError(cam: CameraDevice, error: Int) {
                cam.close()
                Toast.makeText(requireContext(), "Camera 1 error: $error", Toast.LENGTH_SHORT).show()
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
                        checkBothStarted()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(requireContext(), "Failed to configure session for camera 2", Toast.LENGTH_SHORT).show()
                    }
                }, handler)
            }

            override fun onDisconnected(cam: CameraDevice) = cam.close()
            override fun onError(cam: CameraDevice, error: Int) {
                cam.close()
                Toast.makeText(requireContext(), "Camera 2 error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkBothStarted() {
        if (!isRecording) {
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
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(backOutputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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
        val fileName = "VID_${tag}_$timeStamp.mp4"
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Challenge")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File(storageDir, fileName)
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
            recorder1.stop()
            recorder2.stop()
        } catch (_: Exception) {}

        recorder1.reset()
        recorder2.reset()
        camera1?.close()
        camera2?.close()
        isRecording = false

        val flippedFront = File(frontOutputFile.parent, "flipped_front_${frontOutputFile.name}")
        Mp4Composer(frontOutputFile.absolutePath, flippedFront.absolutePath)
            .rotation(Rotation.ROTATION_270)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .flipHorizontal(true)
            .listener(object : Mp4Composer.Listener {
                override fun onProgress(progress: Double) {}
                override fun onCurrentWrittenVideoTime(timeUs: Long) {}
                override fun onCanceled() {}
                override fun onCompleted() {
                    requireActivity().runOnUiThread {
                        if (frontOutputFile.exists()) frontOutputFile.delete()
                        Toast.makeText(requireContext(), "Recording finished and front video flipped!", Toast.LENGTH_SHORT).show()
                        activity?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
                    }
                }

                override fun onFailed(exception: java.lang.Exception?) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to flip video", Toast.LENGTH_SHORT).show()
                        activity?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
                    }
                }
            })
            .start()
    }
}
