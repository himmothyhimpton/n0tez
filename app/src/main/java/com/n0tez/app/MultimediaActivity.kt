package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.n0tez.app.databinding.ActivityMultimediaBinding

class MultimediaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMultimediaBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, PhotoEditorActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, VideoEditorActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }
    }

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

        binding.cardVoiceRecorder.setOnClickListener {
            startActivity(Intent(this, VoiceRecorderActivity::class.java))
        }

        binding.cardPhotoEditor.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.cardVideoEditor.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.cardMediaGallery.setOnClickListener {
            startActivity(Intent(this, MediaGalleryActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
