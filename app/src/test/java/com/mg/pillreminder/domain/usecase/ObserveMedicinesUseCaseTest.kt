package com.mg.pillreminder.domain.usecase

import app.cash.turbine.test
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class ObserveMedicinesUseCaseTest {

    private val repository = FakeMedicineRepository()
    private val useCase = ObserveMedicinesUseCase(repository)

    @Test
    fun `저장된 약과 다음 알림 시각을 흘려보낸다`() = runTest {
        useCase().test {
            assertEquals(emptyList<Any>(), awaitItem())

            repository.addWithReminder(medicine("비타민"), reminder(NEXT))

            val item = awaitItem().single()
            assertEquals("비타민", item.medicine.name)
            assertEquals(NEXT, item.nextTriggerAt)
        }
    }

    private fun medicine(name: String) = Medicine(id = 0, name = name, createdAt = Instant.EPOCH)

    private fun reminder(nextTriggerAt: Instant) = Reminder(
        id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = nextTriggerAt,
    )

    private companion object {
        val NEXT: Instant = Instant.parse("2026-11-16T23:00:00Z")
    }
}
