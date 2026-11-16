package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.alarm.AlarmScheduler
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import javax.inject.Inject

/** 약을 지우고 예약된 알람을 취소한다. 알림 데이터는 FK CASCADE로 함께 지워진다. */
class DeleteMedicineUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(medicineId: Long) {
        val reminderIds = reminderRepository.getByMedicine(medicineId).map { it.id }
        // 취소보다 삭제를 먼저 한다. 취소가 빠져도 울린 알람은 지워진 알림을 못 찾고 조용히 끝난다.
        medicineRepository.delete(medicineId)
        reminderIds.forEach(alarmScheduler::cancel)
    }
}
