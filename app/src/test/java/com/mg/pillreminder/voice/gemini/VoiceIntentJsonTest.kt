package com.mg.pillreminder.voice.gemini

import com.mg.pillreminder.domain.voice.VoiceIntent
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIntentJsonTest {

    @Test
    fun `type별로 VoiceIntent를 만든다`() {
        assertEquals(VoiceIntent.RecordDose("생리통약"), parse("record_dose", name = "생리통약"))
        assertEquals(VoiceIntent.ScheduleAfter("두통약", 30), parse("schedule_after", name = "두통약", minutes = 30))
        assertEquals(VoiceIntent.AskLastDose("생리통약"), parse("ask_last_dose", name = "생리통약"))
        assertEquals(VoiceIntent.Answer(yes = false), parse("answer", yes = false))
    }

    @Test
    fun `필요한 값이 빠지거나 이상하면 Unknown`() {
        assertEquals(VoiceIntent.Unknown, parse("record_dose", name = "  "))
        assertEquals(VoiceIntent.Unknown, parse("schedule_after", minutes = 0))
        assertEquals(VoiceIntent.Unknown, parse("answer"))
        assertEquals(VoiceIntent.Unknown, parse("dance"))
        assertEquals(VoiceIntent.Unknown, VoiceIntentJson.parse("not json"))
    }

    private fun parse(type: String, name: String? = null, minutes: Int? = null, yes: Boolean? = null): VoiceIntent {
        val nameJson = name?.let { "\"$it\"" } ?: "null"
        return VoiceIntentJson.parse(
            """{"type":"$type","medicine_name":$nameJson,"minutes":${minutes ?: "null"},"yes":${yes ?: "null"}}""",
        )
    }
}
