package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.voice.ListenResult
import com.mg.pillreminder.domain.voice.SpeechListener
import com.mg.pillreminder.domain.voice.SpeechSpeaker
import com.mg.pillreminder.domain.voice.VoiceIntent
import com.mg.pillreminder.domain.voice.VoiceIntentParser

/** 미리 정해 둔 결과를 차례로 돌려준다. 다 쓰면 [ListenResult.NoSpeech]. */
class FakeSpeechListener(vararg results: ListenResult) : SpeechListener {
    private val queue = ArrayDeque(results.toList())
    var listenCount = 0
        private set

    override suspend fun listen(): ListenResult {
        listenCount++
        return queue.removeFirstOrNull() ?: ListenResult.NoSpeech
    }
}

class FakeSpeechSpeaker : SpeechSpeaker {
    val spoken = mutableListOf<String>()
    var shutdownCount = 0
        private set

    override suspend fun speak(text: String) {
        spoken += text
    }

    override fun shutdown() {
        shutdownCount++
    }
}

/** 문장 → 의도 표. 없는 문장은 Unknown. */
class FakeVoiceIntentParser(private val intents: Map<String, VoiceIntent>) : VoiceIntentParser {
    override suspend fun parse(utterance: String): VoiceIntent = intents[utterance] ?: VoiceIntent.Unknown
}
