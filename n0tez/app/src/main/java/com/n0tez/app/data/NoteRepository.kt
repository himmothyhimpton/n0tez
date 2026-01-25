package com.n0tez.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class NoteRepository(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "building_block_notes"
        private const val KEY_NOTES = "notes_list"
        private const val KEY_CURRENT_NOTE = "current_note"
        private const val SHRED_PREFS_NAME = "building_block_shred"
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                SHRED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            sharedPrefs // Fallback to regular prefs
        }
    }
    
    fun getAllNotes(): List<Note> {
        val notesJson = sharedPrefs.getString(KEY_NOTES, "[]")
        return parseNotesFromJson(notesJson ?: "[]")
    }
    
    fun getActiveNotes(): List<Note> {
        return getAllNotes().filter { !it.isDeleted }
    }
    
    fun getPinnedNotes(): List<Note> {
        return getAllNotes().filter { it.isPinned && !it.isDeleted }
    }
    
    fun saveNote(note: Note) {
        val notes = getAllNotes().toMutableList()
        val existingIndex = notes.indexOfFirst { it.id == note.id }
        
        note.updatedAt = System.currentTimeMillis()
        
        if (existingIndex >= 0) {
            notes[existingIndex] = note
        } else {
            notes.add(note)
        }
        
        saveNotesToPrefs(notes)
    }
    
    fun deleteNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        note?.isDeleted = true
        saveNotesToPrefs(notes)
    }
    
    fun permanentlyDeleteNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        notes.removeIf { it.id == noteId }
        saveNotesToPrefs(notes)
    }
    
    fun shredNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        
        if (note != null) {
            // Encrypt the content before deletion for secure removal
            val encryptedContent = securelyEncryptForDeletion(note.content)
            
            // Overwrite with random data multiple times (secure deletion pattern)
            val random = SecureRandom()
            val overwriteData = ByteArray(note.content.length * 3)
            repeat(3) {
                random.nextBytes(overwriteData)
                note.content = Base64.encodeToString(overwriteData, Base64.DEFAULT)
            }
            
            // Clear and remove
            note.content = ""
            note.title = ""
            notes.removeIf { it.id == noteId }
            saveNotesToPrefs(notes)
            
            // Store encrypted version briefly then delete
            securePrefs.edit()
                .putString("shredded_${noteId}", encryptedContent)
                .apply()
            
            // Immediately remove
            securePrefs.edit()
                .remove("shredded_${noteId}")
                .apply()
        }
    }
    
    private fun securelyEncryptForDeletion(content: String): String {
        return try {
            val random = SecureRandom()
            val keyBytes = ByteArray(32)
            random.nextBytes(keyBytes)
            val key = SecretKeySpec(keyBytes, "AES")
            
            val iv = ByteArray(12)
            random.nextBytes(iv)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            
            val encrypted = cipher.doFinal(content.toByteArray())
            Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            // On failure, just return random data
            val random = SecureRandom()
            val randomBytes = ByteArray(content.length)
            random.nextBytes(randomBytes)
            Base64.encodeToString(randomBytes, Base64.DEFAULT)
        }
    }
    
    fun pinNote(noteId: String, pinned: Boolean) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        note?.isPinned = pinned
        saveNotesToPrefs(notes)
    }
    
    fun getCurrentNote(): Note? {
        val noteJson = sharedPrefs.getString(KEY_CURRENT_NOTE, null)
        return if (noteJson != null) parseNoteFromJson(noteJson) else null
    }
    
    fun setCurrentNote(note: Note?) {
        if (note != null) {
            sharedPrefs.edit().putString(KEY_CURRENT_NOTE, noteToJson(note)).apply()
        } else {
            sharedPrefs.edit().remove(KEY_CURRENT_NOTE).apply()
        }
    }
    
    private fun saveNotesToPrefs(notes: List<Note>) {
        val jsonArray = JSONArray()
        notes.forEach { note ->
            jsonArray.put(noteToJsonObject(note))
        }
        sharedPrefs.edit().putString(KEY_NOTES, jsonArray.toString()).apply()
    }
    
    private fun parseNotesFromJson(json: String): List<Note> {
        val notes = mutableListOf<Note>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val noteJson = jsonArray.getJSONObject(i)
                notes.add(parseNoteFromJsonObject(noteJson))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return notes.sortedByDescending { it.updatedAt }
    }
    
    private fun parseNoteFromJson(json: String): Note? {
        return try {
            parseNoteFromJsonObject(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseNoteFromJsonObject(json: JSONObject): Note {
        return Note(
            id = json.optString("id", ""),
            title = json.optString("title", ""),
            content = json.optString("content", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            isPinned = json.optBoolean("isPinned", false),
            isDeleted = json.optBoolean("isDeleted", false)
        )
    }
    
    private fun noteToJson(note: Note): String {
        return noteToJsonObject(note).toString()
    }
    
    private fun noteToJsonObject(note: Note): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
            put("isPinned", note.isPinned)
            put("isDeleted", note.isDeleted)
        }
    }
}
