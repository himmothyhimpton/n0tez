package com.n0tez.app

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.n0tez.app.databinding.ActivityAudioEditorBinding
import java.io.File
import java.io.IOException
import java.util.*

class AudioEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioEditorBinding
    private var audioUri: Uri? = null
    private var audioPath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var audioDuration = 0
    private var trimStart = 0f
    private var trimEnd = 0f
    private var volume = 1.0f
    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (isAudioPlaying) {
                    val currentPosition = player.currentPosition
                    binding.seekBarProgress.progress = currentPosition
                    updateTimeDisplay(currentPosition, audioDuration)
                    
                    if (trimEnd > 0 && currentPosition >= trimEnd) {
                        player.seekTo(trimStart.toInt())
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
        setupUI()

        val path = intent.getStringExtra("AUDIO_FILE_PATH")
        audioUri = intent.data

        if (audioUri != null) {
            loadAudio(audioUri!!)
        } else if (path != null) {
            loadAudio(Uri.fromFile(File(path)))
        } else {
            Toast.makeText(this, "No audio loaded", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Audio Editor"

        binding.btnPlayPause.setOnClickListener {
            if (isAudioPlaying) pauseAudio() else playAudio()
        }

        binding.btnStop.setOnClickListener { stopAudio() }
        binding.btnSave.setOnClickListener { saveAudio() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    updateTimeDisplay(progress, audioDuration)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.rangeSliderTrim.addOnChangeListener { slider, value, fromUser ->
            val values = slider.values
            trimStart = values[0]
            trimEnd = values[1]
            updateTrimInfo()
            if (fromUser) {
                mediaPlayer?.seekTo(trimStart.toInt())
                updateTimeDisplay(trimStart.toInt(), audioDuration)
            }
        }

        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadAudio(uri: Uri) {
        try {
            // Copy to temp file for FFmpeg and path access
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "temp_audio.m4a") // Assume m4a or detect ext
            val outputStream = java.io.FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            audioPath = tempFile.absolutePath
            inputStream?.close()
            outputStream.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                setOnPreparedListener { mp ->
                    audioDuration = mp.duration
                    binding.seekBarProgress.max = audioDuration
                    binding.tvTotalTime.text = formatTime(audioDuration)
                    
                    trimEnd = audioDuration.toFloat()
                    binding.rangeSliderTrim.valueFrom = 0f
                    binding.rangeSliderTrim.valueTo = audioDuration.toFloat()
                    binding.rangeSliderTrim.setValues(0f, audioDuration.toFloat())
                    updateTrimInfo()
                }
                setOnCompletionListener {
                    isAudioPlaying = false
                    binding.btnPlayPause.setIconResource(R.drawable.ic_add) // ic_play
                    binding.seekBarProgress.progress = 0
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load audio: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun playAudio() {
        mediaPlayer?.let {
            it.start()
            isAudioPlaying = true
            binding.btnPlayPause.setIconResource(R.drawable.ic_close) // ic_pause
            binding.btnPlayPause.text = "Pause"
            handler.post(updateProgressRunnable)
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            it.pause()
            isAudioPlaying = false
            binding.btnPlayPause.setIconResource(R.drawable.ic_add) // ic_play
            binding.btnPlayPause.text = "Play"
            handler.removeCallbacks(updateProgressRunnable)
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            it.pause()
            it.seekTo(0)
            isAudioPlaying = false
            binding.btnPlayPause.setIconResource(R.drawable.ic_add) // ic_play
            binding.btnPlayPause.text = "Play"
            binding.seekBarProgress.progress = 0
            handler.removeCallbacks(updateProgressRunnable)
        }
    }

    private fun updateTimeDisplay(current: Int, total: Int) {
        binding.tvCurrentTime.text = formatTime(current)
    }

    private fun updateTrimInfo() {
        val start = formatTime(trimStart.toInt())
        val end = formatTime(trimEnd.toInt())
        val duration = formatTime((trimEnd - trimStart).toInt())
        binding.tvTrimInfo.text = "Trim: $start - $end (Duration: $duration)"
    }

    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun saveAudio() {
        if (audioPath == null) return
        
        val startSec = trimStart / 1000f
        val durationSec = (trimEnd - trimStart) / 1000f
        
        val outputFile = File(filesDir, "media/audio/edited_${System.currentTimeMillis()}.m4a")
        outputFile.parentFile?.mkdirs()
        
        // Basic trim command
        // -ss start -i input -t duration -c copy output
        // Also apply volume? -filter:a "volume=X" requires re-encoding (-c:a aac)
        // If volume != 1.0, we re-encode.
        
        val cmd = if (volume == 1.0f) {
            "-ss $startSec -i \"$audioPath\" -t $durationSec -c copy \"${outputFile.absolutePath}\""
        } else {
            "-ss $startSec -i \"$audioPath\" -t $durationSec -filter:a \"volume=$volume\" -c:a aac \"${outputFile.absolutePath}\""
        }
        
        // Disable UI
        binding.btnSave.isEnabled = false
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        
        FFmpegKit.executeAsync(cmd) { session ->
            runOnUiThread {
                binding.btnSave.isEnabled = true
                if (ReturnCode.isSuccess(session.returnCode)) {
                    Toast.makeText(this, "Audio saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Save failed: ${session.failStackTrace}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacks(updateProgressRunnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
