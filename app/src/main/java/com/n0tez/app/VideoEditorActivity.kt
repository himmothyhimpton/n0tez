package com.n0tez.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.android.video.util.VideoEditorUtil
import com.n0tez.app.databinding.ActivityVideoEditorBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private var videoUri: Uri? = null
    private var videoPath: String? = null
    private var isPlaying = false
    private var videoDuration = 0
    private var trimStart = 0f
    private var trimEnd = 0f
    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val currentPosition = binding.videoView.currentPosition
                updateTimeDisplay(currentPosition, videoDuration)
                
                // Loop if reached trim end
                if (trimEnd > 0 && currentPosition >= trimEnd) {
                    binding.videoView.seekTo(trimStart.toInt())
                }
                
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        videoPath = intent.getStringExtra("VIDEO_FILE_PATH")
        videoUri = intent.data

        if (videoUri != null) {
            // Need to get absolute path for FFmpeg
            // This is complex with Uri, ideally we copy to cache
            // For now, assuming file Uri or copy logic
            // simplified:
            loadVideo(videoUri!!)
        } else if (videoPath != null) {
            loadVideo(Uri.fromFile(File(videoPath!!)))
        } else {
            Toast.makeText(this, "No video loaded", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Video Editor"

        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) pauseVideo() else playVideo()
        }

        binding.btnSave.setOnClickListener { saveVideo() }
        binding.btnExtractFrame.setOnClickListener { extractFrame() }
        
        binding.rangeSliderTrim.addOnChangeListener { slider, value, fromUser ->
            val values = slider.values
            trimStart = values[0]
            trimEnd = values[1]
            if (fromUser) {
                binding.videoView.seekTo(trimStart.toInt())
                updateTimeDisplay(trimStart.toInt(), videoDuration)
            }
        }
        
        binding.videoView.setOnPreparedListener { mp ->
            videoDuration = mp.duration
            trimEnd = videoDuration.toFloat()
            binding.rangeSliderTrim.valueFrom = 0f
            binding.rangeSliderTrim.valueTo = videoDuration.toFloat()
            binding.rangeSliderTrim.setValues(0f, videoDuration.toFloat())
            updateTimeDisplay(0, videoDuration)
        }
        
        binding.videoView.setOnCompletionListener {
            isPlaying = false
            binding.btnPlayPause.setIconResource(R.drawable.ic_play) // Assuming ic_play
        }
    }

    private fun loadVideo(uri: Uri) {
        binding.videoView.setVideoURI(uri)
        videoPath?.let { updateNativeVideoInfo(it) }
        
        if (videoPath == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File(cacheDir, "temp_video.mp4")
                    val outputStream = java.io.FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    
                    inputStream?.close()
                    outputStream.close()
                    
                    videoPath = tempFile.absolutePath
                    updateNativeVideoInfo(tempFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VideoEditorActivity, "Failed to load video file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateNativeVideoInfo(path: String) {
        if (!VideoEditorUtil.isAvailable()) {
            binding.tvVideoInfo.text = "Native video engine unavailable"
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val info = VideoEditorUtil.nativeGetVideoInfo(this@VideoEditorActivity, path)
                val resolved = if (info.isNullOrBlank()) "Native info unavailable" else info
                withContext(Dispatchers.Main) {
                    binding.tvVideoInfo.text = resolved
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.tvVideoInfo.text = "Native info error: ${e.message}"
                }
            }
        }
    }
    private fun playVideo() {
        binding.videoView.start()
        isPlaying = true
        binding.btnPlayPause.setIconResource(R.drawable.ic_pause) // Assuming ic_pause
        handler.post(updateProgressRunnable)
    }

    private fun pauseVideo() {
        binding.videoView.pause()
        isPlaying = false
        binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateTimeDisplay(current: Int, total: Int) {
        binding.tvCurrentTime.text = formatTime(current)
        binding.tvTotalTime.text = formatTime(total)
    }

    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun trimVideo() {
        if (videoPath == null) return
        
        val startSec = trimStart / 1000f
        val durationSec = (trimEnd - trimStart) / 1000f
        
        val outputFile = File(filesDir, "media/video/trimmed_${System.currentTimeMillis()}.mp4")
        outputFile.parentFile?.mkdirs()
        
        val cmd = "-ss $startSec -i \"$videoPath\" -t $durationSec -c copy \"${outputFile.absolutePath}\""
        
        FFmpegKit.executeAsync(cmd) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                runOnUiThread {
                    Toast.makeText(this, "Video trimmed successfully", Toast.LENGTH_SHORT).show()
                    // Maybe open the trimmed video?
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Trim failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun saveVideo() {
        // Just trimming for now as "Save"
        trimVideo()
    }

    private fun extractFrame() {
        val path = videoPath
        if (path == null) {
            Toast.makeText(this, "No video loaded", Toast.LENGTH_SHORT).show()
            return
        }
        if (!VideoEditorUtil.isAvailable()) {
            Toast.makeText(this, "Native video engine unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                val w = if (binding.videoView.width > 0) binding.videoView.width else 640
                val h = if (binding.videoView.height > 0) binding.videoView.height else 360
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val openResult = VideoEditorUtil.nativeOpenVideoFile(path, 0)
                if (openResult < 0) {
                    throw IllegalStateException("Native open failed")
                }
                VideoEditorUtil.nativeSeekTo(binding.videoView.currentPosition.toLong())
                val frameResult = VideoEditorUtil.nativeGetNextFrame(bitmap)
                if (frameResult < 0) {
                    throw IllegalStateException("Frame extraction failed")
                }
                val outputFile = File(cacheDir, "frame_${System.currentTimeMillis()}.png")
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoEditorActivity, "Frame saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoEditorActivity, "Frame extract failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                VideoEditorUtil.nativeRelease()
                bitmap?.recycle()
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
    }
}
