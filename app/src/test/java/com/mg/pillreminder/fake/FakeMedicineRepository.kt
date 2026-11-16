package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Room처럼 약과 알림을 한 상태에 둔다. 약을 지우면 알림도 지워지는 FK CASCADE를 흉내 내려고
 * [reminderRepository]가 같은 상태를 쓴다. 상태가 하나라 약과 알림을 함께 넣어도 Flow가 한 번만 흐른다.
 */
class FakeMedicineRepository : MedicineRepository {

    private data class State(
        val medicines: List<Medicine> = emptyList(),
        val reminders: List<Reminder> = emptyList(),
    )

    private val state = MutableStateFlow(State())
    private var nextMedicineId = 1L
    private var nextReminderId = 1L

    val medicines: List<Medicine> get() = state.value.medicines
    val reminders: List<Reminder> get() = state.value.reminders

    val reminderRepository: ReminderRepository = Reminders()

    override fun observeWithNextReminder(): Flow<List<MedicineWithNextReminder>> = state.map { current ->
        current.medicines.map { medicine ->
            val next = current.reminders.filter { it.medicineId == medicine.id }.minOfOrNull { it.nextTriggerAt }
            MedicineWithNextReminder(medicine, next)
        }
    }

    override suspend fun find(medicineId: Long): Medicine? = medicines.find { it.id == medicineId }

    override suspend fun addWithReminder(medicine: Medicine, reminder: Reminder): Long {
        val medicineId = nextMedicineId++
        val savedReminder = reminder.copy(id = nextReminderId++, medicineId = medicineId)
        state.update {
            it.copy(medicines = it.medicines + medicine.copy(id = medicineId), reminders = it.reminders + savedReminder)
        }
        return medicineId
    }

    override suspend fun delete(medicineId: Long) = state.update { current ->
        current.copy(
            medicines = current.medicines.filterNot { it.id == medicineId },
            reminders = current.reminders.filterNot { it.medicineId == medicineId },
        )
    }

    private inner class Reminders : ReminderRepository {
        override suspend fun add(reminder: Reminder): Long {
            val id = nextReminderId++
            state.update { it.copy(reminders = it.reminders + reminder.copy(id = id)) }
            return id
        }

        override suspend fun find(reminderId: Long): Reminder? = reminders.find { it.id == reminderId }

        override suspend fun getAll(): List<Reminder> = reminders

        override suspend fun getByMedicine(medicineId: Long): List<Reminder> =
            reminders.filter { it.medicineId == medicineId }

        override suspend fun updateNextTriggerAt(reminderId: Long, nextTriggerAt: Instant) = updateReminders { list ->
            list.map { if (it.id == reminderId) it.copy(nextTriggerAt = nextTriggerAt) else it }
        }

        override suspend fun delete(reminderId: Long) = updateReminders { list -> list.filterNot { it.id == reminderId } }

        private fun updateReminders(transform: (List<Reminder>) -> List<Reminder>) =
            state.update { it.copy(reminders = transform(it.reminders)) }
    }
}
