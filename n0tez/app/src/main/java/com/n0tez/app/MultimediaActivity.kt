package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.n0tez.app.databinding.ActivityMultimediaBinding

class MultimediaActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMultimediaBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultimediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Multimedia"
        
        // Voice Recorder Card
        binding.cardVoiceRecorder.setOnClickListener {
            startActivity(Intent(this, VoiceRecorderActivity::class.java))
        }
        
        // Audio Editor Card
        binding.cardAudioEditor.setOnClickListener {
            // TODO: Show file picker for audio files
            startActivity(Intent(this, AudioEditorActivity::class.java))
        }
        
        // Photo Editor Card
        binding.cardPhotoEditor.setOnClickListener {
            startActivity(Intent(this, PhotoEditorActivity::class.java))
        }
        
        // Video Editor Card
        binding.cardVideoEditor.setOnClickListener {
            startActivity(Intent(this, VideoEditorActivity::class.java))
        }
        
        // Media Gallery Card
        binding.cardMediaGallery.setOnClickListener {
            // TODO: Implement media gallery
            android.widget.Toast.makeText(
                this,
                "Media Gallery coming soon",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
