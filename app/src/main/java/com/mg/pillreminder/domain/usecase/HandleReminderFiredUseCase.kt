package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.model.DueReminder
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** 알람이 울렸을 때 다음 알림을 예약하고, 보여 줄 알림을 돌려준다. 그새 약이나 알림이 지워졌으면 null. */
class HandleReminderFiredUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(reminderId: Long): DueReminder? {
        val reminder = reminderRepository.find(reminderId) ?: return null
        val medicine = medicineRepository.find(reminder.medicineId) ?: return null
        scheduleNext(reminder)
        return DueReminder(reminderId = reminder.id, medicineName = medicine.name)
    }

    private suspend fun scheduleNext(reminder: Reminder) {
        val now = Instant.now(clock)
        val next = when (val schedule = reminder.schedule) {
            // 알람이 예정보다 조금 일찍 울려도 같은 날 또 울리지 않게 예정 시각과 지금 중 늦은 쪽을 기준으로 한다.
            is ReminderSchedule.Daily ->
                ReminderTimeCalculator.nextDaily(maxOf(now, reminder.nextTriggerAt), schedule.time, ZoneId.systemDefault())
            is ReminderSchedule.Interval ->
                ReminderTimeCalculator.nextInterval(reminder.nextTriggerAt, schedule.interval, now)
            ReminderSchedule.Once -> {
                reminderRepository.delete(reminder.id)
                return
            }
        }
        reminderRepository.updateNextTriggerAt(reminder.id, next)
        alarmScheduler.schedule(reminder.id, next)
    }
}
