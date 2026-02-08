package com.n0tez.app

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.n0tez.app.databinding.FloatingWidgetBinding
import kotlinx.coroutines.*

class FloatingWidgetService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var binding: FloatingWidgetBinding
    private var layoutParams: WindowManager.LayoutParams? = null
    private var transparencyLevel: Float = 0.7f
    private var currentNoteContent: String = ""
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            val notification = buildNotification()
            startForeground(1, notification)
        } catch (e: Exception) {
            android.util.Log.e("FloatingWidget", "startForeground error", e)
        }
        setupFloatingWidget()
    }
    
    private fun setupFloatingWidget() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Inflate floating widget layout
            binding = FloatingWidgetBinding.inflate(LayoutInflater.from(this))
            floatingView = binding.root
            
            // Setup layout parameters
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            
            // Position the widget
            layoutParams?.gravity = Gravity.TOP or Gravity.START
            layoutParams?.x = 100
            layoutParams?.y = 100
            
            // Add view to window manager
            windowManager.addView(floatingView, layoutParams)
            
            setupWidgetUI()
            setupDragAndDrop()
        } catch (e: Exception) {
            android.util.Log.e("FloatingWidget", "setupFloatingWidget error", e)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "n0tez_widget_channel",
                "n0tez Widget",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "n0tez_widget_channel")
                .setSmallIcon(R.drawable.ic_note)
                .setContentTitle("n0tez widget running")
                .setContentText("Tap to open editor")
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_note)
                .setContentTitle("n0tez widget running")
                .setContentText("Tap to open editor")
                .setOngoing(true)
                .build()
        }
    }
    
    private fun setupWidgetUI() {
        try {
            binding.apply {
                // Set initial transparency
                setTransparency(transparencyLevel)
                
                // Setup edit text
                floatingEditText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                floatingEditText.setTextColor(android.graphics.Color.WHITE)
                
                // Setup control buttons
                btnClose.setOnClickListener {
                    stopSelf()
                }
                
                btnMinimize.setOnClickListener {
                    floatingEditText.visibility = if (floatingEditText.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
                
                btnTransparency.setOnClickListener {
                    transparencyLevel = when (transparencyLevel) {
                        0.3f -> 0.5f
                        0.5f -> 0.7f
                        0.7f -> 0.9f
                        else -> 0.3f
                    }
                    setTransparency(transparencyLevel)
                }
                
                // Auto-save functionality
                floatingEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        saveCurrentNote()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWidget", "setupWidgetUI error", e)
            stopSelf()
        }
    }
    
    private fun setTransparency(level: Float) {
        binding.floatingContainer.alpha = level
        binding.btnClose.alpha = level + 0.2f
        binding.btnMinimize.alpha = level + 0.2f
        binding.btnTransparency.alpha = level + 0.2f
    }
    
    private fun setupDragAndDrop() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        binding.floatingContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    layoutParams?.x = initialX + deltaX
                    layoutParams?.y = initialY + deltaY
                    
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }
    }
    
    private fun saveCurrentNote() {
        try {
            val content = binding.floatingEditText.text.toString()
            if (content != currentNoteContent) {
                currentNoteContent = content
                serviceScope.launch {
                    // Save to database or shared preferences
                    saveNoteToStorage(content)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWidget", "saveCurrentNote error", e)
        }
    }
    
    private suspend fun saveNoteToStorage(content: String) {
        // Implementation for saving notes to local storage
        withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
                sharedPrefs.edit().putString("current_note", content).apply()
            } catch (e: Exception) {
                android.util.Log.e("FloatingWidget", "saveNoteToStorage error", e)
            }
        }
    }
    
    override fun onDestroy() {
        try {
            saveCurrentNote()
            serviceScope.cancel()
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWidget", "onDestroy error", e)
        }
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
