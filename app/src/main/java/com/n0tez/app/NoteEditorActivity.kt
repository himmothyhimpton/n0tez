package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.*
import java.util.*

class NoteEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var noteRepository: NoteRepository
    private var currentNote: Note? = null
    private var noteId: String? = null
    private var autoSaveJob: Job? = null
    private val autoSaveScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        noteRepository = NoteRepository(this)
        noteId = intent.getStringExtra("NOTE_ID")
        
        setupUI()
        loadNote()
        setupAutoSave()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (noteId == null) "New Note" else "Edit Note"
        
        binding.apply {
            // Transparency
            sliderTransparency.addOnChangeListener { _, value, _ ->
                val alpha = value / 100f
                noteContainer.alpha = alpha
                saveTransparencyPreference(value.toInt())
            }
            
            // Buttons
            btnSave.setOnClickListener { saveNote(finishAfter = true) }
            btnShare.setOnClickListener { shareNote() }
            btnCopy.setOnClickListener { copyNoteContent() }
            btnPaste.setOnClickListener { pasteContent() }
            
            // Styling
            noteEditText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            noteEditText.setTextColor(android.graphics.Color.WHITE)
        }
        
        // Load transparency pref
        val savedTransparency = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("transparency_level", 70)
        binding.sliderTransparency.value = savedTransparency.toFloat()
        binding.noteContainer.alpha = savedTransparency / 100f
    }
    
    private fun loadNote() {
        if (noteId != null) {
            val notes = noteRepository.getAllNotes()
            currentNote = notes.find { it.id == noteId }
            currentNote?.let {
                binding.noteEditText.setText(it.content)
            }
        }
    }
    
    private fun setupAutoSave() {
        binding.noteEditText.addTextChangedListener {
            autoSaveJob?.cancel()
            autoSaveJob = autoSaveScope.launch {
                delay(2000)
                saveNote(finishAfter = false)
            }
        }
    }
    
    private fun saveNote(finishAfter: Boolean) {
        val content = binding.noteEditText.text.toString()
        if (content.isBlank() && currentNote == null) {
            if (finishAfter) {
                Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        if (currentNote == null) {
            // Create new
            val title = extractTitle(content)
            currentNote = Note(
                content = content,
                title = title
            )
            noteRepository.saveNote(currentNote!!)
            noteId = currentNote!!.id
        } else {
            // Update
            currentNote!!.content = content
            currentNote!!.title = extractTitle(content)
            currentNote!!.updatedAt = System.currentTimeMillis()
            noteRepository.saveNote(currentNote!!)
        }
        
        if (finishAfter) {
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun extractTitle(content: String): String {
        val lines = content.lines()
        return if (lines.isNotEmpty()) lines[0].take(50) else "Untitled"
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
        }
    }
    
    private fun copyNoteContent() {
        val content = binding.noteEditText.text.toString()
        if (content.isNotBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Note", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pasteContent() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).text.toString()
            val start = binding.noteEditText.selectionStart.coerceAtLeast(0)
            val end = binding.noteEditText.selectionEnd.coerceAtLeast(0)
            binding.noteEditText.text?.replace(Math.min(start, end), Math.max(start, end), pastedText)
        }
    }
    
    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE).edit()
            .putInt("transparency_level", level)
            .apply()
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
