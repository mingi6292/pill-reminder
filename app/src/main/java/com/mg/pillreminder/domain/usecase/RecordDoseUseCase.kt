package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** 지금 시각으로 복용을 기록한다. 알림이 떠 있는 사이 약이 지워졌으면 기록하지 않고 null. */
class RecordDoseUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseRecordRepository: DoseRecordRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(medicineId: Long): DoseRecord? {
        medicineRepository.find(medicineId) ?: return null
        val record = DoseRecord(id = 0, medicineId = medicineId, takenAt = Instant.now(clock))
        val id = doseRecordRepository.add(record)
        return record.copy(id = id)
    }
}
