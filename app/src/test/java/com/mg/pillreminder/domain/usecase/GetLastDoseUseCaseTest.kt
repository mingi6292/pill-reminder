package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class GetLastDoseUseCaseTest {

    private val medicineRepository = FakeMedicineRepository()
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val useCase = GetLastDoseUseCase(medicineRepository, doseRecordRepository)

    @Test
    fun `가장 최근 복용 시각을 돌려준다`() = runTest {
        val id = addMedicine("생리통약")
        recordDose(id, EARLIER)
        recordDose(id, LATER)

        assertEquals(LastDoseResult.Taken("생리통약", LATER), useCase("생리 통약"))
    }

    @Test
    fun `다른 약의 기록은 보지 않는다`() = runTest {
        val target = addMedicine("생리통약")
        val other = addMedicine("비타민")
        recordDose(target, EARLIER)
        recordDose(other, LATER)

        assertEquals(LastDoseResult.Taken("생리통약", EARLIER), useCase("생리통약"))
    }

    @Test
    fun `이름이 같은 약이 여러 개면 그중 가장 최근 기록`() = runTest {
        val morning = addMedicine("비타민")
        val evening = addMedicine("비타민")
        recordDose(evening, EARLIER)
        recordDose(morning, LATER)

        assertEquals(LastDoseResult.Taken("비타민", LATER), useCase("비타민"))
    }

    @Test
    fun `등록된 약이 아니면 MedicineNotFound`() = runTest {
        addMedicine("비타민")

        assertEquals(LastDoseResult.MedicineNotFound, useCase("생리통약"))
    }

    @Test
    fun `기록이 없으면 NoRecord`() = runTest {
        addMedicine("생리통약")

        assertEquals(LastDoseResult.NoRecord, useCase("생리통약"))
    }

    private suspend fun addMedicine(name: String): Long = medicineRepository.addWithReminder(
        Medicine(id = 0, name = name, createdAt = Instant.EPOCH),
        Reminder(id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH),
    )

    private suspend fun recordDose(medicineId: Long, takenAt: Instant) {
        doseRecordRepository.add(DoseRecord(id = 0, medicineId = medicineId, takenAt = takenAt))
    }

    private companion object {
        val EARLIER: Instant = Instant.parse("2026-11-17T23:00:00Z")
        val LATER: Instant = Instant.parse("2026-11-18T02:20:00Z")
    }
}
