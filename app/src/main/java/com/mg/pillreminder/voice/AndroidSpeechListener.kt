package com.mg.pillreminder.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.mg.pillreminder.domain.voice.ListenResult
import com.mg.pillreminder.domain.voice.SpeechListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/** `SpeechRecognizer`는 메인 스레드에서만 만들고 쓸 수 있어서 [listen] 전체를 메인에서 돈다. */
class AndroidSpeechListener @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SpeechListener {

    override suspend fun listen(): ListenResult = withContext(Dispatchers.Main.immediate) {
        when {
            !hasRecordAudioPermission() -> ListenResult.PermissionDenied
            !SpeechRecognizer.isRecognitionAvailable(context) -> ListenResult.Unavailable
            // 주변 소음이 있으면 인식기가 끝을 못 찾고 계속 기다린다. 사용자가 다시 누르게 시간 제한을 둔다.
            else -> withTimeoutOrNull(LISTEN_TIMEOUT_MILLIS) { recognizeOnce() } ?: ListenResult.NoSpeech
        }
    }

    private suspend fun recognizeOnce(): ListenResult {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        // 완료와 취소 어느 쪽이든 인식기를 놓아야 마이크가 풀린다.
        return try {
            listenWith(recognizer)
        } finally {
            recognizer.destroy()
        }
    }

    private suspend fun listenWith(recognizer: SpeechRecognizer): ListenResult = suspendCancellableCoroutine { continuation ->
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                finish(if (text.isNullOrBlank()) ListenResult.NoSpeech else ListenResult.Heard(text))
            }

            override fun onError(error: Int) = finish(error.toListenResult())

            private fun finish(result: ListenResult) {
                if (continuation.isActive) continuation.resume(result)
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(recognizerIntent())
    }

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_TAG)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun hasRecordAudioPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun Int.toListenResult(): ListenResult = when (this) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ListenResult.NoSpeech
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ListenResult.PermissionDenied
        else -> ListenResult.Unavailable
    }

    private companion object {
        const val LANGUAGE_TAG = "ko-KR"
        const val LISTEN_TIMEOUT_MILLIS = 12_000L
    }
}
