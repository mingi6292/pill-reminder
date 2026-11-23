package com.mg.pillreminder.domain

import java.time.Duration
import java.time.Instant

/**
 * 최근 복용 간격이 반복되는지 본다. 규칙은 design.md 5장.
 * 간격 셋의 중앙값을 기준으로 삼아 한 번 늦게 먹은 기록이 평균처럼 결과를 끌고 가지 않게 한다.
 */
object RecurringIntervalDetector {
    const val SAMPLE_SIZE = 4
    private val TOLERANCE: Duration = Duration.ofMinutes(30)
    private val MIN_INTERVAL: Duration = Duration.ofHours(1)
    private val ROUNDING_UNIT: Duration = Duration.ofMinutes(30)

    /** 주기로 보이면 30분 단위로 반올림한 간격, 아니면 null. [takenAt] 순서는 상관없다. */
    fun detect(takenAt: List<Instant>): Duration? {
        if (takenAt.size < SAMPLE_SIZE) return null
        val recent = takenAt.sortedDescending().take(SAMPLE_SIZE).reversed()
        val gaps = recent.zipWithNext { earlier, later -> Duration.between(earlier, later) }

        val median = gaps.sorted()[gaps.size / 2]
        if (median < MIN_INTERVAL) return null
        if (gaps.any { (it - median).abs() > TOLERANCE }) return null
        return roundToUnit(median)
    }

    private fun roundToUnit(duration: Duration): Duration {
        val unitSeconds = ROUNDING_UNIT.seconds
        val units = (duration.seconds + unitSeconds / 2) / unitSeconds
        return Duration.ofSeconds(units * unitSeconds)
    }
}
