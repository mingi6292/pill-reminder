package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class DeleteMedicineUseCaseTest {

    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val useCase = DeleteMedicineUseCase(repository, repository.reminderRepository, alarmScheduler)

    @Test
    fun `지정한 약만 지운다`() = runTest {
        val vitaminId = repository.addWithReminder(medicine("비타민"), reminder())
        repository.addWithReminder(medicine("유산균"), reminder())

        useCase(vitaminId)

        assertEquals(listOf("유산균"), repository.medicines.map { it.name })
    }

    @Test
    fun `지운 약의 알람만 취소한다`() = runTest {
        val vitaminId = repository.addWithReminder(medicine("비타민"), reminder())
        repository.addWithReminder(medicine("유산균"), reminder())
        repository.reminders.forEach { alarmScheduler.schedule(it.id, it.nextTriggerAt) }
        val remainingReminderId = repository.reminders.single { it.medicineId != vitaminId }.id

        useCase(vitaminId)

        assertEquals(setOf(remainingReminderId), alarmScheduler.scheduled.keys)
    }

    private fun medicine(name: String) = Medicine(id = 0, name = name, createdAt = Instant.EPOCH)

    private fun reminder() = Reminder(
        id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH,
    )
}
