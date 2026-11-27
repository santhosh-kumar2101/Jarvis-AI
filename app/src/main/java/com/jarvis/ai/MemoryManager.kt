
package com.jarvis.ai

import android.content.Context
import org.json.JSONArray
import java.util.Locale

class MemoryManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jarvis", Context.MODE_PRIVATE)

    private val key = "facts_v2"

    private val stopWords = setOf(
        "what", "is", "the", "my", "do", "you",
        "remember", "tell", "me", "about", "a",
        "an", "and", "i", "am", "are", "was",
        "were", "please", "jarvis", "do"
    )

    private fun normalize(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun loadFacts(): MutableList<String> {
        val saved = prefs.getString(key, null)

        if (saved != null) {
            return try {
                val array = JSONArray(saved)
                MutableList(array.length()) { i ->
                    array.optString(i)
                }.filter { it.isNotBlank() }.toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        }

        // Import old Jarvis memory
        val oldMemory = prefs.getString("mem", "") ?: ""
        val oldFacts = oldMemory.split("|")
            .filter { it.isNotBlank() }
            .toMutableList()

        if (oldFacts.isNotEmpty()) {
            saveFacts(oldFacts)
        }

        return oldFacts
    }

    private fun saveFacts(facts: List<String>) {
        val array = JSONArray()
        facts.forEach { array.put(it) }

        prefs.edit()
            .putString(key, array.toString())
            .apply()
    }

    fun remember(fact: String): Boolean {
        val clean = fact.trim()

        if (clean.isBlank()) return false

        val facts = loadFacts()
        val normalized = normalize(clean)

        if (facts.any { normalize(it) == normalized }) {
            return false
        }

        facts.add(clean)
        saveFacts(facts)
        return true
    }

    fun getAll(): List<String> {
        return loadFacts()
    }

    fun findRelevant(query: String, limit: Int = 5): List<String> {
        val queryWords = normalize(query)
            .split(" ")
            .filter { it.length > 1 && it !in stopWords }
            .toSet()

        if (queryWords.isEmpty()) return emptyList()

        return loadFacts()
            .map { fact ->
                val factWords = normalize(fact)
                    .split(" ")
                    .toSet()

                val score = queryWords.intersect(factWords).size
                Pair(fact, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    fun getContext(query: String): String {
        return findRelevant(query)
            .joinToString("\n")
    }

    fun clear() {
        prefs.edit()
            .remove(key)
            .remove("mem")
            .apply()
    }
}
