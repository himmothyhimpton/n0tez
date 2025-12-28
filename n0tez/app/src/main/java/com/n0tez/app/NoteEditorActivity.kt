package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.*

class NoteEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var noteRepository: NoteRepository
    private var currentNote: Note? = null
    private var autoSaveJob: Job? = null
    private val autoSaveScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        noteRepository = NoteRepository(this)
        
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
        val noteId = intent.getStringExtra("NOTE_ID")
        
        if (noteId != null) {
            // Load existing note
            val notes = noteRepository.getAllNotes()
            currentNote = notes.find { it.id == noteId }
            currentNote?.let { note ->
                binding.noteEditText.setText(note.content)
                supportActionBar?.title = "Edit Note"
            }
        } else {
            // Create new note
            currentNote = Note()
        }
        
        // Load saved transparency level
        val savedTransparency = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("editor_transparency", 100)
        binding.sliderTransparency.value = savedTransparency.toFloat()
        binding.noteContainer.alpha = savedTransparency / 100f
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
        
        currentNote?.content = content
        currentNote?.let { noteRepository.saveNote(it) }
        
        showMessage("Note saved successfully")
        finish()
    }
    
    private fun saveNoteSilently() {
        val content = binding.noteEditText.text.toString()
        if (content.isNotBlank()) {
            currentNote?.content = content
            currentNote?.let { noteRepository.saveNote(it) }
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
            binding.noteEditText.setSelection(
                minOf(cursorPosition + pastedText.length, newText.length)
            )
        }
    }
    
    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE).edit()
            .putInt("editor_transparency", level)
            .apply()
    }
    
    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        saveNoteSilently()
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    override fun onPause() {
        super.onPause()
        saveNoteSilently()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
        autoSaveScope.cancel()
    }
}
