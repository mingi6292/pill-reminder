package com.mg.pillreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun findById(reminderId: Long): ReminderEntity?

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE medicineId = :medicineId")
    suspend fun getByMedicineId(medicineId: Long): List<ReminderEntity>

    @Query("UPDATE reminders SET nextTriggerAt = :nextTriggerAt WHERE id = :reminderId")
    suspend fun updateNextTriggerAt(reminderId: Long, nextTriggerAt: Long)
}
