package com.n0tez.app.data

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isPinned: Boolean = false,
    var isDeleted: Boolean = false
) {
    fun getPreviewText(): String {
        return if (content.length > 50) {
            content.substring(0, 50) + "..."
        } else {
            content
        }
    }
    
    fun getDisplayTitle(): String {
        return if (title.isNotBlank()) {
            title
        } else if (content.isNotBlank()) {
            content.lines().firstOrNull()?.take(30) ?: "Untitled Note"
        } else {
            "Untitled Note"
        }
    }
}
