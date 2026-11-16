package com.mg.pillreminder.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

data class Reminder(
    val id: Long,
    val medicineId: Long,
    val schedule: ReminderSchedule,
    val nextTriggerAt: Instant,
)

sealed interface ReminderSchedule {
    data class Daily(val time: LocalTime) : ReminderSchedule
    data object Once : ReminderSchedule
    data class Interval(val interval: Duration) : ReminderSchedule
}
