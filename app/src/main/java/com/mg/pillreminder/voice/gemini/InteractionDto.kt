package com.mg.pillreminder.voice.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class InteractionRequest(
    val model: String,
    val input: String,
    @SerialName("system_instruction") val systemInstruction: String,
    @SerialName("response_format") val responseFormat: ResponseFormat,
    @SerialName("generation_config") val generationConfig: GenerationConfig,
    // 복약 문장을 서버에 남기지 않는다. 대화 맥락은 앱이 기억하므로 이전 interaction을 이어 쓸 일도 없다.
    val store: Boolean = false,
)

@Serializable
data class ResponseFormat(
    val type: String = "text",
    @SerialName("mime_type") val mimeType: String = "application/json",
    val schema: JsonObject,
)

@Serializable
data class GenerationConfig(
    @SerialName("thinking_level") val thinkingLevel: String,
)

@Serializable
data class InteractionResponse(
    val status: String? = null,
    val steps: List<Step> = emptyList(),
) {
    /** 모델이 낸 텍스트를 이어 붙인다. 완료되지 않았거나 비었으면 null. */
    fun outputText(): String? {
        if (status != "completed") return null
        return steps.filter { it.type == "model_output" }
            .flatMap { it.content }
            .filter { it.type == "text" }
            .joinToString("") { it.text.orEmpty() }
            .ifBlank { null }
    }
}

@Serializable
data class Step(
    val type: String,
    val content: List<Content> = emptyList(),
)

@Serializable
data class Content(
    val type: String,
    val text: String? = null,
)
