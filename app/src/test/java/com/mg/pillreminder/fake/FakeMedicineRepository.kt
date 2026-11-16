package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMedicineRepository : MedicineRepository {

    private val saved = MutableStateFlow<List<Pair<Medicine, Reminder>>>(emptyList())
    private var nextId = 1L

    val medicines: List<Medicine> get() = saved.value.map { it.first }
    val reminders: List<Reminder> get() = saved.value.map { it.second }

    override fun observeWithNextReminder(): Flow<List<MedicineWithNextReminder>> =
        saved.map { list -> list.map { (medicine, reminder) -> MedicineWithNextReminder(medicine, reminder.nextTriggerAt) } }

    override suspend fun addWithReminder(medicine: Medicine, reminder: Reminder): Long {
        val id = nextId++
        saved.update { it + (medicine.copy(id = id) to reminder.copy(id = id, medicineId = id)) }
        return id
    }

    override suspend fun delete(medicineId: Long) {
        saved.update { list -> list.filterNot { it.first.id == medicineId } }
    }
}
