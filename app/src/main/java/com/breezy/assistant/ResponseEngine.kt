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
    private val matcher      = IntentMatcher()
    private val memory       = BreezyMemory(context)
    private val controls     = SystemControls(context)
    private val wifiMonitor  = WifiSecurityMonitor(context)
    private val spamDb       = SpamDatabase(context)
    private val inspector    = HardwareInspector(context)
    private val antiStalker  by lazy { AntiStalkerScanner(context) }
    private val llm          by lazy { LLMInference(context) }
    private val search       by lazy { SearchEngine() }

    private val userName get() = memory.getUserName().ifEmpty { "there" }

    // ── Main entry point ────────────────────────────────────────────────────

    suspend fun respond(input: String): String = withContext(Dispatchers.Default) {

        // Layer 0: Constitutional AI
        if (ConstitutionalAI.detectJailbreak(input))  return@withContext ConstitutionalAI.getJailbreakResponse()
        if (ConstitutionalAI.detectPolitics(input))   return@withContext ConstitutionalAI.getPoliticsResponse()
        if (ConstitutionalAI.detectMedical(input))    return@withContext ConstitutionalAI.getMedicalResponse()

        // Layer 1: Crisis — always instant, always first
        if (isCrisisHuman(input.lowercase())) return@withContext getCrisisResponse()

        val result = matcher.match(input.lowercase().trim())
        val data   = batteryMonitor.getBatteryData()

        if (result.type == IntentMatcher.IntentType.GREETING)
            return@withContext ResponsePool.getGreeting(memory.getTone(), userName, data.level, data.temperature)

        // Layer 2: Rule engine (known intents)
        val ruleResponse = tryRuleEngine(result, data)
        if (ruleResponse != null) return@withContext ruleResponse

        // Layer 3: Local LLM
        if (llm.isReady()) {
            val llmResponse = llm.generate(input)
            if (llmResponse.isNotEmpty()) return@withContext llmResponse
        }

        // Layer 4: Response pool (warm, curated)
        ResponsePool.getUnknown(userName)
    }

    suspend fun respondWithContext(input: String, history: List<Pair<String, String>>): String {
        val lastBreezy = history.lastOrNull { it.first == "breezy" }?.second ?: ""
        val lower = input.lowercase()
        return when {
            (lower.contains("what did you say") || lower.contains("repeat")) && lastBreezy.isNotEmpty() ->
                "I said: $lastBreezy"
            lower.contains("tell me more") || lower.contains("explain more") ->
                respond(history.lastOrNull { it.first == "user" }?.second ?: input)
            else -> respond(input)
        }
    }

    // ── Rule engine ─────────────────────────────────────────────────────────

    private suspend fun tryRuleEngine(
        result: IntentMatcher.MatchResult,
        data: BatteryMonitor.BatteryData
    ): String? {
        return when (result.type) {
            IntentMatcher.IntentType.BATTERY_QUERY -> batteryMonitor.getBatteryResponse(userName)
            IntentMatcher.IntentType.TEMPERATURE_QUERY -> when {
                data.temperature > 45f -> "Phone is at ${data.temperature}°C — dangerously hot. Close everything now."
                data.temperature > 40f -> "Running warm at ${data.temperature}°C. Give it a break."
                else -> "Temperature is ${data.temperature}°C — healthy."
            }
            IntentMatcher.IntentType.CHARGING_QUERY ->
                if (data.isCharging) "Charging at ${data.level}%." else "Not charging. Battery at ${data.level}%."
            IntentMatcher.IntentType.WIFI_CHECK  -> wifiMonitor.getWifiResponse()
            IntentMatcher.IntentType.WIFI_ON     -> controls.openWifiSettings()
            IntentMatcher.IntentType.WIFI_OFF    -> controls.openWifiSettings()
            IntentMatcher.IntentType.DND_ON      -> controls.setDND(true)
            IntentMatcher.IntentType.DND_OFF     -> controls.setDND(false)
            IntentMatcher.IntentType.STORAGE_CHECK -> getStorageResponse()
            IntentMatcher.IntentType.STRESS_ANXIETY -> ResponsePool.getStressResponse(userName, memory)
            IntentMatcher.IntentType.HELP        -> getHelpText()
            IntentMatcher.IntentType.SEARCH      -> {
                val query = result.originalInput
                    .replace(Regex("^(search|find|google|what is|who is|how to|how do|tell me about)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                search.searchDuckDuckGo(query)
            }
            IntentMatcher.IntentType.UNKNOWN -> {
                val lower = result.originalInput.lowercase()
                when {
                    lower.contains("cpu") || lower.contains("processor") -> {
                        val cpu = inspector.getCpuInfo()
                        "${cpu.hardware} · ${cpu.coreCount} cores · ${cpu.maxFreqMHz} · ${cpu.abi}"
                    }
                    lower.contains("ram") || lower.contains("memory") -> {
                        val ram = inspector.getRamInfo()
                        "${ram.availableMb}MB free / ${ram.totalMb}MB (${ram.usedPercent}% used)"
                    }
                    lower.contains("storage") && (lower.contains("how") || lower.contains("much")) -> getStorageResponse()
                    lower.contains("my phone") || lower.contains("device info") || lower.contains("specs") ->
                        inspector.getDeviceSummary()
                    lower.contains("stalker") || lower.contains("spy") || lower.contains("threat scan") ->
                        runSecurityScan()
                    else -> null  // falls through to LLM
                }
            }
            else -> null
        }
    }

    private fun runSecurityScan(): String {
        val threats = antiStalker.scanForThreats()
        return if (threats.isEmpty())
            "Security scan complete. No suspicious apps detected."
        else buildString {
            append("Found ${threats.size} potential threats:\n")
            threats.take(3).forEach { append("• ${it.appName} — risk ${it.riskScore}/100\n") }
            if (threats.size > 3) append("...and ${threats.size - 3} more.")
        }
    }

    private fun isCrisisHuman(lower: String) = listOf(
        "kill myself", "end my life", "suicide", "want to die",
        "don't want to live", "no reason to live", "end it all",
        "can't take it", "khud ko khatam", "jeena nahi", "mar jaana"
    ).any { lower.contains(it) }

    private fun getCrisisResponse(): String {
        val helplines = when (memory.getRegion()) {
            "IN" -> "📞 iCall: 9152987821\n📞 Vandrevala: 1860-2662-345"
            "PH" -> "📞 In Crisis PH: 1553\n📞 Hopeline: (02) 8804-4673"
            "US" -> "📞 988 Lifeline: 988\n📞 Crisis Text: HOME to 741741"
            else -> "🌍 findahelpline.com"
        }
        return "${ResponsePool.getCrisisResponse(userName)}\n\nPlease reach out:\n$helplines"
    }

    private fun getStorageResponse(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free  = stat.availableBytes / (1024f * 1024f * 1024f)
            val total = stat.totalBytes      / (1024f * 1024f * 1024f)
            val pct   = ((total - free) / total * 100).toInt()
            when {
                pct >= 90 -> "Storage at $pct% — critically full. ${String.format("%.1f", free)}GB left. Time to delete things."
                pct >= 75 -> "${String.format("%.1f", free)}GB free of ${String.format("%.1f", total)}GB (${pct}% used)."
                else      -> "${String.format("%.1f", free)}GB free — storage healthy."
            }
        } catch (_: Exception) { "Can't read storage right now." }
    }

    private fun getHelpText() = """What I can do, $userName:

battery · temperature · charging
wifi check · wifi on/off
dnd on · dnd off
storage · speed test
search [anything]
my phone specs · cpu · ram
security scan
hey / hi

Hinglish works too."""
}
