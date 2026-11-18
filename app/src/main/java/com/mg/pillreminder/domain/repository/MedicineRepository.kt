package com.mg.pillreminder.domain.repository

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun observeWithNextReminder(): Flow<List<MedicineWithNextReminder>>

    suspend fun getAll(): List<Medicine>

    suspend fun find(medicineId: Long): Medicine?

    /** 약과 알림을 함께 저장하고 새 약 id를 돌려준다. medicine.id, reminder.id, reminder.medicineId는 무시한다. */
    suspend fun addWithReminder(medicine: Medicine, reminder: Reminder): Long

    /** 연결된 Reminder도 함께 지워진다. 예약된 알람 취소는 호출하는 쪽 책임. */
    suspend fun delete(medicineId: Long)
}
