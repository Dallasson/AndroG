package com.android.challenge.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.*
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.android.challenge.R
import com.android.challenge.databinding.CameraLayoutBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {

    private lateinit var binding: CameraLayoutBinding
    private lateinit var cameraManager: CameraManager
    private var frontCameraId: String? = null
    private var backCameraId: String? = null

    private var frontCaptureSession: CameraCaptureSession? = null
    private var backCaptureSession: CameraCaptureSession? = null

    private var frontDevice: CameraDevice? = null
    private var backDevice: CameraDevice? = null

    private lateinit var frontRecorder: MediaRecorder
    private lateinit var backRecorder: MediaRecorder

    private lateinit var frontPreviewSurface: Surface
    private lateinit var backPreviewSurface: Surface

    private lateinit var frontRecordSurface: Surface
    private lateinit var backRecordSurface: Surface

    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        findCameraIds()

        binding.recordButton.setOnClickListener {
            if (!isRecording) {
                startDualRecording()
            }
        }
    }

    private fun findCameraIds() {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> backCameraId = id
                CameraCharacteristics.LENS_FACING_FRONT -> frontCameraId = id
            }
        }
    }

    private fun startDualRecording() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }

        setupRecorders()

        // Front Camera Setup
        openCamera(frontCameraId!!) { camera ->
            frontDevice = camera
            val texture = binding.frontPreview.surfaceTexture!!
            texture.setDefaultBufferSize(1920, 1080)

            binding.frontPreview.scaleX = -1f // ✅ Flip horizontally for mirror effect

            frontPreviewSurface = Surface(texture)
            frontRecordSurface = frontRecorder.surface

            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(frontPreviewSurface)
                addTarget(frontRecordSurface)
            }

            camera.createCaptureSession(
                listOf(frontPreviewSurface, frontRecordSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        frontCaptureSession = session
                        session.setRepeatingRequest(request.build(), null, null)
                        frontRecorder.start()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                null
            )
        }

        // Back Camera Setup
        openCamera(backCameraId!!) { camera ->
            backDevice = camera
            val texture = binding.backPreview.surfaceTexture!!
            texture.setDefaultBufferSize(1920, 1080)
            backPreviewSurface = Surface(texture)
            backRecordSurface = backRecorder.surface

            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(backPreviewSurface)
                addTarget(backRecordSurface)
            }

            camera.createCaptureSession(
                listOf(backPreviewSurface, backRecordSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        backCaptureSession = session
                        session.setRepeatingRequest(request.build(), null, null)
                        backRecorder.start()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                null
            )
        }

        isRecording = true
        Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()

        var timeLeft = 15
        binding.recordButton.text = timeLeft.toString()
        val countdown = object : Runnable {
            override fun run() {
                timeLeft--
                binding.recordButton.text = timeLeft.toString()
                if (timeLeft > 0) handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(countdown, 1000)

        handler.postDelayed({
            stopDualRecording()
        }, 15_000)
    }

    private fun setupRecorders() {
        frontRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(createVideoFile("front").absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
            prepare()
        }

        backRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(createVideoFile("back").absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
            prepare()
        }
    }

    private fun stopDualRecording() {
        isRecording = false
        try {
            frontRecorder.stop()
            backRecorder.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            frontRecorder.reset()
            backRecorder.reset()
            frontDevice?.close()
            backDevice?.close()
        }

        Toast.makeText(requireContext(), "Recording finished", Toast.LENGTH_SHORT).show()
        activity?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String, callback: (CameraDevice) -> Unit) {
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) = callback(camera)
            override fun onDisconnected(camera: CameraDevice) = camera.close()
            override fun onError(camera: CameraDevice, error: Int) = camera.close()
        }, null)
    }

    private fun createVideoFile(tag: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "VID_${tag}_$timeStamp.mp4"
        val storageDir = File(Environment.getExternalStorageDirectory(), "Challenge")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File(storageDir, fileName)
    }
}
