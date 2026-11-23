package com.mg.pillreminder.domain.usecase

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

class GetIntervalSuggestionUseCaseTest {

    private val medicineRepository = FakeMedicineRepository()
    private val reminderRepository = medicineRepository.reminderRepository
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val useCase = GetIntervalSuggestionUseCase(medicineRepository, reminderRepository, doseRecordRepository)
    private val reject = RejectIntervalSuggestionUseCase(medicineRepository)

    @Test
    fun `4시간 간격으로 4번 먹었으면 4시간을 제안한다`() = runTest {
        val medicineId = addMedicine()
        recordEvery(medicineId, Duration.ofHours(4), times = 4)

        assertEquals(Duration.ofHours(4), useCase(medicineId))
    }

    @Test
    fun `기록이 부족하면 제안하지 않는다`() = runTest {
        val medicineId = addMedicine()
        recordEvery(medicineId, Duration.ofHours(4), times = 3)

        assertNull(useCase(medicineId))
    }

    @Test
    fun `다른 약의 기록은 섞지 않는다`() = runTest {
        val medicineId = addMedicine()
        val otherId = addMedicine()
        recordEvery(medicineId, Duration.ofHours(4), times = 2)
        recordEvery(otherId, Duration.ofHours(4), times = 2)

        assertNull(useCase(medicineId))
    }

    @Test
    fun `이미 주기 알림이 있으면 제안하지 않는다`() = runTest {
        val medicineId = addMedicine()
        recordEvery(medicineId, Duration.ofHours(4), times = 4)
        reminderRepository.add(
            Reminder(0, medicineId, ReminderSchedule.Interval(Duration.ofHours(4)), nextTriggerAt = Instant.EPOCH),
        )

        assertNull(useCase(medicineId))
    }

    @Test
    fun `거절하면 다시 제안하지 않는다`() = runTest {
        val medicineId = addMedicine()
        recordEvery(medicineId, Duration.ofHours(4), times = 4)

        reject(medicineId)

        assertNull(useCase(medicineId))
    }

    @Test
    fun `지워진 약이면 null`() = runTest {
        assertNull(useCase(medicineId = 99))
    }

    private suspend fun addMedicine(): Long = medicineRepository.addWithReminder(
        Medicine(id = 0, name = "진통제", createdAt = Instant.EPOCH),
        Reminder(0, 0, ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH),
    )

    private suspend fun recordEvery(medicineId: Long, interval: Duration, times: Int) = repeat(times) { index ->
        val takenAt = START + interval.multipliedBy(index.toLong())
        doseRecordRepository.add(DoseRecord(id = 0, medicineId = medicineId, takenAt = takenAt))
    }

    private companion object {
        val START: Instant = Instant.parse("2026-11-20T00:00:00Z")
    }
}
