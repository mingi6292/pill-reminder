package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** 재부팅이나 시각·타임존 변경으로 사라지거나 틀어진 알람을 DB 기준으로 다시 예약한다. */
class RescheduleAllRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke() {
        val now = Instant.now(clock)
        reminderRepository.getAll().forEach { reminder ->
            val next = nextTriggerAt(reminder, now)
            if (next != reminder.nextTriggerAt) reminderRepository.updateNextTriggerAt(reminder.id, next)
            alarmScheduler.schedule(reminder.id, next)
        }
    }

    private fun nextTriggerAt(reminder: Reminder, now: Instant): Instant {
        // 꺼져 있는 동안 놓친 알림은 그 시각 그대로 예약해 바로 울리게 한다. 약 시간을 놓쳤다고 알리는 편이 안전하다.
        if (!reminder.nextTriggerAt.isAfter(now)) return reminder.nextTriggerAt
        val schedule = reminder.schedule
        // 타임존이 바뀌었을 수 있어 매일 알림은 지금 타임존으로 다시 계산한다.
        if (schedule is ReminderSchedule.Daily) {
            return ReminderTimeCalculator.nextDaily(now, schedule.time, ZoneId.systemDefault())
        }
        return reminder.nextTriggerAt
    }
}
