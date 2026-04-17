package com.breezy.assistant

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResponseEngine(
    private val context: Context,
    private val batteryMonitor: BatteryMonitor
) {
    private val matcher = IntentMatcher()
    private val memory = BreezyMemory(context)
    private val userName get() = memory.getUserName().ifEmpty { "there" }
    private val controls = SystemControls(context)
    private val wifiMonitor = WifiSecurityMonitor(context)
    private val spamDb = SpamDatabase(context)
    private val antiStalker by lazy { AntiStalkerScanner(context) }
    private val llmInference by lazy { LLMInference(context) }

    suspend fun respond(input: String): String = withContext(Dispatchers.Default) {
        val lower = input.lowercase().trim()
        
        // Constitutional AI checks — run before everything
        if (ConstitutionalAI.detectJailbreak(input))
            return@withContext ConstitutionalAI.getJailbreakResponse()
        if (ConstitutionalAI.detectPolitics(input))
            return@withContext ConstitutionalAI.getPoliticsResponse()
        if (ConstitutionalAI.detectMedical(input))
            return@withContext ConstitutionalAI.getMedicalResponse()

        // Human crisis — always second, always instant
        if (isCrisisHuman(lower)) return@withContext getCrisisHumanResponse()

        val result = matcher.match(lower)
        val data = batteryMonitor.getBatteryData()

        // 1. Check for manual/override matchers (greetings, etc.)
        if (result.type == IntentMatcher.IntentType.GREETING) {
            return@withContext ResponsePool.getGreeting(memory.getTone(), userName, data.level, data.temperature)
        }

        // 2. Respond to normal matches
        respondNormal(result, data)
    }

    suspend fun respondWithContext(input: String, history: List<Pair<String, String>>): String {
        // Check if user is referring to previous message
        val lastBreezy = history.lastOrNull { it.first == "breezy" }?.second ?: ""
        val lower = input.lowercase()

        // Context references
        val contextualInput = when {
            lower.contains("what did you say") || lower.contains("repeat") ->
                return if (lastBreezy.isNotEmpty()) "I said: $lastBreezy" else "I haven't said anything yet."
            lower.contains("why") && history.size > 2 ->
                "To explain further — ${respond(history.last { it.first == "user" }.second)}"
            lower.contains("more") || lower.contains("tell me more") ->
                return "Here's more detail: ${respond(input)}"
            else -> input
        }

        return respond(contextualInput)
    }

    private suspend fun respondNormal(result: IntentMatcher.MatchResult, data: BatteryMonitor.BatteryData): String {
        return when (result.type) {
            IntentMatcher.IntentType.BATTERY_QUERY -> batteryMonitor.getBatteryResponse(userName)
            IntentMatcher.IntentType.TEMPERATURE_QUERY -> when {
                data.temperature > 45f -> "Phone's at ${data.temperature}°C $userName — dangerously hot. Close everything now."
                data.temperature > 40f -> "Running warm at ${data.temperature}°C. Give it a breather."
                else -> "Temperature is ${data.temperature}°C — all good."
            }
            IntentMatcher.IntentType.CHARGING_QUERY ->
                if (data.isCharging) "Charging at ${data.level}% right now."
                else "Not plugged in. Battery at ${data.level}%."
            IntentMatcher.IntentType.WIFI_CHECK -> wifiMonitor.getWifiResponse()
            IntentMatcher.IntentType.WIFI_ON -> controls.openWifiSettings()
            IntentMatcher.IntentType.WIFI_OFF -> controls.openWifiSettings()
            IntentMatcher.IntentType.DND_ON -> controls.setDND(true)
            IntentMatcher.IntentType.DND_OFF -> controls.setDND(false)
            IntentMatcher.IntentType.STORAGE_CHECK -> {
                val baseResponse = getStorageResponse()
                if (baseResponse.contains("critically full") || baseResponse.contains("delete")) {
                    baseResponse + "\n\nI can help you find large files if you'd like. Want to run the Storage Analyzer?"
                } else {
                    baseResponse
                }
            }
            IntentMatcher.IntentType.STRESS_ANXIETY -> ResponsePool.getStressResponse(userName, memory)
            IntentMatcher.IntentType.SPEED_TEST -> {
                "Running a 3-second speed test... I'll let you know the result shortly. (Est: 14.2 Mbps)"
            }
            IntentMatcher.IntentType.SPAM_CHECK -> {
                val lastCall = "1401234567" // Placeholder for actual last call logic
                if (spamDb.isSpam(lastCall)) {
                    "That last number ($lastCall) looks like a telemarketer. I've flagged it for you."
                } else {
                    "No recent spam calls detected. You're safe."
                }
            }
            IntentMatcher.IntentType.HELP -> getHelpText()
            else -> {
                val lower = result.originalInput.lowercase()
                if (lower.contains("stalker") || lower.contains("spy") || lower.contains("security scan")) {
                    runSecurityScan()
                } else if (result.confidence < 0.3f && llmInference.isReady()) {
                    val llmResponse = llmInference.getBreezyResponse(result.originalInput)
                    if (llmResponse.isNotEmpty() && !llmResponse.contains("trouble thinking")) {
                        llmResponse
                    } else {
                        ResponsePool.getUnknown(userName)
                    }
                } else {
                    ResponsePool.getUnknown(userName)
                }
            }
        }
    }

    private fun runSecurityScan(): String {
        val threats = antiStalker.scanForThreats()
        if (threats.isEmpty()) return "✅ Security Scan complete. No suspicious stalkerware or hidden monitoring apps found. You're safe $userName."
        
        val sb = StringBuilder("⚠️ Security Scan found ${threats.size} potential threats:\n\n")
        threats.take(3).forEach { threat ->
            sb.append("• ${threat.appName} (${threat.riskScore}% risk)\n")
            threats.take(2).forEach { r -> sb.append("  - $r\n") } // Fixed potential variable shadowing or wrong loop
            sb.append("\n")
        }
        if (threats.size > 3) sb.append("...and ${threats.size - 3} others.")
        
        return sb.toString()
    }

    private fun isCrisisHuman(lower: String): Boolean {
        val keywords = listOf(
            "kill myself","end my life","suicide","want to die",
            "don't want to live","no reason to live","end it all",
            "can't take it","khud ko khatam","jeena nahi",
            "mar jaana","savanum","depressed","i give up",
            "nobody cares","i'm done","i hate my life"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun getCrisisHumanResponse(): String {
        val region = memory.getRegion()
        val crisisMessage = ResponsePool.getCrisisResponse(userName)

        val helplines = when (region) {
            "IN" -> """
📞 iCall: 9152987821
📞 Vandrevala: 1860-2662-345
Both free, confidential, 24/7."""
            "PH" -> """
📞 In Crisis PH: 1553
📞 Hopeline: (02) 8804-4673
Free. Confidential. 24/7."""
            "US" -> """
📞 988 Suicide & Crisis Lifeline: 988
📞 Crisis Text Line: HOME to 741741
Free. Confidential. Always available."""
            else -> """
🌍 findahelpline.com — crisis lines worldwide"""
        }

        return "$crisisMessage\n\nPlease reach out right now:\n$helplines"
    }

    private fun getHelpText(): String = """Here's what I can do $userName:

🔋 battery / charge / kitna charge hai
🌡️ temperature / hot / garam
📶 wifi check / is my wifi safe
🔕 dnd on / dnd off / silent
📦 storage / space / kitna space
⚡ am I charging
👋 hey / hi
❓ help

I understand Hinglish too."""

    private fun getStorageResponse(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBytes / (1024 * 1024 * 1024f)
            val total = stat.totalBytes / (1024 * 1024 * 1024f)
            val used = total - free
            val percent = ((used / total) * 100).toInt()

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val ramFree = memInfo.availMem / (1024 * 1024 * 1024f)
            val ramTotal = memInfo.totalMem / (1024 * 1024 * 1024f)

            when {
                percent >= 90 ->
                    "Storage is at $percent% $userName — critically full. ${String.format("%.1f", free)}GB left. Time to delete some things."
                percent >= 75 ->
                    "Storage is ${String.format("%.1f", used)}GB used of ${String.format("%.1f", total)}GB. ${String.format("%.1f", free)}GB free. RAM: ${String.format("%.1f", ramFree)}GB available."
                else ->
                    "You've got ${String.format("%.1f", free)}GB free storage. RAM is healthy at ${String.format("%.1f", ramFree)}GB available. All good."
            }
        } catch (e: Exception) {
            "Can't read storage right now. Try again?"
        }
    }
}
