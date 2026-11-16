package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class DeleteMedicineUseCaseTest {

    private val repository = FakeMedicineRepository()
    private val useCase = DeleteMedicineUseCase(repository)

    @Test
    fun `지정한 약만 지운다`() = runTest {
        val vitaminId = repository.addWithReminder(medicine("비타민"), reminder())
        repository.addWithReminder(medicine("유산균"), reminder())

        useCase(vitaminId)

        assertEquals(listOf("유산균"), repository.medicines.map { it.name })
    }

    private fun medicine(name: String) = Medicine(id = 0, name = name, createdAt = Instant.EPOCH)

    private fun reminder() = Reminder(
        id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH,
    )
}
