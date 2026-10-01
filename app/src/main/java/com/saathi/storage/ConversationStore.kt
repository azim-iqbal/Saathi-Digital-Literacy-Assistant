package com.saathi.storage

import android.content.Context

data class StoredMessage(val text: String, val fromUser: Boolean)

/** In-process transcript only. Construction removes transcripts written by older builds. */
class ConversationStore(context: Context) {
    init { context.getSharedPreferences("saathi_conversation", Context.MODE_PRIVATE).edit().clear().apply() }
    fun load(): List<StoredMessage> = synchronized(messages) { messages.toList() }
    fun append(message: StoredMessage) = synchronized(messages) {
        if (messages.size >= 80) messages.removeAt(0)
        messages.add(message)
        Unit
    }
    fun clear() = synchronized(messages) { messages.clear() }
    private companion object { val messages = mutableListOf<StoredMessage>() }
}
