package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.MedicineNameMatcher
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 약 이름으로 직전 복용 시각을 찾는다. 음성과 AppFunctions 입구가 그대로 호출한다.
 * 약은 하루 한 시각만 등록되므로 같은 이름을 시각별로 여러 개 등록할 수 있어, 이름이 같은 약 전체에서 가장 최근 기록을 고른다.
 */
class GetLastDoseUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseRecordRepository: DoseRecordRepository,
) {
    suspend operator fun invoke(medicineName: String): LastDoseResult {
        val matched = MedicineNameMatcher.matches(medicineName, medicineRepository.getAll())
        if (matched.isEmpty()) return LastDoseResult.MedicineNotFound
        val latest = doseRecordRepository.findLatest(matched.map { it.id }) ?: return LastDoseResult.NoRecord
        val medicine = matched.first { it.id == latest.medicineId }
        return LastDoseResult.Taken(medicineName = medicine.name, takenAt = latest.takenAt)
    }
}

sealed interface LastDoseResult {
    data object MedicineNotFound : LastDoseResult
    data object NoRecord : LastDoseResult
    data class Taken(val medicineName: String, val takenAt: Instant) : LastDoseResult
}
