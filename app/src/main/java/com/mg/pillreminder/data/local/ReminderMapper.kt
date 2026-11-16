package com.mg.pillreminder.data.local

import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

private const val TYPE_DAILY = "DAILY"
private const val TYPE_ONCE = "ONCE"
private const val TYPE_INTERVAL = "INTERVAL"

fun Reminder.toEntity(): ReminderEntity {
    val base = ReminderEntity(
        medicineId = medicineId,
        type = "",
        timeOfDayMinutes = null,
        intervalMinutes = null,
        nextTriggerAt = nextTriggerAt.toEpochMilli(),
    )
    return when (schedule) {
        is ReminderSchedule.Daily -> base.copy(type = TYPE_DAILY, timeOfDayMinutes = schedule.time.toMinuteOfDay())
        ReminderSchedule.Once -> base.copy(type = TYPE_ONCE)
        is ReminderSchedule.Interval -> base.copy(type = TYPE_INTERVAL, intervalMinutes = schedule.interval.toMinutes())
    }
}

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    medicineId = medicineId,
    schedule = toSchedule(),
    nextTriggerAt = Instant.ofEpochMilli(nextTriggerAt),
)

private fun ReminderEntity.toSchedule(): ReminderSchedule = when (type) {
    TYPE_DAILY -> ReminderSchedule.Daily(LocalTime.ofSecondOfDay(requireColumn(timeOfDayMinutes) * 60L))
    TYPE_ONCE -> ReminderSchedule.Once
    TYPE_INTERVAL -> ReminderSchedule.Interval(Duration.ofMinutes(requireColumn(intervalMinutes)))
    else -> error("Unknown reminder type: $type (id=$id)")
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun <T : Any> ReminderEntity.requireColumn(value: T?): T =
    requireNotNull(value) { "Reminder $id of type $type is missing its schedule column" }
