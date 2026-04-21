package com.breezy.assistant

import android.content.Context
import androidx.core.content.edit

class ChatHistoryManager(private val context: Context) {

    data class ChatSession(val id: String, val title: String, val timestamp: Long)
    data class Message(val sender: String, val text: String, val timestamp: Long)

    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val msgPrefs = context.getSharedPreferences("chat_messages", Context.MODE_PRIVATE)

    fun getAllSessions(): List<ChatSession> {
        val sessions = mutableListOf<ChatSession>()
        prefs.all.keys.filter { it.startsWith("session_title_") }.forEach { key ->
            val id = key.removePrefix("session_title_")
            val title = prefs.getString(key, "Untitled Chat") ?: "Untitled Chat"
            val timestamp = prefs.getLong("session_time_$id", 0L)
            sessions.add(ChatSession(id, title, timestamp))
        }
        return sessions.sortedByDescending { it.timestamp }
    }

    fun saveSession(id: String, title: String) {
        prefs.edit {
            putString("session_title_$id", title)
            putLong("session_time_$id", System.currentTimeMillis())
        }
    }

    fun saveMessage(sessionId: String, sender: String, text: String) {
        val currentMsgs = getMessages(sessionId).toMutableList()
        currentMsgs.add(Message(sender, text, System.currentTimeMillis()))
        
        // Simple serialization: sender|text||sender|text
        val serialized = currentMsgs.joinToString("||") { "${it.sender}|${it.text.replace("|", " ").replace("||", " ")}" }
        msgPrefs.edit {
            putString("messages_$sessionId", serialized)
        }
    }

    fun getMessages(sessionId: String): List<Message> {
        val serialized = msgPrefs.getString("messages_$sessionId", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split("||").mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) {
                Message(parts[0], parts[1], 0L) // Timestamp not fully reconstructed here for simplicity
            } else null
        }
    }

    fun deleteSession(id: String) {
        prefs.edit {
            remove("session_title_$id")
            remove("session_time_$id")
        }
        msgPrefs.edit {
            remove("messages_$id")
        }
    }
}
