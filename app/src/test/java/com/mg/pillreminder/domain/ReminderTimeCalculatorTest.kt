package com.mg.pillreminder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

class ReminderTimeCalculatorTest {

    // nextDaily

    @Test
    fun `nextDaily - 시각 전이면 오늘`() {
        val next = ReminderTimeCalculator.nextDaily(seoul("2026-11-06T07:59"), LocalTime.of(8, 0), SEOUL)

        assertEquals(seoul("2026-11-06T08:00"), next)
    }

    @Test
    fun `nextDaily - 같은 분이면 내일`() {
        val next = ReminderTimeCalculator.nextDaily(seoul("2026-11-06T08:00"), LocalTime.of(8, 0), SEOUL)

        assertEquals(seoul("2026-11-07T08:00"), next)
    }

    @Test
    fun `nextDaily - 시각이 지났으면 내일`() {
        val next = ReminderTimeCalculator.nextDaily(seoul("2026-11-06T23:45"), LocalTime.of(23, 30), SEOUL)

        assertEquals(seoul("2026-11-07T23:30"), next)
    }

    @Test
    fun `nextDaily - 다른 타임존은 그 지역 시각 기준`() {
        val now = at("2026-11-06T07:59", NEW_YORK)

        val next = ReminderTimeCalculator.nextDaily(now, LocalTime.of(8, 0), NEW_YORK)

        assertEquals(at("2026-11-06T08:00", NEW_YORK), next)
        assertNotEquals(seoul("2026-11-06T08:00"), next)
    }

    @Test
    fun `nextDaily - 서머타임 시작일에 없는 시각은 갭만큼 뒤로 밀린다`() {
        // 2026-03-08 뉴욕은 02:00 EST에서 03:00 EDT로 넘어가 02:30이 존재하지 않는다.
        // 알림을 건너뛰지 않고 03:30 EDT에 울리는지 확인한다.
        val now = OffsetDateTime.parse("2026-03-08T01:00-05:00").toInstant()

        val next = ReminderTimeCalculator.nextDaily(now, LocalTime.of(2, 30), NEW_YORK)

        assertEquals(OffsetDateTime.parse("2026-03-08T03:30-04:00").toInstant(), next)
    }

    // after

    @Test
    fun `after - 지금에 기간을 더한다`() {
        val now = seoul("2026-11-06T11:20")

        val next = ReminderTimeCalculator.after(now, Duration.ofHours(4))

        assertEquals(seoul("2026-11-06T15:20"), next)
    }

    // nextInterval

    @Test
    fun `nextInterval - 다음 주기가 아직 미래면 마지막 복용 + 주기`() {
        val next = ReminderTimeCalculator.nextInterval(
            lastTakenAt = seoul("2026-11-06T08:00"),
            interval = Duration.ofHours(4),
            now = seoul("2026-11-06T10:00"),
        )

        assertEquals(seoul("2026-11-06T12:00"), next)
    }

    @Test
    fun `nextInterval - 주기를 여러 번 넘겼으면 지금 이후 가장 가까운 주기`() {
        val next = ReminderTimeCalculator.nextInterval(
            lastTakenAt = seoul("2026-11-06T08:00"),
            interval = Duration.ofHours(4),
            now = seoul("2026-11-06T17:00"),
        )

        assertEquals(seoul("2026-11-06T20:00"), next)
    }

    @Test
    fun `nextInterval - 주기가 지금과 같으면 그다음 주기`() {
        val next = ReminderTimeCalculator.nextInterval(
            lastTakenAt = seoul("2026-11-06T08:00"),
            interval = Duration.ofHours(4),
            now = seoul("2026-11-06T16:00"),
        )

        assertEquals(seoul("2026-11-06T20:00"), next)
    }

    @Test
    fun `nextInterval - 주기가 0이면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderTimeCalculator.nextInterval(seoul("2026-11-06T08:00"), Duration.ZERO, seoul("2026-11-06T10:00"))
        }
    }

    @Test
    fun `nextInterval - 주기가 음수면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderTimeCalculator.nextInterval(seoul("2026-11-06T08:00"), Duration.ofHours(-1), seoul("2026-11-06T10:00"))
        }
    }

    private fun seoul(localDateTime: String): Instant = at(localDateTime, SEOUL)

    private fun at(localDateTime: String, zone: ZoneId): Instant =
        LocalDateTime.parse(localDateTime).atZone(zone).toInstant()

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
    }
}
