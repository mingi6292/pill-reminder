package com.mg.pillreminder.domain.voice

/**
 * 음성 입구가 할 일. [speech]를 읽어 주고, [keepListening]이면 사용자의 답을 이어서 듣는다.
 * [openRegistrationFor]가 있으면 그 이름을 채운 약 등록 화면을 연다. 시각은 화면에서 고르게 한다.
 */
data class VoiceReply(
    val speech: String,
    val keepListening: Boolean,
    val openRegistrationFor: String? = null,
)
