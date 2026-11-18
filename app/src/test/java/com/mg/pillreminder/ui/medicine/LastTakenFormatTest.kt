package com.mg.pillreminder.ui.medicine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LastTakenFormatTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val now = Instant.parse("2026-11-18T05:00:00Z") // 서울 14:00

    @Test
    fun `오늘 먹었으면 시각만`() {
        assertEquals("11:20", formatLastTaken(Instant.parse("2026-11-18T02:20:00Z"), now, seoul))
    }

    @Test
    fun `어제 먹었으면 날짜까지`() {
        assertEquals("11/17 23:30", formatLastTaken(Instant.parse("2026-11-17T14:30:00Z"), now, seoul))
    }

    @Test
    fun `UTC로는 어제여도 서울 기준 오늘이면 시각만`() {
        // UTC 11/17 15:30 = 서울 11/18 00:30
        assertEquals("00:30", formatLastTaken(Instant.parse("2026-11-17T15:30:00Z"), now, seoul))
    }
}
