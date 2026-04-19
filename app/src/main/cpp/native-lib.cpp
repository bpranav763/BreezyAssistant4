#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#define TAG "BreezyNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// This will eventually hold the llama_context from the real library
struct BreezyBrain {
    void* model_data;
    size_t model_size;
    int fd;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_breezy_assistant_LLMInference_initModel(JNIEnv* env, jobject /* this */, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    LOGI("Attempting to map brain file: %s", path);

    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        LOGE("Failed to open model file!");
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }

    // Get file size
    size_t size = lseek(fd, 0, SEEK_END);
    lseek(fd, 0, SEEK_SET);

    // Memory map the 90MB file (This is how real AI stays fast)
    void* data = mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (data == MAP_FAILED) {
        LOGE("Mmap failed!");
        close(fd);
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }

    BreezyBrain* brain = new BreezyBrain();
    brain->model_data = data;
    brain->model_size = size;
    brain->fd = fd;

    LOGI("Brain mapped successfully: %zu bytes at %p", size, data);

    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(brain);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_breezy_assistant_LLMInference_generateResponse(JNIEnv* env, jobject /* this */, jlong modelPtr, jstring prompt) {
    BreezyBrain* brain = reinterpret_cast<BreezyBrain*>(modelPtr);
    if (!brain || !brain->model_data) {
        return env->NewStringUTF("Error: Brain not initialized.");
    }

    const char* input = env->GetStringUTFChars(prompt, nullptr);

    // ----------------------------------------------------------------------
    // REAL LLM LOGIC GOES HERE
    // Once we link llama.cpp, we call: llama_decode(brain->ctx, ...)
    // For now, we confirm the data is actually there:
    // ----------------------------------------------------------------------

    std::string response = "I have successfully mapped your 90MB brain file in memory. ";
    response += "I'm ready to link the llama.cpp headers to start actual inference.";

    env->ReleaseStringUTFChars(prompt, input);
    return env->NewStringUTF(response.c_str());
}
