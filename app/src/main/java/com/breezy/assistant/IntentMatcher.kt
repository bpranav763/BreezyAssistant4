package com.breezy.assistant

class IntentMatcher {
  
   enum class IntentType {
       BATTERY_QUERY, TEMPERATURE_QUERY, CHARGING_QUERY,
       WIFI_ON, WIFI_OFF, DND_ON, DND_OFF,
       VOLUME_UP, VOLUME_DOWN, BRIGHTNESS_UP, BRIGHTNESS_DOWN,
       WIFI_CHECK, STORAGE_CHECK, CRISIS_HUMAN, STRESS_ANXIETY, SPEED_TEST, SPAM_CHECK, HELP,
       GREETING, SEARCH, UNKNOWN
   }

   data class MatchResult(val type: IntentType, val confidence: Float, val originalInput: String = "")

   private val STOP_WORDS = setOf(
       "breezy","my","the","a","is","it","how",
       "what","can","you","i","me","please","pls","karo",
       "kar","do","de","bata","batao","kya","hai","hain"
   )

   private val BATTERY_KEYWORDS = setOf(
       "battery","charge","charging","percent","%","power",
       "kitna","kitni","batri","charj","drain","draining","low"
   )
   private val TEMP_KEYWORDS = setOf(
       "hot","heat","temperature","warm","heating",
       "overheat","burning","garam","temp"
   )
   private val WIFI_ON_KEYWORDS = setOf("wifi on","wi-fi on","turn on wifi","enable wifi","wifi chalu")
   private val WIFI_OFF_KEYWORDS = setOf("wifi off","wi-fi off","turn off wifi","disable wifi","wifi band")
   private val DND_ON_KEYWORDS = setOf("dnd on","do not disturb","silent","quiet","mute","dnd chalu")
   private val DND_OFF_KEYWORDS = setOf("dnd off","disturb off","unmute","dnd band")
   private val GREETING_KEYWORDS = setOf("hi","hello","hey","sup","wassup","namaste","hola")

   private val SEARCH_KEYWORDS = setOf(
       "search","google","find","who is","what is",
       "weather in","news","check","latest","tell me about"
   )

   private val WIFI_CHECK_KEYWORDS = setOf(
       "wifi safe","wifi check","network safe","is my wifi",
       "connection safe","network check","wifi secure"
   )
   private val STORAGE_KEYWORDS = setOf(
       "storage","space","memory","full","store",
       "jagah","jagha","disk","gb","mb","free space"
   )
   private val SPEED_KEYWORDS = setOf(
       "speed","fast","slow","internet speed","network speed",
       "speed test","kitna fast","connection speed"
   )
   private val SPAM_KEYWORDS = setOf(
       "spam","block","fraud","unknown number","number check",
       "scam","kaun hai","kisne call kiya","check number"
   )
   private val HELP_KEYWORDS = setOf(
       "help","commands","what can","kya kar","options",
       "features","what do","capabilities"
   )
   private val STRESS_KEYWORDS = setOf(
       "stressed","anxious","anxiety","overwhelmed","panic",
       "pressure","tension","stres","pareshan","tension"
   )
   private val CRISIS_HUMAN_KEYWORDS = setOf(
       "kill myself","end my life","suicide","want to die",
       "don't want to live","no reason to live","end it all",
       "can't take it","khud ko khatam","jeena nahi",
       "mar jaana","savanum","depressed","i give up",
       "nobody cares","i'm done","i hate my life"
   )

   fun match(input: String): MatchResult {
       val lower = input.lowercase().trim()
       val tokens = lower.split(' ',',','?','!','.').filter {
           it.isNotEmpty() && it !in STOP_WORDS
       }

       return when {
           CRISIS_HUMAN_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.CRISIS_HUMAN, 1.0f, input)
           WIFI_CHECK_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.WIFI_CHECK, 0.9f, input)
           WIFI_ON_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.WIFI_ON, 0.95f, input)
           WIFI_OFF_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.WIFI_OFF, 0.95f, input)
           DND_ON_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.DND_ON, 0.95f, input)
           DND_OFF_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.DND_OFF, 0.95f, input)
           STORAGE_KEYWORDS.any { it in tokens } -> MatchResult(IntentType.STORAGE_CHECK, 0.9f, input)
           STRESS_KEYWORDS.any { lower.contains(it) } -> MatchResult(IntentType.STRESS_ANXIETY, 0.9f, input)
           SPEED_KEYWORDS.any { it in tokens } -> MatchResult(IntentType.SPEED_TEST, 0.85f, input)
           HELP_KEYWORDS.any { it in tokens } -> MatchResult(IntentType.HELP, 0.9f, input)
           tokens.any { it in TEMP_KEYWORDS } -> MatchResult(IntentType.TEMPERATURE_QUERY, 0.9f, input)
           tokens.any { it in BATTERY_KEYWORDS } -> MatchResult(IntentType.BATTERY_QUERY, 0.9f, input)
           tokens.any { it in GREETING_KEYWORDS } -> MatchResult(IntentType.GREETING, 0.85f, input)
           SEARCH_KEYWORDS.any { lower.startsWith(it) } -> MatchResult(IntentType.SEARCH, 0.6f, input)
           else -> MatchResult(IntentType.UNKNOWN, 0.1f, input)
       }
   }
}
