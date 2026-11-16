package com.mg.pillreminder.domain.repository

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun observeWithNextReminder(): Flow<List<MedicineWithNextReminder>>

    /** medicine.id는 무시하고 새 id를 돌려준다. */
    suspend fun add(medicine: Medicine): Long

    /** 연결된 Reminder도 함께 지워진다. 예약된 알람 취소는 호출하는 쪽 책임. */
    suspend fun delete(medicineId: Long)
}
