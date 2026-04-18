package com.breezy.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SearchEngine {

    suspend fun searchDuckDuckGo(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            // Using DDG's 'html' or 'lite' version for easier parsing without API
            val url = URL("https://html.duckduckgo.com/html/?q=$encodedQuery")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val html = connection.inputStream.bufferedReader().readText()
            
            // Extract the first few results using basic string parsing
            // This is "poor man's parsing" to avoid adding heavy JSoup dependencies
            val results = mutableListOf<String>()
            var currentPos = 0
            
            for (i in 1..3) {
                val snippetStart = html.indexOf("result__snippet", currentPos)
                if (snippetStart == -1) break
                
                val textStart = html.indexOf(">", snippetStart) + 1
                val textEnd = html.indexOf("</a>", textStart)
                
                if (textEnd > textStart) {
                    val snippet = html.substring(textStart, textEnd)
                        .replace(Regex("<[^>]*>"), "") // Remove tags
                        .replace("&amp;", "&")
                        .trim()
                    if (snippet.length > 10) results.add(snippet)
                }
                currentPos = snippetStart + 20
            }

            if (results.isEmpty()) return@withContext "I couldn't find anything specific on the web for that right now."
            
            return@withContext "According to my web search: " + results.joinToString(" ").take(300) + "..."
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }
}
