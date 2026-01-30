package com.n0tez.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class FloatingWidgetService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingBubbleView: View? = null
    private var floatingNotepadView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var notepadParams: WindowManager.LayoutParams? = null
    
    // Use atomic boolean for thread-safe state management
    private val isNotepadExpanded = AtomicBoolean(false)
    private val isTransitioning = AtomicBoolean(false)
    
    private var transparencyLevel: Int = 20 // 0-100, lower = more transparent
    private var currentNote: Note? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var noteRepository: NoteRepository
    private var autoSaveJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.n0tez.app.STOP_WIDGET"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        noteRepository = NoteRepository(this)
        
        // Load saved transparency level
        transparencyLevel = getSharedPreferences("faceshot_buildingblock_prefs", MODE_PRIVATE)
            .getInt("widget_transparency", 20)
        
        // Start as foreground service
        startForegroundService()
        
        // Create the floating bubble
        createFloatingBubble()
    }
    
    private fun startForegroundService() {
        val stopIntent = Intent(this, FloatingWidgetService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, N0tezApplication.CHANNEL_ID)
            .setContentTitle("FaceShot-BuildingBlock Active")
            .setContentText("Tap to open app, or use floating bubble")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createFloatingBubble() {
        try {
            val inflater = LayoutInflater.from(this)
            floatingBubbleView = inflater.inflate(R.layout.floating_bubble, null)
            
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            bubbleParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }
            
            setupBubbleDragAndTap()
            windowManager.addView(floatingBubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Failed to create bubble: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupBubbleDragAndTap() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        
        floatingBubbleView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams?.x ?: 0
                    initialY = bubbleParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        bubbleParams?.x = initialX + deltaX.toInt()
                        bubbleParams?.y = initialY + deltaY.toInt()
                        try {
                            windowManager.updateViewLayout(floatingBubbleView, bubbleParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // It's a tap - toggle notepad (thread-safe)
                        toggleNotepad()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun toggleNotepad() {
        // Prevent rapid toggling - use atomic check-and-set
        if (!isTransitioning.compareAndSet(false, true)) {
            return // Already transitioning, ignore
        }
        
        mainHandler.post {
            try {
                if (isNotepadExpanded.get()) {
                    closeNotepadInternal()
                } else {
                    openNotepadInternal()
                }
            } finally {
                // Allow next transition after a short delay to prevent spam
                mainHandler.postDelayed({
                    isTransitioning.set(false)
                }, 300)
            }
        }
    }
    
    private fun openNotepadInternal() {
        // Clean up any existing notepad view first
        cleanupNotepadViewSafely()
        
        try {
            val inflater = LayoutInflater.from(this)
            val newNotepadView = inflater.inflate(R.layout.floating_notepad, null)
            
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            val newParams = WindowManager.LayoutParams(
                dpToPx(320),
                dpToPx(400),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (bubbleParams?.x ?: 50) + dpToPx(60)
                y = bubbleParams?.y ?: 200
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            
            // Add view to window manager
            windowManager.addView(newNotepadView, newParams)
            
            // Only set state after successful add
            floatingNotepadView = newNotepadView
            notepadParams = newParams
            isNotepadExpanded.set(true)
            
            // Setup UI after view is added
            setupNotepadUI()
            
            // Update bubble appearance
            floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 0.5f
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Failed to open notepad: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            // Clean up on failure
            cleanupNotepadViewSafely()
        }
    }
    
    private fun cleanupNotepadViewSafely() {
        floatingNotepadView?.let { view ->
            try {
                // Try to hide keyboard first
                view.findViewById<EditText>(R.id.notepad_edit_text)?.let { editText ->
                    hideKeyboard(editText)
                }
            } catch (e: Exception) {
                // Ignore keyboard errors
            }
            
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View might not be attached
            }
        }
        
        // Reset state
        floatingNotepadView = null
        notepadParams = null
        isNotepadExpanded.set(false)
    }
    
    private fun setupNotepadUI() {
        val notepadView = floatingNotepadView ?: return
        
        try {
            val editText = notepadView.findViewById<EditText>(R.id.notepad_edit_text)
            val transparencySeekBar = notepadView.findViewById<SeekBar>(R.id.transparency_seekbar)
            val btnClose = notepadView.findViewById<ImageButton>(R.id.btn_close_notepad)
            val btnSave = notepadView.findViewById<ImageButton>(R.id.btn_save_note)
            val btnPin = notepadView.findViewById<ImageButton>(R.id.btn_pin_note)
            val btnNew = notepadView.findViewById<ImageButton>(R.id.btn_new_note)
            val btnDelete = notepadView.findViewById<ImageButton>(R.id.btn_delete_note)
            val btnShred = notepadView.findViewById<ImageButton>(R.id.btn_shred_note)
            val headerBar = notepadView.findViewById<View>(R.id.header_bar)
            val transparencyLabel = notepadView.findViewById<TextView>(R.id.transparency_label)
            
            // Check all views are found
            if (editText == null || transparencySeekBar == null || btnClose == null || 
                btnSave == null || btnPin == null || btnNew == null || 
                btnDelete == null || btnShred == null || headerBar == null || 
                transparencyLabel == null) {
                android.widget.Toast.makeText(this, "Error: Missing UI elements", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Set initial transparency
            updateTransparency(transparencyLevel)
            transparencySeekBar.progress = transparencyLevel
            transparencyLabel.text = "${100 - transparencyLevel}%"
            
            // Load current note or create new
            currentNote = try {
                noteRepository.getCurrentNote() ?: Note()
            } catch (e: Exception) {
                Note()
            }
            editText.setText(currentNote?.content ?: "")
            updatePinButton(btnPin)
            
            // Setup transparency slider
            transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    transparencyLevel = progress
                    updateTransparency(progress)
                    transparencyLabel.text = "${100 - progress}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Save preference
                    getSharedPreferences("faceshot_buildingblock_prefs", MODE_PRIVATE)
                        .edit()
                        .putInt("widget_transparency", transparencyLevel)
                        .apply()
                }
            })
            
            // Setup text change listener for auto-save
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    autoSaveJob?.cancel()
                    autoSaveJob = serviceScope.launch {
                        delay(1500)
                        saveCurrentNote()
                    }
                }
            })
            
            // Focus on edit text to show keyboard
            editText.setOnClickListener {
                editText.requestFocus()
                showKeyboard(editText)
            }
            
            // Close button
            btnClose.setOnClickListener {
                saveCurrentNote()
                toggleNotepad()
            }
            
            // Save button
            btnSave.setOnClickListener {
                saveCurrentNote()
                android.widget.Toast.makeText(this@FloatingWidgetService, "Note saved", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            // Pin button
            btnPin.setOnClickListener {
                currentNote?.let { note ->
                    note.isPinned = !note.isPinned
                    saveCurrentNote()
                    updatePinButton(btnPin)
                    val msg = if (note.isPinned) "Note pinned" else "Note unpinned"
                    android.widget.Toast.makeText(this@FloatingWidgetService, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            // New note button
            btnNew.setOnClickListener {
                saveCurrentNote()
                currentNote = Note()
                editText.setText("")
                noteRepository.setCurrentNote(currentNote)
                updatePinButton(btnPin)
            }
            
            // Delete button
            btnDelete.setOnClickListener {
                currentNote?.let { note ->
                    noteRepository.deleteNote(note.id)
                    android.widget.Toast.makeText(this@FloatingWidgetService, "Note deleted", android.widget.Toast.LENGTH_SHORT).show()
                    currentNote = Note()
                    editText.setText("")
                    noteRepository.setCurrentNote(currentNote)
                    updatePinButton(btnPin)
                }
            }
            
            // Shred button (secure delete)
            btnShred.setOnClickListener {
                currentNote?.let { note ->
                    noteRepository.shredNote(note.id)
                    android.widget.Toast.makeText(this@FloatingWidgetService, "Note securely shredded", android.widget.Toast.LENGTH_SHORT).show()
                    currentNote = Note()
                    editText.setText("")
                    noteRepository.setCurrentNote(currentNote)
                    updatePinButton(btnPin)
                }
            }
            
            // Setup drag for header bar
            setupNotepadDrag(headerBar)
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this@FloatingWidgetService, "UI setup error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTransparency(level: Int) {
        floatingNotepadView?.apply {
            val editTextBackground = findViewById<View>(R.id.edit_text_background)
            
            // Only the backdrop/background is transparent
            // level: 0 = fully transparent, 100 = opaque
            val alpha = level / 100f
            editTextBackground?.alpha = alpha
        }
    }
    
    private fun updatePinButton(btnPin: ImageButton) {
        val isPinned = currentNote?.isPinned ?: false
        btnPin.setColorFilter(
            if (isPinned) Color.parseColor("#FFD700") else Color.WHITE
        )
    }
    
    private fun setupNotepadDrag(headerBar: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        headerBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = notepadParams?.x ?: 0
                    initialY = notepadParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    notepadParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    notepadParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        floatingNotepadView?.let { view ->
                            windowManager.updateViewLayout(view, notepadParams)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun saveCurrentNote() {
        try {
            floatingNotepadView?.findViewById<EditText>(R.id.notepad_edit_text)?.let { editText ->
                currentNote?.content = editText.text.toString()
                currentNote?.let { note ->
                    if (note.content.isNotBlank()) {
                        noteRepository.saveNote(note)
                        noteRepository.setCurrentNote(note)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun closeNotepadInternal() {
        try {
            saveCurrentNote()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        cleanupNotepadViewSafely()
        
        // Restore bubble appearance
        try {
            floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 1f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        saveCurrentNote()
        autoSaveJob?.cancel()
        serviceScope.cancel()
        
        floatingNotepadView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) { }
        }
        floatingBubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) { }
        }
        
        floatingNotepadView = null
        floatingBubbleView = null
        isNotepadExpanded.set(false)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
