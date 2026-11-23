package com.mg.pillreminder.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mg.pillreminder.domain.voice.SpeechSpeaker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

/** 한국어 음성이 없거나 엔진이 준비되지 않으면 조용히 건너뛴다. 같은 문구가 화면에 보이기 때문이다. */
class AndroidSpeechSpeaker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SpeechSpeaker {

    private var engine: CompletableDeferred<TextToSpeech?>? = null

    override suspend fun speak(text: String) {
        val tts = readyEngine() ?: return
        suspendCancellableCoroutine { continuation ->
            val utteranceId = UUID.randomUUID().toString()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = resumeOnce()
                override fun onError(utteranceId: String?) = resumeOnce()
                private fun resumeOnce() {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            })
            continuation.invokeOnCancellation { tts.stop() }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS) {
                continuation.resume(Unit)
            }
        }
    }

    override fun shutdown() {
        val current = engine ?: return
        engine = null
        if (current.isCompleted) current.getCompleted()?.shutdown()
        // 아직 준비 중이면 준비가 끝나는 시점에 결과를 버린다.
        else current.invokeOnCompletion { current.getCompleted()?.shutdown() }
    }

    private suspend fun readyEngine(): TextToSpeech? {
        val deferred = engine ?: createEngine().also { engine = it }
        return deferred.await()
    }

    private fun createEngine(): CompletableDeferred<TextToSpeech?> {
        val result = CompletableDeferred<TextToSpeech?>()
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context) { status ->
            val usable = status == TextToSpeech.SUCCESS && tts.isKoreanAvailable()
            if (!usable) tts.shutdown()
            result.complete(if (usable) tts else null)
        }
        return result
    }

    private fun TextToSpeech.isKoreanAvailable(): Boolean {
        val code = setLanguage(Locale.KOREAN)
        return code != TextToSpeech.LANG_MISSING_DATA && code != TextToSpeech.LANG_NOT_SUPPORTED
    }
}
