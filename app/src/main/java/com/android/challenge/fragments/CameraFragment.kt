package com.android.challenge.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
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
import java.util.Date
import java.util.Locale

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

    private fun checkBothStarted() {
        if (isRecorder1Started && isRecorder2Started && !isRecording) {
            isRecording = true
            startCountdown()
        }
    }

    private fun setupRecorders() {
        recorder1 = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) // Only one recorder should capture audio
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
        return File(dir, "MERGED_$timeStamp.mp4")
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

            val flippedFront = File(frontOutputFile.parent, "flipped_${frontOutputFile.name}")
            showProcessingDialog()

            mergeVideosVertically(frontOutputFile, backOutputFile, getMergedOutputFile())

//            Mp4Composer(frontOutputFile.absolutePath, flippedFront.absolutePath)
//                .rotation(Rotation.ROTATION_270)
//                .flipHorizontal(true)
//                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
//                .listener(object : Mp4Composer.Listener {
//                    override fun onProgress(progress: Double) {}
//                    override fun onCurrentWrittenVideoTime(timeUs: Long) {}
//                    override fun onCanceled() {
//                        dismissProcessingDialog()
//                    }
//
//                    override fun onCompleted() {
//
//                    }
//
//                    override fun onFailed(exception: Exception?) {
//                        dismissProcessingDialog()
//                        Toast.makeText(requireContext(), "Failed to flip video", Toast.LENGTH_SHORT).show()
//                    }
//                }).start()
        }
    }


    /**
     * Merges two videos vertically using FFmpeg.
     * The first video (top) will provide the audio track.
     * @param topVideo The video file to place at the top.
     * @param bottomVideo The video file to place at the bottom.
     * @param finalOutput The destination file for the merged video.
     */
    private fun mergeVideosVertically(topVideo: File, bottomVideo: File, finalOutput: File) {
        // This FFmpeg command stacks two videos.
        // -filter_complex "[0:v][1:v]vstack=inputs=2[v]" -> Takes video from input 0 and input 1, stacks them, and tags the result as [v]
        // -map "[v]" -> Uses the stacked video for the output
        // -map 0:a?  -> Takes the audio from the first input (topVideo). The '?' makes it optional, preventing errors if there's no audio.
        // -c:a copy -> Copies the audio stream without re-encoding for speed and quality.
        // -preset ultrafast -> Optimizes for speed, crucial for a good user experience on mobile.
        val command = "-i ${topVideo.absolutePath} -i ${bottomVideo.absolutePath} -filter_complex \"[0:v]scale=960:540[v0];[1:v]scale=960:540[v1];[v0][v1]vstack=inputs=2[v]\" -map \"[v]\" -map 0:a? -c:a copy -preset ultrafast ${finalOutput.absolutePath}"

        FFmpegKit.executeAsync(command) { session ->

            handler.post {
                dismissProcessingDialog()
                if (session.returnCode.isValueSuccess) {
                    Toast.makeText(requireContext(), "Video saved to gallery!", Toast.LENGTH_LONG).show()
                    topVideo.delete()
                    frontOutputFile.delete()
                    bottomVideo.delete()
                    navigateToGalleryTab()

                } else {
                    val errorLog = "FFmpeg failed!\nReturn Code: ${session.returnCode}\nLogs: ${session.allLogsAsString}"
                    Log.e("FFmpegMerge", errorLog)
                    uploadErrorLogs(errorLog) // Upload detailed logs for debugging
                    Toast.makeText(requireContext(), "Failed to create final video.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Navigates the user to the gallery tab.
     * TODO: Replace this with your app's navigation logic (e.g., NavController, FragmentTransaction, etc.).
     */
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

