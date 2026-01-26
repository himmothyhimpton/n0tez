package com.n0tez.app

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.n0tez.app.databinding.ActivityAudioEditorBinding
import java.io.File
import java.io.IOException

class AudioEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAudioEditorBinding
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var isPlaying = false
    private var trimStart: Int = 0 // in milliseconds
    private var trimEnd: Int = 0 // in milliseconds
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val currentPosition = player.currentPosition
                    binding.seekBarProgress.progress = currentPosition
                    updateTimeDisplay(currentPosition, player.duration)
                    
                    // Check if we've reached the trim end point
                    if (trimEnd > 0 && currentPosition >= trimEnd) {
                        player.seekTo(trimStart)
                    }
                    
                    handler.postDelayed(this, 100)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")
        
        setupUI()
        loadAudio()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Audio Editor"
        
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }
        
        binding.btnStop.setOnClickListener {
            stopPlayback()
        }
        
        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
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
        
        binding.btnSave.setOnClickListener {
            saveEditedAudio()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun loadAudio() {
        audioFilePath?.let { path ->
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    
                    val duration = this.duration
                    binding.seekBarProgress.max = duration
                    trimEnd = duration
                    
                    binding.rangeSliderTrim.valueFrom = 0f
                    binding.rangeSliderTrim.valueTo = duration.toFloat()
                    binding.rangeSliderTrim.values = listOf(0f, duration.toFloat())
                    
                    updateTimeDisplay(0, duration)
                    updateTrimDisplay()
                }
                
                Toast.makeText(this, "Audio loaded", Toast.LENGTH_SHORT).show()
                
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load audio: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "No audio file specified", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                player.pause()
                isPlaying = false
                binding.btnPlayPause.text = "Play"
                binding.btnPlayPause.setIconResource(R.drawable.ic_add)
                handler.removeCallbacks(updateProgressRunnable)
            } else {
                // If at the end, restart from trim start
                if (player.currentPosition >= trimEnd || player.currentPosition >= player.duration) {
                    player.seekTo(trimStart)
                }
                
                player.start()
                isPlaying = true
                binding.btnPlayPause.text = "Pause"
                binding.btnPlayPause.setIconResource(R.drawable.ic_close)
                handler.post(updateProgressRunnable)
            }
        }
    }
    
    private fun stopPlayback() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                player.pause()
                isPlaying = false
                binding.btnPlayPause.text = "Play"
                binding.btnPlayPause.setIconResource(R.drawable.ic_add)
                handler.removeCallbacks(updateProgressRunnable)
            }
            player.seekTo(trimStart)
            binding.seekBarProgress.progress = trimStart
            updateTimeDisplay(trimStart, player.duration)
        }
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
    
    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun saveEditedAudio() {
        // TODO: Implement audio trimming using FFmpeg or similar
        // For now, just show a message
        Toast.makeText(
            this,
            "Audio editing will be implemented with FFmpeg library",
            Toast.LENGTH_LONG
        ).show()
        
        // Placeholder for actual implementation
        /*
        val inputPath = audioFilePath ?: return
        val outputPath = inputPath.replace(".m4a", "_edited.m4a")
        
        val startSeconds = trimStart / 1000.0
        val duration = (trimEnd - trimStart) / 1000.0
        
        // FFmpeg command: ffmpeg -i input.m4a -ss START -t DURATION -c copy output.m4a
        val command = "-i $inputPath -ss $startSeconds -t $duration -c copy $outputPath"
        
        // Execute FFmpeg command
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                runOnUiThread {
                    Toast.makeText(this, "Audio saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to save audio", Toast.LENGTH_SHORT).show()
                }
            }
        }
        */
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
