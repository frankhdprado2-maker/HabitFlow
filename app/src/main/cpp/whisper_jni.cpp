#include <jni.h>
#include <whisper.h>

#include <atomic>
#include <mutex>
#include <string>
#include <vector>

namespace {
std::mutex g_mutex;
whisper_context * g_context = nullptr;
std::string g_model_path;
std::atomic_bool g_cancelled{false};

void throw_state(JNIEnv * env, const char * message) {
    jclass clazz = env->FindClass("java/lang/IllegalStateException");
    if (clazz != nullptr) env->ThrowNew(clazz, message);
}

bool should_abort(void *) { return g_cancelled.load(std::memory_order_relaxed); }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_unmsm_habitflow_voice_whisper_WhisperNative_initializeModel(
    JNIEnv * env, jobject, jstring model_path
) {
    if (model_path == nullptr) {
        throw_state(env, "MODEL_NOT_FOUND");
        return -1;
    }
    const char * chars = env->GetStringUTFChars(model_path, nullptr);
    if (chars == nullptr) return -1;
    const std::string requested(chars);
    env->ReleaseStringUTFChars(model_path, chars);

    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_context != nullptr && g_model_path == requested) return 0;
    if (g_context != nullptr) {
        whisper_free(g_context);
        g_context = nullptr;
        g_model_path.clear();
    }
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    params.flash_attn = false;
    g_context = whisper_init_from_file_with_params(requested.c_str(), params);
    if (g_context == nullptr) {
        throw_state(env, "MODEL_INCOMPATIBLE");
        return -2;
    }
    g_model_path = requested;
    g_cancelled.store(false, std::memory_order_relaxed);
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_unmsm_habitflow_voice_whisper_WhisperNative_transcribe(
    JNIEnv * env, jobject, jfloatArray samples, jstring language, jint thread_count
) {
    if (samples == nullptr || env->GetArrayLength(samples) == 0) {
        throw_state(env, "EMPTY_AUDIO");
        return nullptr;
    }
    const char * language_chars = language == nullptr ? nullptr : env->GetStringUTFChars(language, nullptr);
    const std::string language_value = language_chars == nullptr ? "es" : language_chars;
    if (language_chars != nullptr) env->ReleaseStringUTFChars(language, language_chars);
    const jsize sample_count = env->GetArrayLength(samples);
    std::vector<float> pcm(static_cast<size_t>(sample_count));
    env->GetFloatArrayRegion(samples, 0, sample_count, pcm.data());
    if (env->ExceptionCheck()) return nullptr;

    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_context == nullptr) {
        throw_state(env, "MODEL_NOT_INITIALIZED");
        return nullptr;
    }
    g_cancelled.store(false, std::memory_order_relaxed);
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = thread_count < 1 ? 1 : thread_count;
    params.translate = false;
    params.no_context = true;
    params.no_timestamps = true;
    params.single_segment = false;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.suppress_blank = true;
    params.suppress_nst = true;
    params.language = language_value.c_str();
    params.initial_prompt = "Frases breves sobre habitos diarios en espanol.";
    params.abort_callback = should_abort;

    const int result = whisper_full(g_context, params, pcm.data(), static_cast<int>(pcm.size()));
    if (g_cancelled.load(std::memory_order_relaxed)) {
        throw_state(env, "INFERENCE_CANCELLED");
        return nullptr;
    }
    if (result != 0) {
        throw_state(env, "TRANSCRIPTION_FAILED");
        return nullptr;
    }
    std::string text;
    const int segments = whisper_full_n_segments(g_context);
    for (int i = 0; i < segments; ++i) {
        const char * segment = whisper_full_get_segment_text(g_context, i);
        if (segment != nullptr) text.append(segment);
    }
    return env->NewStringUTF(text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_unmsm_habitflow_voice_whisper_WhisperNative_cancelTranscription(JNIEnv *, jobject) {
    g_cancelled.store(true, std::memory_order_relaxed);
}

extern "C" JNIEXPORT void JNICALL
Java_com_unmsm_habitflow_voice_whisper_WhisperNative_releaseModel(JNIEnv *, jobject) {
    g_cancelled.store(true, std::memory_order_relaxed);
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_context != nullptr) {
        whisper_free(g_context);
        g_context = nullptr;
        g_model_path.clear();
    }
}
