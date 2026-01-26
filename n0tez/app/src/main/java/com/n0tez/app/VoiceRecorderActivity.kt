package com.n0tez.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.n0tez.app.databinding.ActivityVoiceRecorderBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecorderActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVoiceRecorderBinding
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var isPaused = false
    private var recordingStartTime: Long = 0
    private var recordingDuration: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                val elapsed = System.currentTimeMillis() - recordingStartTime + recordingDuration
                updateTimerDisplay(elapsed)
                handler.postDelayed(this, 100)
            }
        }
    }
    
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "Audio recording permission is required", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Voice Recorder"
        
        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                checkPermissionAndRecord()
            } else {
                stopRecording()
            }
        }
        
        binding.btnPause.setOnClickListener {
            if (isRecording) {
                togglePauseRecording()
            }
        }
        
        binding.btnSave.setOnClickListener {
            saveRecording()
        }
        
        binding.btnDiscard.setOnClickListener {
            discardRecording()
        }
        
        updateUIState()
    }
    
    private fun checkPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startRecording() {
        try {
            // Create audio directory if it doesn't exist
            val audioDir = File(filesDir, "media/audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(audioDir, "recording_$timestamp.m4a")
            audioFilePath = audioFile.absolutePath
            
            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)
                
                prepare()
                start()
            }
            
            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            recordingDuration = 0
            
            handler.post(updateTimerRunnable)
            updateUIState()
            
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun togglePauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (isPaused) {
                    mediaRecorder?.resume()
                    recordingStartTime = System.currentTimeMillis()
                    isPaused = false
                    handler.post(updateTimerRunnable)
                    Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show()
                } else {
                    mediaRecorder?.pause()
                    recordingDuration += System.currentTimeMillis() - recordingStartTime
                    isPaused = true
                    Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show()
                }
                updateUIState()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to pause/resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Pause/Resume requires Android 7.0+", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            isRecording = false
            isPaused = false
            handler.removeCallbacks(updateTimerRunnable)
            
            updateUIState()
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveRecording() {
        audioFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // TODO: Implement save to note or gallery
                Toast.makeText(this, "Recording saved: ${file.name}", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Recording file not found", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun discardRecording() {
        audioFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        Toast.makeText(this, "Recording discarded", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun updateTimerDisplay(milliseconds: Long) {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val millis = (milliseconds % 1000) / 10
        
        binding.tvTimer.text = String.format("%02d:%02d.%02d", minutes, remainingSeconds, millis)
    }
    
    private fun updateUIState() {
        when {
            isRecording && !isPaused -> {
                binding.btnRecord.text = "Stop"
                binding.btnRecord.setIconResource(R.drawable.ic_close)
                binding.btnPause.isEnabled = true
                binding.btnPause.text = "Pause"
                binding.btnSave.isEnabled = false
                binding.btnDiscard.isEnabled = false
                binding.waveformView.visibility = android.view.View.VISIBLE
            }
            isRecording && isPaused -> {
                binding.btnRecord.text = "Stop"
                binding.btnRecord.setIconResource(R.drawable.ic_close)
                binding.btnPause.isEnabled = true
                binding.btnPause.text = "Resume"
                binding.btnSave.isEnabled = false
                binding.btnDiscard.isEnabled = false
            }
            !isRecording && audioFilePath != null -> {
                binding.btnRecord.text = "Record"
                binding.btnRecord.setIconResource(R.drawable.ic_add)
                binding.btnPause.isEnabled = false
                binding.btnSave.isEnabled = true
                binding.btnDiscard.isEnabled = true
                binding.waveformView.visibility = android.view.View.GONE
            }
            else -> {
                binding.btnRecord.text = "Record"
                binding.btnRecord.setIconResource(R.drawable.ic_add)
                binding.btnPause.isEnabled = false
                binding.btnSave.isEnabled = false
                binding.btnDiscard.isEnabled = false
                binding.waveformView.visibility = android.view.View.GONE
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        handler.removeCallbacks(updateTimerRunnable)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
