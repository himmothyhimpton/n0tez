package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import java.text.SimpleDateFormat
import java.util.*

class NotesListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: NotesAdapter
    private lateinit var noteRepository: NoteRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_list)
        
        noteRepository = NoteRepository(this)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Notes"
        
        recyclerView = findViewById(R.id.recycler_notes)
        emptyView = findViewById(R.id.empty_view)
        
        adapter = NotesAdapter(
            onNoteClick = { note -> openNote(note) },
            onPinClick = { note -> togglePin(note) },
            onDeleteClick = { note -> confirmDelete(note) },
            onShredClick = { note -> confirmShred(note) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    override fun onResume() {
        super.onResume()
        loadNotes()
    }
    
    private fun loadNotes() {
        val notes = noteRepository.getActiveNotes()
        adapter.submitList(notes)
        
        if (notes.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun openNote(note: Note) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        intent.putExtra("NOTE_ID", note.id)
        startActivity(intent)
    }
    
    private fun togglePin(note: Note) {
        noteRepository.pinNote(note.id, !note.isPinned)
        loadNotes()
    }
    
    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                noteRepository.deleteNote(note.id)
                loadNotes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmShred(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Shred Note")
            .setMessage("This will securely delete the note by encrypting and overwriting it multiple times. This cannot be undone.")
            .setPositiveButton("Shred") { _, _ ->
                noteRepository.shredNote(note.id)
                loadNotes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    // Adapter class
    inner class NotesAdapter(
        private val onNoteClick: (Note) -> Unit,
        private val onPinClick: (Note) -> Unit,
        private val onDeleteClick: (Note) -> Unit,
        private val onShredClick: (Note) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {
        
        private var notes: List<Note> = emptyList()
        
        fun submitList(newNotes: List<Note>) {
            // Sort: pinned first, then by updated date
            notes = newNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return NoteViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            holder.bind(notes[position])
        }
        
        override fun getItemCount() = notes.size
        
        inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.note_title)
            private val previewText: TextView = itemView.findViewById(R.id.note_preview)
            private val dateText: TextView = itemView.findViewById(R.id.note_date)
            private val pinIndicator: View = itemView.findViewById(R.id.pin_indicator)
            private val btnPin: ImageButton = itemView.findViewById(R.id.btn_pin)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
            private val btnShred: ImageButton = itemView.findViewById(R.id.btn_shred)
            
            fun bind(note: Note) {
                titleText.text = note.getDisplayTitle()
                previewText.text = note.getPreviewText()
                
                val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                dateText.text = dateFormat.format(Date(note.updatedAt))
                
                pinIndicator.visibility = if (note.isPinned) View.VISIBLE else View.GONE
                
                btnPin.setColorFilter(
                    if (note.isPinned) 
                        resources.getColor(R.color.md_theme_primary, null)
                    else 
                        resources.getColor(R.color.md_theme_onSurfaceVariant, null)
                )
                
                itemView.setOnClickListener { onNoteClick(note) }
                btnPin.setOnClickListener { onPinClick(note) }
                btnDelete.setOnClickListener { onDeleteClick(note) }
                btnShred.setOnClickListener { onShredClick(note) }
            }
        }
    }
}
