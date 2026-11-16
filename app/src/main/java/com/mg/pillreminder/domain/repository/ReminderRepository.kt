package com.mg.pillreminder.domain.repository

import com.mg.pillreminder.domain.model.Reminder
import java.time.Instant

interface ReminderRepository {
    /** reminder.id는 무시하고 새 id를 돌려준다. */
    suspend fun add(reminder: Reminder): Long

    suspend fun find(reminderId: Long): Reminder?

    suspend fun getAll(): List<Reminder>

    suspend fun getByMedicine(medicineId: Long): List<Reminder>

    suspend fun updateNextTriggerAt(reminderId: Long, nextTriggerAt: Instant)
}
