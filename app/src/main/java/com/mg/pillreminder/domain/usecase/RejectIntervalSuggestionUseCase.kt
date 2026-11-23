package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.repository.MedicineRepository
import javax.inject.Inject

/** 이 약의 주기 제안을 다시 띄우지 않는다. 거절은 영구다(design.md 8장). */
class RejectIntervalSuggestionUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
) {
    suspend operator fun invoke(medicineId: Long) = medicineRepository.dismissIntervalSuggestion(medicineId)
}
