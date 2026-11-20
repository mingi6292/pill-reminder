package com.mg.pillreminder.ui.medicine

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TODAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val OTHER_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d HH:mm")

/** 오늘이면 시각만, 아니면 날짜까지 붙인다. 오늘 여부는 사용자가 있는 타임존 기준이다. */
fun formatShortDateTime(at: Instant, now: Instant, zone: ZoneId): String {
    val local = at.atZone(zone)
    val isToday = local.toLocalDate() == now.atZone(zone).toLocalDate()
    return (if (isToday) TODAY_FORMAT else OTHER_DAY_FORMAT).format(local)
}
