package com.mg.pillreminder.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class SpokenTimeFormatTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun `기간은 시간과 분을 말로 읽는다`() {
        assertEquals("4시간", SpokenTimeFormat.duration(Duration.ofHours(4)))
        assertEquals("30분", SpokenTimeFormat.duration(Duration.ofMinutes(30)))
        assertEquals("1시간 30분", SpokenTimeFormat.duration(Duration.ofMinutes(90)))
        assertEquals("1분", SpokenTimeFormat.duration(Duration.ofSeconds(20)))
    }

    @Test
    fun `시각은 오전 오후 12시간제로 읽고 정각이면 분을 뺀다`() {
        assertEquals("오후 3시 20분", SpokenTimeFormat.clockTime(seoulAt("15:20"), seoul))
        assertEquals("오전 9시", SpokenTimeFormat.clockTime(seoulAt("09:00"), seoul))
        assertEquals("오전 12시 5분", SpokenTimeFormat.clockTime(seoulAt("00:05"), seoul))
        assertEquals("오후 12시", SpokenTimeFormat.clockTime(seoulAt("12:00"), seoul))
    }

    @Test
    fun `날짜는 오늘 어제 내일 그 밖에는 월일로 읽는다`() {
        val now = seoulAt("10:00", day = 20)
        assertEquals("오늘", SpokenTimeFormat.day(seoulAt("23:59", day = 20), now, seoul))
        assertEquals("어제", SpokenTimeFormat.day(seoulAt("23:59", day = 19), now, seoul))
        assertEquals("내일", SpokenTimeFormat.day(seoulAt("00:00", day = 21), now, seoul))
        assertEquals("11월 17일", SpokenTimeFormat.day(seoulAt("10:00", day = 17), now, seoul))
    }

    private fun seoulAt(time: String, day: Int = 20): Instant =
        Instant.parse("2026-11-${day}T$time:00+09:00")
}
