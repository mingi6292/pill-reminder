package com.mg.pillreminder.domain.usecase

import app.cash.turbine.test
import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class ObserveMedicinesUseCaseTest {

    private val medicineRepository = FakeMedicineRepository()
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val useCase = ObserveMedicinesUseCase(
        medicineRepository,
        doseRecordRepository,
        GetIntervalSuggestionUseCase(medicineRepository, medicineRepository.reminderRepository, doseRecordRepository),
    )

    @Test
    fun `같은 간격으로 4번 기록하면 제안이 붙고 거절하면 사라진다`() = runTest {
        val medicineId = medicineRepository.addWithReminder(medicine("진통제"), reminder(NEXT))
        repeat(3) { recordAt(medicineId, TAKEN + Duration.ofHours(4L * it)) }

        useCase().test {
            assertNull(awaitItem().single().suggestedInterval)

            recordAt(medicineId, TAKEN + Duration.ofHours(12))
            assertEquals(Duration.ofHours(4), awaitItem().single().suggestedInterval)

            RejectIntervalSuggestionUseCase(medicineRepository)(medicineId)
            assertNull(awaitItem().single().suggestedInterval)
        }
    }

    private suspend fun recordAt(medicineId: Long, takenAt: Instant) {
        doseRecordRepository.add(DoseRecord(id = 0, medicineId = medicineId, takenAt = takenAt))
    }

    @Test
    fun `저장된 약과 다음 알림 시각을 흘려보낸다`() = runTest {
        useCase().test {
            assertEquals(emptyList<Any>(), awaitItem())

            medicineRepository.addWithReminder(medicine("비타민"), reminder(NEXT))

            val item = awaitItem().single()
            assertEquals("비타민", item.medicine.name)
            assertEquals(NEXT, item.nextTriggerAt)
            assertNull(item.lastTakenAt)
        }
    }

    @Test
    fun `복용을 기록하면 약별 마지막 복용 시각이 붙는다`() = runTest {
        val vitamin = medicineRepository.addWithReminder(medicine("비타민"), reminder(NEXT))
        medicineRepository.addWithReminder(medicine("유산균"), reminder(NEXT))

        useCase().test {
            awaitItem()

            doseRecordRepository.add(DoseRecord(id = 0, medicineId = vitamin, takenAt = TAKEN))

            val items = awaitItem()
            assertEquals(TAKEN, items.single { it.medicine.id == vitamin }.lastTakenAt)
            assertNull(items.single { it.medicine.id != vitamin }.lastTakenAt)
        }
    }

    private fun medicine(name: String) = Medicine(id = 0, name = name, createdAt = Instant.EPOCH)

    private fun reminder(nextTriggerAt: Instant) = Reminder(
        id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = nextTriggerAt,
    )

    private companion object {
        val NEXT: Instant = Instant.parse("2026-11-16T23:00:00Z")
        val TAKEN: Instant = Instant.parse("2026-11-16T23:05:00Z")
    }
}
