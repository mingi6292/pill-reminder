package com.mg.pillreminder.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 알림 시각 계산. 현재 시각은 호출하는 쪽(UseCase)이 주입받은 Clock으로 구해 넘긴다.
 */
object ReminderTimeCalculator {

    fun nextDaily(now: Instant, time: LocalTime, zone: ZoneId): Instant {
        val today = now.atZone(zone).toLocalDate()
        // ZonedDateTime.of는 서머타임으로 사라진 시각(예: 02:30)을 갭 길이만큼 뒤로 민다(03:30).
        // 그날 알림을 건너뛰는 것보다 조금 늦게라도 울리는 편이 복약 알림에 맞아서 그대로 쓴다.
        val todayAt = ZonedDateTime.of(today, time, zone).toInstant()
        if (todayAt.isAfter(now)) return todayAt
        return ZonedDateTime.of(today.plusDays(1), time, zone).toInstant()
    }

    fun after(now: Instant, duration: Duration): Instant = now.plus(duration)

    fun nextInterval(lastTakenAt: Instant, interval: Duration, now: Instant): Instant {
        require(!interval.isZero && !interval.isNegative) { "interval must be positive: $interval" }

        val first = lastTakenAt.plus(interval)
        if (first.isAfter(now)) return first

        // Duration.dividedBy(Duration)는 API 31부터라 minSdk 26에서는 나노초로 나눈다.
        val passedPeriods = Duration.between(lastTakenAt, now).toNanos() / interval.toNanos()
        return lastTakenAt.plus(interval.multipliedBy(passedPeriods + 1))
    }
}
