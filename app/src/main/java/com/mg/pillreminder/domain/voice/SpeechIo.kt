package com.mg.pillreminder.domain.voice

/** 마이크로 한 문장을 듣는다. 구현은 Android `SpeechRecognizer`, 테스트에서는 Fake. */
fun interface SpeechListener {
    suspend fun listen(): ListenResult
}

sealed interface ListenResult {
    data class Heard(val text: String) : ListenResult

    /** 말이 없었거나 알아듣지 못했다. 다시 눌러 말하면 된다. */
    data object NoSpeech : ListenResult

    data object PermissionDenied : ListenResult

    /** 기기에 음성 인식 서비스가 없거나 인식기가 오류를 냈다. */
    data object Unavailable : ListenResult
}

/** 응답을 소리로 읽는다. 읽기가 끝나거나 읽을 수 없으면 돌아온다. 화면에도 문구가 보이므로 실패해도 대화는 이어 간다. */
interface SpeechSpeaker {
    suspend fun speak(text: String)

    /** 엔진을 놓는다. 이후 [speak]를 부르면 다시 준비한다. */
    fun shutdown()
}
