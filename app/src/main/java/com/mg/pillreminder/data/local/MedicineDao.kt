package com.mg.pillreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert
    suspend fun insert(medicine: MedicineEntity): Long

    @Query("DELETE FROM medicines WHERE id = :medicineId")
    suspend fun deleteById(medicineId: Long)

    @Query(
        """
        SELECT medicines.*, MIN(reminders.nextTriggerAt) AS nextTriggerAt
        FROM medicines
        LEFT JOIN reminders ON reminders.medicineId = medicines.id
        GROUP BY medicines.id
        ORDER BY medicines.createdAt
        """,
    )
    fun observeWithNextTrigger(): Flow<List<MedicineWithNextTriggerRow>>
}
