package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.ReminderTimeCalculator
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.MedicineRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** 약을 등록하고 매일 [time]에 울릴 알림을 함께 저장한다. 새 약 id를 돌려준다. */
class AddDailyMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(name: String, time: LocalTime): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "medicine name must not be blank" }

        val now = Instant.now(clock)
        val medicine = Medicine(id = 0, name = trimmedName, createdAt = now)
        val reminder = Reminder(
            id = 0,
            medicineId = 0,
            schedule = ReminderSchedule.Daily(time),
            // 사용자가 타임존을 바꿀 수 있어 Clock의 zone이 아니라 지금 시점의 시스템 타임존을 쓴다.
            nextTriggerAt = ReminderTimeCalculator.nextDaily(now, time, ZoneId.systemDefault()),
        )
        return repository.addWithReminder(medicine, reminder)
    }
}
