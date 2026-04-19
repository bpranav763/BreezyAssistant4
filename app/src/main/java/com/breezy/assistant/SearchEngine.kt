package com.breezy.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class SearchEngine {

    suspend fun searchDuckDuckGo(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            // Add hardware specific search targeting
            val hardwareKeywords = listOf("fix", "spec", "problem", "battery", "screen", "repair")
            val enhancedQuery = if (hardwareKeywords.any { query.contains(it, ignoreCase = true) }) {
                "$query site:gsmarena.com OR site:ifixit.com"
            } else query
            
            val doc = Jsoup.connect("https://html.duckduckgo.com/html/?q=${URLEncoder.encode(enhancedQuery, "UTF-8")}")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(5000)
                .get()
            
            val results = doc.select(".result__snippet")
                .take(3)
                .map { it.text() }
                .filter { it.length > 10 }

            if (results.isEmpty()) return@withContext "I couldn't find anything specific on the web for '$query'."
            
            val combined = results.joinToString(" ")
            return@withContext "Search result: " + if (combined.length > 300) combined.take(300) + "..." else combined
        } catch (e: Exception) {
            "I tried searching the web but ran into an issue. You might want to check your connection."
        }
    }
}
