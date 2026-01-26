package com.n0tez.app

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.n0tez.app.databinding.ActivityVideoEditorBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoEditorBinding
    private var videoUri: Uri? = null
    private var videoDuration: Int = 0
    private var trimStart: Int = 0 // in milliseconds
    private var trimEnd: Int = 0 // in milliseconds
    private var isPlaying = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val currentPosition = binding.videoView.currentPosition
                binding.seekBarProgress.progress = currentPosition
                updateTimeDisplay(currentPosition, videoDuration)
                
                // Check if we've reached the trim end point
                if (trimEnd > 0 && currentPosition >= trimEnd) {
                    binding.videoView.seekTo(trimStart)
                }
                
                handler.postDelayed(this, 100)
            }
        }
    }
    
    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoUri = it
            loadVideo(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        
        // Check if video path was passed
        intent.getStringExtra("VIDEO_FILE_PATH")?.let { path ->
            loadVideoFromPath(path)
        } ?: run {
            // Prompt user to select video
            pickVideoLauncher.launch("video/*")
        }
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Video Editor"
        
        binding.btnSelectVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
        
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }
        
        binding.btnStop.setOnClickListener {
            stopPlayback()
        }
        
        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.rangeSliderTrim.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            trimStart = values[0].toInt()
            trimEnd = values[1].toInt()
            updateTrimDisplay()
        }
        
        binding.spinnerQuality.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val qualities = arrayOf("Original", "High (1080p)", "Medium (720p)", "Low (480p)")
                Toast.makeText(this@VideoEditorActivity, "Quality: ${qualities[position]}", Toast.LENGTH_SHORT).show()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        binding.btnExtractFrame.setOnClickListener {
            extractCurrentFrame()
        }
        
        binding.btnSave.setOnClickListener {
            saveEditedVideo()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        // Setup video view
        binding.videoView.setOnPreparedListener { mp ->
            mp.isLooping = false
            videoDuration = mp.duration
            binding.seekBarProgress.max = videoDuration
            
            trimEnd = videoDuration
            binding.rangeSliderTrim.valueFrom = 0f
            binding.rangeSliderTrim.valueTo = videoDuration.toFloat()
            binding.rangeSliderTrim.values = listOf(0f, videoDuration.toFloat())
            
            updateTimeDisplay(0, videoDuration)
            updateTrimDisplay()
            updateVideoInfo()
        }
        
        binding.videoView.setOnCompletionListener {
            isPlaying = false
            binding.btnPlayPause.text = "Play"
            binding.btnPlayPause.setIconResource(R.drawable.ic_add)
            handler.removeCallbacks(updateProgressRunnable)
            binding.videoView.seekTo(trimStart)
        }
    }
    
    private fun loadVideo(uri: Uri) {
        try {
            binding.videoView.setVideoURI(uri)
            videoUri = uri
            
            Toast.makeText(this, "Video loaded", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadVideoFromPath(path: String) {
        try {
            val uri = Uri.fromFile(File(path))
            binding.videoView.setVideoURI(uri)
            videoUri = uri
            
            Toast.makeText(this, "Video loaded", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load video: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun togglePlayPause() {
        if (isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            binding.btnPlayPause.text = "Play"
            binding.btnPlayPause.setIconResource(R.drawable.ic_add)
            handler.removeCallbacks(updateProgressRunnable)
        } else {
            // If at the end, restart from trim start
            if (binding.videoView.currentPosition >= trimEnd || 
                binding.videoView.currentPosition >= videoDuration) {
                binding.videoView.seekTo(trimStart)
            }
            
            binding.videoView.start()
            isPlaying = true
            binding.btnPlayPause.text = "Pause"
            binding.btnPlayPause.setIconResource(R.drawable.ic_close)
            handler.post(updateProgressRunnable)
        }
    }
    
    private fun stopPlayback() {
        if (isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            binding.btnPlayPause.text = "Play"
            binding.btnPlayPause.setIconResource(R.drawable.ic_add)
            handler.removeCallbacks(updateProgressRunnable)
        }
        binding.videoView.seekTo(trimStart)
        binding.seekBarProgress.progress = trimStart
        updateTimeDisplay(trimStart, videoDuration)
    }
    
    private fun updateTimeDisplay(current: Int, total: Int) {
        val currentTime = formatTime(current)
        val totalTime = formatTime(total)
        binding.tvCurrentTime.text = currentTime
        binding.tvTotalTime.text = totalTime
    }
    
    private fun updateTrimDisplay() {
        val startTime = formatTime(trimStart)
        val endTime = formatTime(trimEnd)
        val duration = formatTime(trimEnd - trimStart)
        binding.tvTrimInfo.text = "Trim: $startTime - $endTime (Duration: $duration)"
    }
    
    private fun updateVideoInfo() {
        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, uri)
                
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                
                retriever.release()
                
                val bitrateKbps = bitrate?.toLongOrNull()?.div(1000) ?: 0
                binding.tvVideoInfo.text = "Resolution: ${width}x${height} | Bitrate: ${bitrateKbps}kbps"
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun extractCurrentFrame() {
        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, uri)
                
                val currentPosition = binding.videoView.currentPosition
                val bitmap = retriever.getFrameAtTime(
                    currentPosition * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                retriever.release()
                
                bitmap?.let {
                    // Save frame as image
                    val imagesDir = File(filesDir, "media/images")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val imageFile = File(imagesDir, "frame_$timestamp.jpg")
                    
                    java.io.FileOutputStream(imageFile).use { out ->
                        it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    
                    Toast.makeText(this, "Frame extracted: ${imageFile.name}", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(this, "Failed to extract frame", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to extract frame: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun saveEditedVideo() {
        // TODO: Implement video editing using FFmpeg
        Toast.makeText(
            this,
            "Video editing will be implemented with FFmpeg library",
            Toast.LENGTH_LONG
        ).show()
        
        // Placeholder for actual implementation
        /*
        videoUri?.let { uri ->
            val inputPath = getRealPathFromUri(uri) ?: return
            val videosDir = File(filesDir, "media/videos")
            if (!videosDir.exists()) {
                videosDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(videosDir, "edited_$timestamp.mp4")
            
            val startSeconds = trimStart / 1000.0
            val duration = (trimEnd - trimStart) / 1000.0
            
            val quality = binding.spinnerQuality.selectedItemPosition
            val scale = when (quality) {
                1 -> "scale=1920:1080" // High
                2 -> "scale=1280:720"  // Medium
                3 -> "scale=854:480"   // Low
                else -> ""             // Original
            }
            
            val scaleFilter = if (scale.isNotEmpty()) "-vf $scale" else ""
            
            // FFmpeg command
            val command = "-i $inputPath -ss $startSeconds -t $duration $scaleFilter -c:v libx264 -c:a aac ${outputFile.absolutePath}"
            
            FFmpegKit.executeAsync(command) { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    runOnUiThread {
                        Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to save video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        */
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        binding.videoView.stopPlayback()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
