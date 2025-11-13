package com.n0tez.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.core.widget.addTextChangedListener
import com.n0tez.app.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNoteEditorBinding
    private var noteId: String? = null
    private var autoSaveJob: Job? = null
    private val autoSaveScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadNoteIfExists()
        setupAutoSave()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Note"
        
        binding.apply {
            // Setup transparency slider
            sliderTransparency.addOnChangeListener { _, value, _ ->
                val alpha = value / 100f
                noteContainer.alpha = alpha
                saveTransparencyPreference(value.toInt())
            }
            
            // Setup text editor
            noteEditText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            noteEditText.setTextColor(android.graphics.Color.WHITE)
            
            // Setup save button
            btnSave.setOnClickListener {
                saveNote()
            }
            
            // Setup share button
            btnShare.setOnClickListener {
                shareNote()
            }
            
            // Setup copy button
            btnCopy.setOnClickListener {
                copyNoteContent()
            }
            
            // Setup paste button
            btnPaste.setOnClickListener {
                pasteContent()
            }
        }
    }
    
    private fun loadNoteIfExists() {
        noteId = intent.getStringExtra("NOTE_ID")
        noteId?.let { id ->
            loadNote(id)
        }
        
        // Load saved transparency level
        val savedTransparency = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("transparency_level", 70)
        binding.sliderTransparency.value = savedTransparency.toFloat()
        binding.noteContainer.alpha = savedTransparency / 100f
    }
    
    private fun loadNote(noteId: String) {
        // Load note from database
        // This is a simplified implementation
        val noteContent = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getString("note_$noteId", "")
        
        binding.noteEditText.setText(noteContent)
        supportActionBar?.title = "Edit Note"
    }
    
    private fun setupAutoSave() {
        binding.noteEditText.addTextChangedListener { text ->
            autoSaveJob?.cancel()
            autoSaveJob = autoSaveScope.launch {
                delay(2000) // Auto-save after 2 seconds of inactivity
                saveNoteSilently()
            }
        }
    }
    
    private fun saveNote() {
        val content = binding.noteEditText.text.toString()
        if (content.isBlank()) {
            showMessage("Note is empty")
            return
        }
        
        val noteIdToSave = noteId ?: generateNoteId()
        saveNoteToStorage(noteIdToSave, content)
        
        showMessage("Note saved successfully")
        finish()
    }
    
    private fun saveNoteSilently() {
        val content = binding.noteEditText.text.toString()
        if (content.isNotBlank()) {
            val noteIdToSave = noteId ?: generateNoteId()
            saveNoteToStorage(noteIdToSave, content)
            noteId = noteIdToSave
        }
    }
    
    private fun saveNoteToStorage(noteId: String, content: String) {
        val sharedPrefs = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        
        sharedPrefs.edit().apply {
            putString("note_$noteId", content)
            putLong("note_${noteId}_timestamp", timestamp)
            apply()
        }
    }
    
    private fun shareNote() {
        val content = binding.noteEditText.text.toString()
        if (content.isNotBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, "Note from n0tez")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Note"))
        } else {
            showMessage("Nothing to share")
        }
    }
    
    private fun copyNoteContent() {
        val content = binding.noteEditText.text.toString()
        if (content.isNotBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Note", content)
            clipboard.setPrimaryClip(clip)
            showMessage("Note copied to clipboard")
        } else {
            showMessage("Nothing to copy")
        }
    }
    
    private fun pasteContent() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).text.toString()
            val currentText = binding.noteEditText.text.toString()
            val cursorPosition = binding.noteEditText.selectionStart
            
            val newText = if (cursorPosition >= 0) {
                currentText.substring(0, cursorPosition) + pastedText + 
                currentText.substring(cursorPosition)
            } else {
                currentText + pastedText
            }
            
            binding.noteEditText.setText(newText)
            binding.noteEditText.setSelection(cursorPosition + pastedText.length)
        }
    }
    
    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE).edit()
            .putInt("transparency_level", level)
            .apply()
    }
    
    private fun generateNoteId(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
        autoSaveScope.cancel()
    }
}
