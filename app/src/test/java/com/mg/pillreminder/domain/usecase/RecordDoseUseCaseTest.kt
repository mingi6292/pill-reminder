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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class RecordDoseUseCaseTest {

    private val medicineRepository = FakeMedicineRepository()
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val now = Instant.parse("2026-11-18T02:20:00Z")
    private val useCase = RecordDoseUseCase(medicineRepository, doseRecordRepository, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `지금 시각으로 기록하고 저장한 기록을 돌려준다`() = runTest {
        val medicineId = addMedicine("생리통약")

        val record = useCase(medicineId)

        val expected = DoseRecord(id = 1, medicineId = medicineId, takenAt = now)
        assertEquals(expected, record)
        assertEquals(listOf(expected), doseRecordRepository.records)
    }

    @Test
    fun `짧은 간격으로 두 번 기록해도 둘 다 남긴다`() = runTest {
        val medicineId = addMedicine("생리통약")

        useCase(medicineId)
        useCase(medicineId)

        assertEquals(2, doseRecordRepository.records.size)
    }

    @Test
    fun `지워진 약이면 기록하지 않고 null`() = runTest {
        val record = useCase(medicineId = 99)

        assertNull(record)
        assertTrue(doseRecordRepository.records.isEmpty())
    }

    private suspend fun addMedicine(name: String): Long = medicineRepository.addWithReminder(
        Medicine(id = 0, name = name, createdAt = Instant.EPOCH),
        Reminder(id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH),
    )
}
