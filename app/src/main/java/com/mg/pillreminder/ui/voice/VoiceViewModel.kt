package com.mg.pillreminder.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.pillreminder.domain.voice.ListenResult
import com.mg.pillreminder.domain.voice.SpeechListener
import com.mg.pillreminder.domain.voice.SpeechSpeaker
import com.mg.pillreminder.domain.voice.VoiceConversation
import com.mg.pillreminder.domain.voice.VoiceIntentParser
import com.mg.pillreminder.domain.voice.VoiceReply
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

enum class VoicePhase { Idle, Listening, Thinking, Speaking }

/** 듣지 못한 이유. 문구는 화면이 리소스로 정한다. */
enum class VoiceProblem { NoSpeech, PermissionDenied, Unavailable }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.Idle,
    /** 방금 인식한 문장. */
    val heard: String? = null,
    /** 앱이 답한 문구. 소리를 못 내도 화면에는 보인다. */
    val reply: String? = null,
    val problem: VoiceProblem? = null,
)

/**
 * 듣기 → 해석 → 응답을 한 사이클로 돌리고, 응답이 질문이면 이어서 듣는다.
 * 비즈니스 로직은 [VoiceConversation]에 있고 여기는 입출력 순서만 맡는다.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val listener: SpeechListener,
    private val speaker: SpeechSpeaker,
    private val parser: VoiceIntentParser,
    private val conversationProvider: Provider<VoiceConversation>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val registrationEvents = Channel<String>(Channel.BUFFERED)

    /** 등록되지 않은 약을 등록하겠다고 답했을 때, 등록 화면에 채울 약 이름. */
    val registrationRequests: Flow<String> = registrationEvents.receiveAsFlow()

    private var conversation: VoiceConversation? = null
    private var session: Job? = null

    /** 새 대화를 시작한다. 직전 약 같은 기억은 시트를 닫으면 사라지고 여기서 새로 시작한다. */
    fun start() {
        session?.cancel()
        val current = conversationProvider.get().also { conversation = it }
        _uiState.value = VoiceUiState()
        session = viewModelScope.launch { converse(current) }
    }

    /** 시트를 닫을 때. 듣던 중이면 마이크를 놓고 읽던 중이면 멈춘다. */
    fun stop() {
        session?.cancel()
        session = null
        conversation = null
        speaker.shutdown()
        _uiState.value = VoiceUiState()
    }

    /** 대화 중 다시 말하려고 마이크를 눌렀을 때. 기억은 유지한다. */
    fun listenAgain() {
        val current = conversation ?: return start()
        session?.cancel()
        session = viewModelScope.launch { converse(current) }
    }

    fun onPermissionDenied() {
        _uiState.value = VoiceUiState(problem = VoiceProblem.PermissionDenied)
    }

    private suspend fun converse(conversation: VoiceConversation) {
        var keepListening = true
        while (keepListening) {
            _uiState.update { it.copy(phase = VoicePhase.Listening, problem = null) }
            keepListening = when (val heard = listener.listen()) {
                is ListenResult.Heard -> respondTo(conversation, heard.text)
                ListenResult.NoSpeech -> stopWith(VoiceProblem.NoSpeech)
                ListenResult.PermissionDenied -> stopWith(VoiceProblem.PermissionDenied)
                ListenResult.Unavailable -> stopWith(VoiceProblem.Unavailable)
            }
        }
    }

    private suspend fun respondTo(conversation: VoiceConversation, text: String): Boolean {
        _uiState.value = VoiceUiState(phase = VoicePhase.Thinking, heard = text)
        val reply = conversation.reply(parser.parse(text))
        _uiState.update { it.copy(phase = VoicePhase.Speaking, reply = reply.speech) }
        speaker.speak(reply.speech)
        reply.openRegistrationFor?.let { registrationEvents.send(it) }
        return finishTurn(reply)
    }

    private fun finishTurn(reply: VoiceReply): Boolean {
        if (!reply.keepListening) _uiState.update { it.copy(phase = VoicePhase.Idle) }
        return reply.keepListening
    }

    private fun stopWith(problem: VoiceProblem): Boolean {
        _uiState.update { it.copy(phase = VoicePhase.Idle, problem = problem) }
        return false
    }

    override fun onCleared() = speaker.shutdown()
}
