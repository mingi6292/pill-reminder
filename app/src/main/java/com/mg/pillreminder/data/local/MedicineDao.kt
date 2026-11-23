package com.mg.pillreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert
    suspend fun insert(medicine: MedicineEntity): Long

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long

    /** 알림 없는 약이 남지 않도록 약과 알림을 한 트랜잭션으로 넣는다. reminder.medicineId는 무시한다. */
    @Transaction
    suspend fun insertWithReminder(medicine: MedicineEntity, reminder: ReminderEntity): Long {
        val medicineId = insert(medicine)
        insertReminder(reminder.copy(medicineId = medicineId))
        return medicineId
    }

    @Query("SELECT * FROM medicines ORDER BY createdAt")
    suspend fun getAll(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :medicineId")
    suspend fun findById(medicineId: Long): MedicineEntity?

    @Query("UPDATE medicines SET intervalSuggestionDismissed = 1 WHERE id = :medicineId")
    suspend fun dismissIntervalSuggestion(medicineId: Long)

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
