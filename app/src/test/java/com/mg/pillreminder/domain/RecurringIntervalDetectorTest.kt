package com.mg.pillreminder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RecurringIntervalDetectorTest {

    @Test
    fun `기록이 3개 이하면 판단하지 않는다`() {
        assertNull(RecurringIntervalDetector.detect(takenAtWithGaps(4.hours, 4.hours)))
        assertNull(RecurringIntervalDetector.detect(emptyList()))
    }

    @Test
    fun `간격 셋이 같으면 그 간격`() {
        assertEquals(4.hours, RecurringIntervalDetector.detect(takenAtWithGaps(4.hours, 4.hours, 4.hours)))
    }

    @Test
    fun `중앙값과 정확히 30분 차이는 주기로 본다`() {
        val gaps = takenAtWithGaps(4.hours - 30.minutes, 4.hours, 4.hours + 30.minutes)

        assertEquals(4.hours, RecurringIntervalDetector.detect(gaps))
    }

    @Test
    fun `중앙값과 30분을 넘게 차이 나면 주기가 아니다`() {
        val gaps = takenAtWithGaps(4.hours - 31.minutes, 4.hours, 4.hours)

        assertNull(RecurringIntervalDetector.detect(gaps))
    }

    @Test
    fun `중앙값이 59분이면 주기가 아니다`() {
        assertNull(RecurringIntervalDetector.detect(takenAtWithGaps(59.minutes, 59.minutes, 59.minutes)))
    }

    @Test
    fun `중앙값이 60분이면 1시간 주기`() {
        assertEquals(1.hours, RecurringIntervalDetector.detect(takenAtWithGaps(60.minutes, 60.minutes, 60.minutes)))
    }

    @Test
    fun `30분 단위로 반올림한다 - 3시간50분, 4시간5분, 4시간10분이면 4시간`() {
        val gaps = takenAtWithGaps(3.hours + 50.minutes, 4.hours + 5.minutes, 4.hours + 10.minutes)

        assertEquals(4.hours, RecurringIntervalDetector.detect(gaps))
    }

    @Test
    fun `15분은 올림해 30분 단위로 맞춘다`() {
        val gaps = takenAtWithGaps(4.hours + 15.minutes, 4.hours + 15.minutes, 4.hours + 15.minutes)

        assertEquals(4.hours + 30.minutes, RecurringIntervalDetector.detect(gaps))
    }

    @Test
    fun `순서가 섞여 들어와도 시각 순으로 본다`() {
        val shuffled = takenAtWithGaps(4.hours, 4.hours, 4.hours).let { listOf(it[2], it[0], it[3], it[1]) }

        assertEquals(4.hours, RecurringIntervalDetector.detect(shuffled))
    }

    @Test
    fun `기록이 4개보다 많으면 최근 4개만 본다`() {
        // 가장 오래된 간격(1시간)은 빠지고 최근 간격 셋(6시간)만 본다.
        val records = takenAtWithGaps(1.hours, 6.hours, 6.hours, 6.hours)

        assertEquals(6.hours, RecurringIntervalDetector.detect(records))
    }

    private fun takenAtWithGaps(vararg gaps: Duration): List<Instant> =
        gaps.runningFold(START) { takenAt, gap -> takenAt + gap }

    private val Int.hours: Duration get() = Duration.ofHours(toLong())
    private val Int.minutes: Duration get() = Duration.ofMinutes(toLong())

    private companion object {
        val START: Instant = Instant.parse("2026-11-20T00:00:00Z")
    }
}
