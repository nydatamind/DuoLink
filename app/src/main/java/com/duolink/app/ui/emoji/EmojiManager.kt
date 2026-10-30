package com.duolink.app.ui.emoji

import android.content.Context
import android.content.SharedPreferences

class EmojiManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "duolink_emoji_prefs"
        private const val KEY_RECENTS = "recent_emojis"
        private const val MAX_RECENTS = 30
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecentEmojis(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, "") ?: ""
        if (raw.isBlank()) {
            return listOf("❤️", "🔥", "✨", "🦉", "😂", "🥰", "👍", "🙏", "💯", "😊", "🎉", "😘")
        }
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun addRecentEmoji(emoji: String) {
        val current = getRecentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        if (current.size > MAX_RECENTS) {
            current.removeAt(current.size - 1)
        }
        prefs.edit().putString(KEY_RECENTS, current.joinToString(",")).apply()
    }

    fun searchEmojis(query: String): List<EmojiItem> {
        if (query.isBlank()) return emptyList()
        val lower = query.trim().lowercase()
        return EmojiCatalog.ALL_EMOJIS.filter { item ->
            item.name.contains(lower) || item.emoji == query
        }
    }
}

