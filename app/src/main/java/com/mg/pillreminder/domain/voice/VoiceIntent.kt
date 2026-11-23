package com.mg.pillreminder.domain.voice

/** LLM이 사용자 한 문장을 바꾼 결과. 앱은 이것을 UseCase 호출로 연결한다. */
sealed interface VoiceIntent {
    data class RecordDose(val medicineName: String) : VoiceIntent

    /** 약 이름이 빠지면("4시간 뒤에") null이고, 직전 대화의 약을 쓴다. */
    data class ScheduleAfter(val medicineName: String?, val minutes: Int) : VoiceIntent

    data class AskLastDose(val medicineName: String) : VoiceIntent

    /** "응", "아니". 무엇에 대한 답인지는 [VoiceConversation]이 직전 질문으로 판단한다. */
    data class Answer(val yes: Boolean) : VoiceIntent

    data object Unknown : VoiceIntent
}
