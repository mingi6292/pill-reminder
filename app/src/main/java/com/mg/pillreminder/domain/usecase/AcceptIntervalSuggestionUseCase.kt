package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * 제안을 받아들여 마지막 복용부터 [interval]마다 울릴 알림을 저장·예약하고 첫 알림 시각을 돌려준다.
 * 약이나 복용 기록이 없거나 이미 주기 알림이 있으면 아무것도 하지 않고 null.
 */
class AcceptIntervalSuggestionUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val doseRecordRepository: DoseRecordRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(medicineId: Long, interval: Duration): Instant? {
        require(!interval.isZero && !interval.isNegative) { "interval must be positive: $interval" }
        medicineRepository.find(medicineId) ?: return null
        // 버튼을 두 번 눌러도 주기 알림이 둘 생기지 않게 한다.
        if (reminderRepository.getByMedicine(medicineId).any { it.schedule is ReminderSchedule.Interval }) return null
        val lastTaken = doseRecordRepository.findLatest(listOf(medicineId)) ?: return null

        val triggerAt = ReminderTimeCalculator.nextInterval(lastTaken.takenAt, interval, Instant.now(clock))
        val reminder = Reminder(
            id = 0,
            medicineId = medicineId,
            schedule = ReminderSchedule.Interval(interval),
            nextTriggerAt = triggerAt,
        )
        val reminderId = reminderRepository.add(reminder)
        alarmScheduler.schedule(reminderId, triggerAt)
        return triggerAt
    }
}
