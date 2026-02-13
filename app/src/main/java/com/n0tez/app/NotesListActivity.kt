package com.n0tez.app

import android.content.DialogInterface
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
import java.util.Date
import java.util.Locale

class NotesListActivity : AppCompatActivity() {

    private lateinit var noteRepository: NoteRepository
    private lateinit var adapter: NotesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var isPinVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pin Check
        if (PinLockActivity.isPinEnabled(this) && !isPinVerified) {
            val intent = Intent(this, PinLockActivity::class.java)
            intent.putExtra("SET_PIN", false)
            startActivityForResult(intent, REQUEST_PIN_VERIFICATION)
            return
        }
        
        setupActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PIN_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                isPinVerified = true
                setupActivity()
            } else {
                finish()
            }
        }
    }

    private fun setupActivity() {
        setContentView(R.layout.activity_notes_list)
        noteRepository = NoteRepository(this)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Notes"

        recyclerView = findViewById(R.id.recycler_notes)
        emptyView = findViewById(R.id.empty_view)

        adapter = NotesAdapter(
            onNoteClick = { openNote(it) },
            onPinClick = { togglePin(it) },
            onDeleteClick = { confirmDelete(it) },
            onShredClick = { confirmShred(it) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        if (::noteRepository.isInitialized) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        if (!::noteRepository.isInitialized) return
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
        onBackPressed()
        return true
    }

    companion object {
        private const val REQUEST_PIN_VERIFICATION = 1001
    }

    class NotesAdapter(
        private val onNoteClick: (Note) -> Unit,
        private val onPinClick: (Note) -> Unit,
        private val onDeleteClick: (Note) -> Unit,
        private val onShredClick: (Note) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

        private var notes: List<Note> = emptyList()

        fun submitList(newNotes: List<Note>) {
            this.notes = newNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
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
                
                val color = if (note.isPinned) {
                    // Try to get primary color or fallback
                    try {
                        itemView.context.getColor(R.color.md_theme_primary)
                    } catch (e: Exception) {
                        android.graphics.Color.BLUE
                    }
                } else {
                    android.graphics.Color.GRAY
                }
                btnPin.setColorFilter(color)

                itemView.setOnClickListener { onNoteClick(note) }
                btnPin.setOnClickListener { onPinClick(note) }
                btnDelete.setOnClickListener { onDeleteClick(note) }
                btnShred.setOnClickListener { onShredClick(note) }
            }
        }
    }
}
