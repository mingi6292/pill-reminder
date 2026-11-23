package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * 지금 시각으로 복용을 기록하고, 주기 알림이 있으면 이 시각부터 다시 센다.
 * 알림이 떠 있는 사이 약이 지워졌으면 기록하지 않고 null.
 */
class RecordDoseUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseRecordRepository: DoseRecordRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(medicineId: Long): DoseRecord? {
        medicineRepository.find(medicineId) ?: return null
        val now = Instant.now(clock)
        val record = DoseRecord(id = 0, medicineId = medicineId, takenAt = now)
        val id = doseRecordRepository.add(record)
        restartIntervalReminders(medicineId, takenAt = now)
        return record.copy(id = id)
    }

    // 주기 알림은 "먹은 뒤 N시간"이 목적이라, 예정보다 일찍이나 늦게 먹었으면 먹은 시각을 기준으로 다시 맞춘다.
    private suspend fun restartIntervalReminders(medicineId: Long, takenAt: Instant) {
        reminderRepository.getByMedicine(medicineId).forEach { reminder ->
            val schedule = reminder.schedule as? ReminderSchedule.Interval ?: return@forEach
            val next = ReminderTimeCalculator.nextInterval(takenAt, schedule.interval, takenAt)
            reminderRepository.updateNextTriggerAt(reminder.id, next)
            alarmScheduler.schedule(reminder.id, next)
        }
    }
}
