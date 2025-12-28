package com.n0tez.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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

class FloatingWidgetService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingBubbleView: View? = null
    private var floatingNotepadView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var notepadParams: WindowManager.LayoutParams? = null
    
    private var isNotepadExpanded = false
    private var transparencyLevel: Int = 20 // 0-100, lower = more transparent
    private var currentNote: Note? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var noteRepository: NoteRepository
    private var autoSaveJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.n0tez.app.STOP_WIDGET"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        noteRepository = NoteRepository(this)
        
        // Load saved transparency level
        transparencyLevel = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
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
            .setContentTitle("n0tez Active")
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
                        windowManager.updateViewLayout(floatingBubbleView, bubbleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // It's a tap - toggle notepad
                        toggleNotepad()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun toggleNotepad() {
        if (isNotepadExpanded) {
            closeNotepad()
        } else {
            openNotepad()
        }
    }
    
    private fun openNotepad() {
        if (floatingNotepadView != null) return
        
        val inflater = LayoutInflater.from(this)
        floatingNotepadView = inflater.inflate(R.layout.floating_notepad, null)
        
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        notepadParams = WindowManager.LayoutParams(
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
        
        setupNotepadUI()
        windowManager.addView(floatingNotepadView, notepadParams)
        isNotepadExpanded = true
        
        // Update bubble appearance
        floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 0.5f
    }
    
    private fun setupNotepadUI() {
        floatingNotepadView?.apply {
            val notepadContainer = findViewById<View>(R.id.notepad_container)
            val notepadBorder = findViewById<View>(R.id.notepad_border)
            val editText = findViewById<EditText>(R.id.notepad_edit_text)
            val transparencySeekBar = findViewById<SeekBar>(R.id.transparency_seekbar)
            val btnClose = findViewById<ImageButton>(R.id.btn_close_notepad)
            val btnSave = findViewById<ImageButton>(R.id.btn_save_note)
            val btnPin = findViewById<ImageButton>(R.id.btn_pin_note)
            val btnNew = findViewById<ImageButton>(R.id.btn_new_note)
            val btnDelete = findViewById<ImageButton>(R.id.btn_delete_note)
            val btnShred = findViewById<ImageButton>(R.id.btn_shred_note)
            val headerBar = findViewById<View>(R.id.header_bar)
            val transparencyLabel = findViewById<TextView>(R.id.transparency_label)
            
            // Set initial transparency (only backdrop is transparent)
            updateTransparency(transparencyLevel)
            transparencySeekBar.progress = transparencyLevel
            transparencyLabel.text = "${100 - transparencyLevel}%"
            
            // Load current note or create new
            currentNote = noteRepository.getCurrentNote() ?: Note()
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
                    getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
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
                closeNotepad()
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
        }
    }
    
    private fun updateTransparency(level: Int) {
        floatingNotepadView?.apply {
            val notepadContainer = findViewById<View>(R.id.notepad_container)
            val editTextBackground = findViewById<View>(R.id.edit_text_background)
            
            // Only the backdrop/background is transparent
            // level: 0 = fully transparent, 100 = opaque
            val alpha = level / 100f
            editTextBackground?.alpha = alpha
            
            // Keep border and controls fully visible
            // The notepadContainer handles the border which stays solid
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
                    windowManager.updateViewLayout(floatingNotepadView, notepadParams)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun saveCurrentNote() {
        floatingNotepadView?.findViewById<EditText>(R.id.notepad_edit_text)?.let { editText ->
            currentNote?.content = editText.text.toString()
            currentNote?.let { note ->
                if (note.content.isNotBlank()) {
                    noteRepository.saveNote(note)
                    noteRepository.setCurrentNote(note)
                }
            }
        }
    }
    
    private fun closeNotepad() {
        saveCurrentNote()
        floatingNotepadView?.let {
            // Hide keyboard first
            val editText = it.findViewById<EditText>(R.id.notepad_edit_text)
            hideKeyboard(editText)
            
            windowManager.removeView(it)
        }
        floatingNotepadView = null
        isNotepadExpanded = false
        
        // Restore bubble appearance
        floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 1f
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
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
