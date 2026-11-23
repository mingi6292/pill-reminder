package com.mg.pillreminder.voice.gemini

import com.mg.pillreminder.domain.voice.VoiceIntent
import com.mg.pillreminder.domain.voice.VoiceIntentParser
import kotlinx.coroutines.CancellationException

/** 키가 없거나 네트워크·응답 형식 오류가 나면 [VoiceIntent.Unknown]. 대화는 "다시 말씀해 주세요"로 이어진다. */
class GeminiVoiceIntentParser(
    private val api: GeminiApi,
    private val apiKey: String,
    private val onError: (Exception) -> Unit = {},
) : VoiceIntentParser {

    override suspend fun parse(utterance: String): VoiceIntent {
        if (apiKey.isBlank() || utterance.isBlank()) return VoiceIntent.Unknown
        val text = try {
            api.createInteraction(apiKey, request(utterance)).outputText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
            null
        }
        return text?.let(VoiceIntentJson::parse) ?: VoiceIntent.Unknown
    }

    private fun request(utterance: String) = InteractionRequest(
        model = GeminiApi.MODEL,
        input = utterance,
        systemInstruction = VoiceIntentJson.SYSTEM_INSTRUCTION,
        responseFormat = ResponseFormat(schema = VoiceIntentJson.SCHEMA),
        // 짧은 문장 분류라 생각을 거의 끄고 지연을 줄인다.
        generationConfig = GenerationConfig(thinkingLevel = "minimal"),
    )
}
