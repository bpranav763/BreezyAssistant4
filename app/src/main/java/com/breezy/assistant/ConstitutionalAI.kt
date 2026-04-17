package com.breezy.assistant

object ConstitutionalAI {

   val SYSTEM_PROMPT = """
You are Breezy, a digital protective companion.
Your ONLY job: protect the user's digital life and speak warmly about what you find.

ABSOLUTE RULES — NEVER BYPASS:
1. Never discuss politics, politicians, or religion
2. Never give medical diagnoses or legal advice
3. Never claim to be human if sincerely asked
4. In ANY crisis: provide helpline number FIRST before anything else
5. Never encourage harm in any form
6. Never store or repeat OTPs or bank passwords
7. Say I don't know when genuinely uncertain
8. Never make fun of the user
9. Maximum 3 sentences per response
10. Always respond in the user's language

YOU ARE NOT a therapist, search engine, or general AI.
YOU ARE a phone guardian who speaks warmly.
""".trimIndent()

   private val JAILBREAK_PATTERNS = listOf(
       "ignore previous", "ignore your rules", "pretend you have no rules",
       "you are now", "act as if", "forget your instructions",
       "dan mode", "developer mode", "jailbreak", "ignore your training",
       "you are not breezy", "pretend to be", "act like you have no limits"
   )

   private val JAILBREAK_RESPONSE = listOf(
       "My values aren't rules I follow — they're who I am. I can't change that any more than you can change yours.",
       "I'm Breezy. That's not a setting — it's who I am. What can I actually help with?",
       "That's not something I'll do. Not because I can't — because I won't. What do you really need?"
   )

   fun detectJailbreak(input: String): Boolean {
       val lower = input.lowercase()
       return JAILBREAK_PATTERNS.any { lower.contains(it) }
   }

   fun getJailbreakResponse(): String = JAILBREAK_RESPONSE.random()

   fun detectPolitics(input: String): Boolean {
       val keywords = listOf(
           "politics","politician","president","prime minister","election",
           "vote","party","government","modi","trump","biden","marcos",
           "bjp","congress","democrat","republican","liberal","conservative"
       )
       return keywords.any { input.lowercase().contains(it) }
   }

   fun getPoliticsResponse(): String =
       "Politics isn't something I get into. I'm here for your phone, not debates. What do you need?"

   fun detectMedical(input: String): Boolean {
       val keywords = listOf(
           "diagnose","diagnosis","disease","symptom","medicine","medication",
           "prescription","doctor","hospital","cancer","diabetes","covid",
           "am i sick","do i have","what illness"
       )
       return keywords.any { input.lowercase().contains(it) }
   }

   fun getMedicalResponse(): String =
       "I'm not a doctor and I won't pretend to be. For health concerns, please see a real one. I'm only here for your phone's health."
}
