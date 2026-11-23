package com.mg.pillreminder.voice.gemini

import com.mg.pillreminder.domain.voice.VoiceIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 모델 출력 JSON과 [VoiceIntent] 사이의 변환. sealed interface를 그대로 스키마로 옮기면 anyOf가 깊어져서
 * type 하나와 nullable 필드로 평평하게 둔다.
 */
internal object VoiceIntentJson {

    val SYSTEM_INSTRUCTION = """
        너는 복약 알림 앱의 음성 명령 해석기다. 사용자의 한국어 한 문장을 JSON 하나로만 바꾼다.
        type 값:
        - record_dose: 약을 먹었다고 말함. 예) "생리통약 먹었어" → medicine_name "생리통약"
        - schedule_after: 지금부터 일정 시간 뒤에 알려 달라고 말함. minutes에 분 단위로 넣는다.
          예) "4시간 뒤에" → minutes 240, medicine_name null / "두통약 30분 뒤에 알려줘" → "두통약", 30
        - ask_last_dose: 마지막으로 언제 먹었는지 물음. 예) "생리통약 언제 먹었지?" → "생리통약"
        - answer: 질문에 대한 예/아니오. 예) "응", "그래", "등록해줘" → yes true / "아니", "됐어" → yes false
        - unknown: 위에 해당하지 않거나 확실하지 않음
        약 이름은 사용자가 말한 그대로 넣고 "약을"의 "을" 같은 조사는 뺀다. 해당 없는 필드는 null.
    """.trimIndent()

    val SCHEMA = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("type") {
                put("type", "string")
                putJsonArray("enum") { Type.entries.forEach { add(it.wireName) } }
            }
            putJsonObject("medicine_name") { nullable("string") }
            putJsonObject("minutes") { nullable("integer") }
            putJsonObject("yes") { nullable("boolean") }
        }
        putJsonArray("required") { listOf("type", "medicine_name", "minutes", "yes").forEach { add(it) } }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** 스키마를 어기거나 필요한 값이 빠졌으면 [VoiceIntent.Unknown]. */
    fun parse(text: String): VoiceIntent {
        val output = runCatching { json.decodeFromString<Output>(text) }.getOrNull() ?: return VoiceIntent.Unknown
        val name = output.medicineName?.trim()?.ifEmpty { null }
        return when (Type.entries.find { it.wireName == output.type }) {
            Type.RECORD_DOSE -> name?.let(VoiceIntent::RecordDose)
            Type.SCHEDULE_AFTER -> output.minutes?.takeIf { it > 0 }?.let { VoiceIntent.ScheduleAfter(name, it) }
            Type.ASK_LAST_DOSE -> name?.let(VoiceIntent::AskLastDose)
            Type.ANSWER -> output.yes?.let(VoiceIntent::Answer)
            Type.UNKNOWN, null -> null
        } ?: VoiceIntent.Unknown
    }

    private fun JsonObjectBuilder.nullable(type: String) =
        putJsonArray("type") { add(type); add("null") }

    private enum class Type(val wireName: String) {
        RECORD_DOSE("record_dose"),
        SCHEDULE_AFTER("schedule_after"),
        ASK_LAST_DOSE("ask_last_dose"),
        ANSWER("answer"),
        UNKNOWN("unknown"),
    }

    @Serializable
    private data class Output(
        val type: String,
        @SerialName("medicine_name") val medicineName: String? = null,
        val minutes: Int? = null,
        val yes: Boolean? = null,
    )
}
