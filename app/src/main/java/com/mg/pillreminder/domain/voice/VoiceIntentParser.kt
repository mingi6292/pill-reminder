package com.mg.pillreminder.domain.voice

fun interface VoiceIntentParser {
    /** 해석하지 못하거나 네트워크 오류가 나면 예외 대신 [VoiceIntent.Unknown]을 돌려준다. */
    suspend fun parse(utterance: String): VoiceIntent
}
