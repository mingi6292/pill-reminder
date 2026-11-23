package com.mg.pillreminder.domain.voice

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** TTS가 읽을 한국어 시각·기간 문구. "오후 3시 20분", "4시간 30분" 식으로 숫자를 말로 읽기 좋게 만든다. */
object SpokenTimeFormat {
    private val OTHER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")

    /** "4시간", "30분", "1시간 30분". 1분 미만은 "1분"으로 올린다. */
    fun duration(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0L -> "${minutes}분"
            minutes == 0L -> "${hours}시간"
            else -> "${hours}시간 ${minutes}분"
        }
    }

    /** "오후 3시 20분", 정각이면 "오후 3시". 자정과 정오는 "오전 12시", "오후 12시". */
    fun clockTime(at: Instant, zone: ZoneId): String {
        val time = at.atZone(zone).toLocalTime()
        val period = if (time.hour < 12) "오전" else "오후"
        val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
        val minutePart = if (time.minute == 0) "" else " ${time.minute}분"
        return "$period ${hour12}시$minutePart"
    }

    /** "오늘", "어제", "내일", 그 밖에는 "11월 20일". */
    fun day(at: Instant, now: Instant, zone: ZoneId): String {
        val date = at.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return when (date) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            today.plusDays(1) -> "내일"
            else -> OTHER_DATE.format(date)
        }
    }
}
