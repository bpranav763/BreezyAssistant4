package com.breezy.assistant

import android.content.Context

object ResponsePool {

   val CRISIS_RESPONSES = listOf(
       "I'm here for you, {name}. Take a deep breath. You're not alone.",
       "Deep breaths, {name}. Whatever is happening, we'll get through this together.",
       "I'm listening, {name}. I'm right here with you.",
       "You're doing great just by reaching out. I'm here.",
       "Take all the time you need. I'm not going anywhere.",
       "It's okay to not be okay right now. I'm here to support you.",
       "Inhale for 4, hold for 4, exhale for 4. Let's do it together.",
       "Your feelings are valid, {name}. I'm here to listen.",
       "Focus on my voice. You are safe. You are cared for.",
       "We can take this one second at a time if we have to.",
       "You have survived 100% of your hardest days. You can do this.",
       "Tell me what's on your mind. I'm all ears.",
       "I'm sending you strength, {name}. I'm here for you.",
       "You matter, {name}. Your life matters. I'm here.",
       "Let's just stay here for a moment until things feel a bit lighter.",
       "If you need someone to talk to, I'm always here.",
       "You don't have to carry this all by yourself.",
       "I'm here to support you in any way I can.",
       "Keep breathing, {name}. You're doing so well.",
       "You're not alone in this. I'm right here.",
       "Take a moment to just be. I'm here with you.",
       "Everything else can wait. Right now, it's just us.",
       "You are stronger than you feel right now.",
       "I'm here. I'm listening. I care.",
       "Let's breathe through this together.",
       "I'm so glad you reached out to me.",
       "You are safe here.",
       "Take your time. There's no rush.",
       "I'm here to hold space for you.",
       "You're going to be okay. I'm with you."
   )

   val LATE_NIGHT_RESPONSES = listOf(
       "Still awake, {name}? I'm here if you need to talk.",
       "Late night thoughts? I'm listening.",
       "The world is quiet, but I'm here for you, {name}.",
       "Can't sleep? Let's chat for a bit.",
       "I'm here, {name}. You're not alone in the dark.",
       "Late nights can be tough. I'm glad you reached out.",
       "The stars are out, and I'm right here with you.",
       "It's late, but my ears are open.",
       "Whatever is keeping you up, we can talk about it.",
       "I'm your late-night companion, {name}.",
       "Rest will come, but until then, I'm here.",
       "You're safe here, even in the middle of the night.",
       "Need a distraction or a listener? I'm both.",
       "The night is long, but I'm here until the sun comes up.",
       "Just a reminder that you're cared for, even at this hour.",
       "Quiet moments are sometimes the loudest. I'm here.",
       "I'm awake because you are. What's on your mind?",
       "Don't worry about the time. I'm here for as long as you need.",
       "Let's keep each other company.",
       "You're never alone, {name}. Not even at 3 AM."
   )

   val STRESS_RESPONSES = listOf(
       "Sounds like things are pretty intense right now. Take a breath.",
       "That's a lot to handle. I'm here to help you decompress.",
       "Let's take a step back together. Deep breath in...",
       "I can feel the stress from here. Let's try to slow down.",
       "You're carrying a lot right now. I'm here to listen.",
       "One thing at a time, {name}. We'll figure it out.",
       "It's okay to feel overwhelmed. I'm here for you.",
       "Let's pause for a second. You're doing your best.",
       "Breathe. You've got this, and I've got you.",
       "Stress is just a visitor. It doesn't have to stay.",
       "Tell me more. Venting helps.",
       "I'm here to help you carry the load.",
       "Let's find a small win together.",
       "You're not in this alone, {name}.",
       "Breathe out the tension. I'm right here."
   )

   val GREETING_FRIENDLY = listOf(
       "Hello {name}! How can I help you today?",
       "Hi {name}, hope you're having a great day!",
       "Hey there {name}, what's on your mind?",
       "Good to see you, {name}!",
       "Hi {name}, I'm here if you need anything.",
       "Hello {name}, how are things going?",
       "Hey {name}, ready for another day?"
   )

   val GREETING_WITTY = listOf(
       "Oh look, it's my favorite human, {name}!",
       "Greetings {name}! I've been waiting for you.",
       "Hey {name}, did you bring snacks? Just kidding, I'm an app.",
       "Hello {name}! Ready to conquer the world? Or just the morning?",
       "Hi {name}! My circuits are buzzing with excitement to see you.",
       "Greetings {name}! What's the plan for world domination today?",
       "Hey {name}, I was just thinking about how awesome you are."
   )

   val GREETING_PROFESSIONAL = listOf(
       "Hello {name}. How may I assist you at this time?",
       "Greetings {name}. I am ready for your instructions.",
       "Good day {name}. How can I be of service?",
       "Hello {name}. I am here to assist with your tasks.",
       "Greetings {name}. Please let me know if you require any assistance.",
       "Hello {name}. I am at your disposal.",
       "Good day {name}. Ready to proceed with our objectives."
   )

   val GREETING_CALM = listOf(
       "Peace be with you, {name}. How are you feeling?",
       "Hello {name}. Let's take a moment of calm together.",
       "Hi {name}. I hope you're finding some quiet today.",
       "Greetings {name}. I'm here to support your peace of mind.",
       "Hello {name}. Breathe in, breathe out. I'm here.",
       "Hi {name}. Let's approach today with tranquility.",
       "Greetings {name}. I'm here whenever you need a calm space."
   )

   val UNKNOWN_RESPONSES = listOf(
       "I'm not sure I understand, but I'm here for you.",
       "Could you tell me more about that?",
       "I'm listening, even if I don't have all the answers.",
       "Tell me more, {name}.",
       "I want to understand. Can you explain that a bit more?",
       "I'm here, and I'm listening.",
       "I'm still learning, but I'm always here for you."
   )

   fun fill(template: String, name: String, level: Int = 1, temp: Float = 20f, battery: String = "100%", time: String = "12:00"): String {
       return template.replace("{name}", name)
           .replace("{level}", level.toString())
           .replace("{temp}", temp.toString())
           .replace("{battery}", battery)
           .replace("{time}", time)
   }

   fun getGreeting(tone: String, name: String, level: Int, temp: Float): String {
       val pool = when (tone) {
           "witty" -> GREETING_WITTY
           "professional" -> GREETING_PROFESSIONAL
           "calm" -> GREETING_CALM
           else -> GREETING_FRIENDLY
       }
       return fill(pool.random(), name, level, temp)
   }

   fun getUnknown(name: String) = fill(UNKNOWN_RESPONSES.random(), name)

   fun getCrisisResponse(name: String) = fill(CRISIS_RESPONSES.random(), name)

   fun getLateNightCheckIn(name: String, time: String) = fill(LATE_NIGHT_RESPONSES.random(), name, time = time)

   fun getStressResponse(name: String, memory: BreezyMemory): String {
       val lastTime = memory.getStressCooldown()
       val now = System.currentTimeMillis()
       val hours48 = 48 * 60 * 60 * 1000L

       return if (now - lastTime > hours48) {
           memory.saveStressCooldown(now)
           fill(STRESS_RESPONSES.random(), name)
       } else {
           // "I'm still here" fallback when on cooldown
           "{name}, I'm still here with you. Take another slow breath.".replace("{name}", name)
       }
   }

   fun getGreetingWithCooldown(context: Context, tone: String, name: String, level: Int, temp: Float): String {
       val cooldown = ResponseCooldown(context)
       val pool = when (tone) {
           "witty" -> GREETING_WITTY
           "professional" -> GREETING_PROFESSIONAL
           "calm" -> GREETING_CALM
           else -> GREETING_FRIENDLY
       }
       return fill(cooldown.getResponse(pool, "greeting_$tone", name), name, level, temp)
   }

   fun getCrisisWithCooldown(context: Context, name: String): String {
       val cooldown = ResponseCooldown(context)
       return fill(cooldown.getResponse(CRISIS_RESPONSES, "crisis", name), name)
   }
}
