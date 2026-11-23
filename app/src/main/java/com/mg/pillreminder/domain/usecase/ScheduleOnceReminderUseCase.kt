package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * 지금부터 [after] 뒤에 한 번만 울릴 알림을 예약하고 그 시각을 돌려준다. 약이 지워졌으면 null.
 * 화면, 알림 버튼, 음성 입구가 같이 쓴다.
 */
class ScheduleOnceReminderUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(medicineId: Long, after: Duration): Instant? {
        require(!after.isZero && !after.isNegative) { "after must be positive: $after" }
        medicineRepository.find(medicineId) ?: return null

        replacePendingOnce(medicineId)
        val triggerAt = ReminderTimeCalculator.after(Instant.now(clock), after)
        val reminder = Reminder(id = 0, medicineId = medicineId, schedule = ReminderSchedule.Once, nextTriggerAt = triggerAt)
        val reminderId = reminderRepository.add(reminder)
        alarmScheduler.schedule(reminderId, triggerAt)
        return triggerAt
    }

    // "나중에"를 여러 번 누르면 마지막 요청만 남긴다. 같은 약 알림이 여러 번 울리는 것보다 덜 헷갈린다.
    private suspend fun replacePendingOnce(medicineId: Long) {
        reminderRepository.getByMedicine(medicineId)
            .filter { it.schedule == ReminderSchedule.Once }
            .forEach {
                reminderRepository.delete(it.id)
                alarmScheduler.cancel(it.id)
            }
    }
}
