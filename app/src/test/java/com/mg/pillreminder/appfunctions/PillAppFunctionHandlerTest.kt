package com.mg.pillreminder.appfunctions

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.usecase.GetLastDoseUseCase
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail

import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class PillAppFunctionHandlerTest {

    private val now = Instant.parse("2026-11-20T02:20:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val medicineRepository = FakeMedicineRepository()
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val handler = PillAppFunctionHandler(
        medicineRepository,
        RecordDoseUseCase(medicineRepository, doseRecordRepository, medicineRepository.reminderRepository, alarmScheduler, clock),
        GetLastDoseUseCase(medicineRepository, doseRecordRepository),
        ScheduleOnceReminderUseCase(medicineRepository, medicineRepository.reminderRepository, alarmScheduler, clock),
    )

    @Test
    fun `복용을 기록하고 등록된 이름과 시각을 돌려준다`() = runTest {
        addMedicine("생리통약")

        val result = handler.recordDose("생리 통약")

        assertEquals(RecordedDose("생리통약", now), result)
        assertEquals(now, doseRecordRepository.records.single().takenAt)
    }

    @Test
    fun `등록되지 않은 약을 기록하면 MedicineNotRegistered`() = runTest {
        assertFailsWith<PillAppFunctionError.MedicineNotRegistered> {
            handler.recordDose("생리통약")
        }
        assertEquals(emptyList<Any>(), doseRecordRepository.records)
    }

    @Test
    fun `마지막 복용 시각을 돌려준다`() = runTest {
        val id = addMedicine("생리통약")
        handler.recordDose("생리통약")

        assertEquals(LastDose("생리통약", now), handler.getLastDose("생리통약"))
        assertEquals(id, doseRecordRepository.records.single().medicineId)
    }

    @Test
    fun `기록이 없으면 시각은 null`() = runTest {
        addMedicine("생리통약")

        val result = handler.getLastDose("생리통약")

        assertEquals("생리통약", result.medicineName)
        assertNull(result.takenAt)
    }

    @Test
    fun `등록되지 않은 약을 조회하면 MedicineNotRegistered`() = runTest {
        assertFailsWith<PillAppFunctionError.MedicineNotRegistered> {
            handler.getLastDose("생리통약")
        }
    }

    @Test
    fun `N분 뒤 1회 알림을 예약하고 울릴 시각을 돌려준다`() = runTest {
        addMedicine("생리통약")

        val result = handler.scheduleReminderAfter("생리통약", minutes = 120)

        val expected = Instant.parse("2026-11-20T04:20:00Z")
        assertEquals(ScheduledReminder("생리통약", expected), result)
        val once = medicineRepository.reminders.single { it.schedule == ReminderSchedule.Once }
        assertEquals(expected, alarmScheduler.scheduled[once.id])
    }

    @Test
    fun `0분 이하는 InvalidArgument이고 예약하지 않는다`() = runTest {
        addMedicine("생리통약")

        listOf(0, -5).forEach { minutes ->
            assertFailsWith<PillAppFunctionError.InvalidArgument> {
                handler.scheduleReminderAfter("생리통약", minutes)
            }
        }
        assertEquals(emptyMap<Long, Instant>(), alarmScheduler.scheduled)
    }

    @Test
    fun `등록되지 않은 약의 알림 예약은 MedicineNotRegistered`() = runTest {
        assertFailsWith<PillAppFunctionError.MedicineNotRegistered> {
            handler.scheduleReminderAfter("생리통약", 30)
        }
    }

    private suspend inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return
            throw error
        }
        fail("expected ${T::class.simpleName}")
    }

    private suspend fun addMedicine(name: String): Long = medicineRepository.addWithReminder(
        Medicine(id = 0, name = name, createdAt = Instant.EPOCH),
        Reminder(id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = now),
    )
}
