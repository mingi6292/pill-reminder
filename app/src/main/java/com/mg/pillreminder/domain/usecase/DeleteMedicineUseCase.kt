package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.repository.MedicineRepository
import javax.inject.Inject

/** 약을 지운다. 알림은 FK CASCADE로 함께 지워진다. 예약된 알람 취소는 AlarmManager 단계에서 추가한다. */
class DeleteMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository,
) {
    suspend operator fun invoke(medicineId: Long) = repository.delete(medicineId)
}
