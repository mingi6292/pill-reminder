package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.RecurringIntervalDetector
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Duration
import javax.inject.Inject

/**
 * 이 약에 제안할 주기 알림 간격. 제안하지 않을 때는 null.
 * 이미 주기 알림이 있거나 사용자가 거절했으면 기록이 반복돼도 제안하지 않는다.
 */
class GetIntervalSuggestionUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val doseRecordRepository: DoseRecordRepository,
) {
    suspend operator fun invoke(medicineId: Long): Duration? {
        val medicine = medicineRepository.find(medicineId) ?: return null
        if (medicine.intervalSuggestionDismissed) return null
        if (hasIntervalReminder(medicineId)) return null
        val recent = doseRecordRepository.findRecentTakenAt(medicineId, RecurringIntervalDetector.SAMPLE_SIZE)
        return RecurringIntervalDetector.detect(recent)
    }

    private suspend fun hasIntervalReminder(medicineId: Long): Boolean =
        reminderRepository.getByMedicine(medicineId).any { it.schedule is ReminderSchedule.Interval }
}
