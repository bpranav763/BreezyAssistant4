# Breezy Assistant 4 🌬️

Breezy is a highly customizable, privacy-focused Android assistant that uses a hybrid AI system to balance speed, intelligence, and offline capability.

## 🧠 AI Architecture (The "Brain")

Breezy uses a multi-layered fallback system to ensure you always get a response, even without internet or an API key.

### 1. The Fallback Order
1.  **Safety Layer (Constitutional AI)**: Checks for jailbreaks, medical advice, or political sensitivity.
2.  **Crisis Detection**: Instant human-centric responses for mental health emergencies.
3.  **Gemini AI (Online)**: High-speed, high-intelligence processing via Google Gemini 1.5 Flash.
4.  **Groq AI (Online)**: Instant fallback using Llama 3 via Groq.
5.  **Local LLM (Offline)**: On-device processing using a quantized MobileLLM model (requires NDK).
6.  **Rule Engine**: Handles system commands (Battery, WiFi, Storage, etc.) using Regex.
7.  **Response Pool**: Pre-set "smart casual" responses for greetings and unknown inputs when offline.

## 🛠️ Key Components

### `ResponseEngine.kt`
The central nervous system. It decides which "brain" to use based on your query, connectivity, and settings. It implements the "Hybrid Logic" which differentiates between casual talk and analytical questions.

### `LLMInference.kt`
The bridge to the local C++ model. It manages the `breezy_brain.gguf` file and runs inference directly on your phone's CPU/GPU.

### `GeminiEngine.kt` & `GroqEngine.kt`
Simple, lightweight API wrappers that handle cloud-based intelligence when the local model is too slow or unavailable.

### `BreezyMemory.kt`
Handles all persistence. It stores your API keys, personality tone, user profile, and custom joystick configurations securely in SharedPreferences.

### `FloatingCircleService.kt`
The UI layer that keeps Breezy accessible over any app. It handles the bubble physics, the joystick menu, and the chat interface.

## ⚙️ How to Configure
- **Hybrid Mode**: Breezy uses casual responses for "Hi/Hello" and switches to AI for everything else.
- **Pure Local**: Disables cloud APIs for maximum privacy.
- **API Keys**: Get a free Gemini key from Google AI Studio and a Groq key from Groq Console to enable cloud features.

## 🚀 Build Info
- **Language**: 100% Kotlin
- **Model**: MobileLLM-125M (Q4_K_M quantization)
- **Minimum SDK**: 26 (Android 8.0)
