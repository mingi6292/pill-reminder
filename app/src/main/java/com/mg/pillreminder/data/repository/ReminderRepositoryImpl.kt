package com.mg.pillreminder.data.repository

import com.mg.pillreminder.data.local.ReminderDao
import com.mg.pillreminder.data.local.toDomain
import com.mg.pillreminder.data.local.toEntity
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.repository.ReminderRepository
import java.time.Instant
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
) : ReminderRepository {

    override suspend fun add(reminder: Reminder): Long = reminderDao.insert(reminder.toEntity())

    override suspend fun find(reminderId: Long): Reminder? = reminderDao.findById(reminderId)?.toDomain()

    override suspend fun getAll(): List<Reminder> = reminderDao.getAll().map { it.toDomain() }

    override suspend fun getByMedicine(medicineId: Long): List<Reminder> =
        reminderDao.getByMedicineId(medicineId).map { it.toDomain() }

    override suspend fun updateNextTriggerAt(reminderId: Long, nextTriggerAt: Instant) =
        reminderDao.updateNextTriggerAt(reminderId, nextTriggerAt.toEpochMilli())

    override suspend fun delete(reminderId: Long) = reminderDao.deleteById(reminderId)
}
