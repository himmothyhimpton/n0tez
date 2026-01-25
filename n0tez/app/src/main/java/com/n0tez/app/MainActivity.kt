package com.n0tez.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.n0tez.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var pendingWidgetStart = false
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasOverlayPermission()) {
            if (pendingWidgetStart) {
                pendingWidgetStart = false
                startFloatingWidget()
            }
        } else {
            Toast.makeText(this, "Overlay permission is required for the floating widget", Toast.LENGTH_LONG).show()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission helps keep the widget running", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkNotificationPermission()
    }
    
    private fun setupUI() {
        // Card click handlers
        binding.cardCreateNote.setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }
        
        binding.cardFloatingWidget.setOnClickListener {
            handleFloatingWidgetClick()
        }
        
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.cardMyNotes.setOnClickListener {
            startActivity(Intent(this, NotesListActivity::class.java))
        }
    }
    
    private fun handleFloatingWidgetClick() {
        if (hasOverlayPermission()) {
            startFloatingWidget()
        } else {
            showOverlayPermissionDialog()
        }
    }
    
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Building-Block needs permission to draw over other apps so the floating notepad can appear on top of your screen. This is essential for the transparent overlay feature.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingWidgetStart = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun startFloatingWidget() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Floating widget started! Look for the bubble icon.", Toast.LENGTH_SHORT).show()
        
        // Minimize app so user can see the widget
        moveTaskToBack(true)
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
