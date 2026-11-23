package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class AcceptIntervalSuggestionUseCaseTest {

    private val medicineRepository = FakeMedicineRepository()
    private val reminderRepository = medicineRepository.reminderRepository
    private val doseRecordRepository = FakeDoseRecordRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val now = Instant.parse("2026-11-20T14:00:00Z")
    private val useCase = AcceptIntervalSuggestionUseCase(
        medicineRepository,
        reminderRepository,
        doseRecordRepository,
        alarmScheduler,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `마지막 복용 + 주기로 주기 알림을 저장하고 예약한다`() = runTest {
        val medicineId = addMedicine()
        record(medicineId, Instant.parse("2026-11-20T12:00:00Z"))

        val triggerAt = useCase(medicineId, Duration.ofHours(4))

        val expected = Instant.parse("2026-11-20T16:00:00Z")
        assertEquals(expected, triggerAt)
        val saved = medicineRepository.reminders.single { it.schedule is ReminderSchedule.Interval }
        assertEquals(ReminderSchedule.Interval(Duration.ofHours(4)), saved.schedule)
        assertEquals(expected, saved.nextTriggerAt)
        assertEquals(expected, alarmScheduler.scheduled[saved.id])
    }

    @Test
    fun `마지막 복용 + 주기가 이미 지났으면 지금 이후 가장 가까운 주기`() = runTest {
        val medicineId = addMedicine()
        record(medicineId, Instant.parse("2026-11-20T05:00:00Z"))

        val triggerAt = useCase(medicineId, Duration.ofHours(4))

        assertEquals(Instant.parse("2026-11-20T17:00:00Z"), triggerAt)
    }

    @Test
    fun `이미 주기 알림이 있으면 하나 더 만들지 않는다`() = runTest {
        val medicineId = addMedicine()
        record(medicineId, Instant.parse("2026-11-20T12:00:00Z"))
        useCase(medicineId, Duration.ofHours(4))

        assertNull(useCase(medicineId, Duration.ofHours(4)))
        assertEquals(1, medicineRepository.reminders.count { it.schedule is ReminderSchedule.Interval })
    }

    @Test
    fun `복용 기록이 없으면 만들지 않는다`() = runTest {
        val medicineId = addMedicine()

        assertNull(useCase(medicineId, Duration.ofHours(4)))
        assertEquals(1, medicineRepository.reminders.size)
    }

    @Test
    fun `지워진 약이면 null`() = runTest {
        assertNull(useCase(medicineId = 99, interval = Duration.ofHours(4)))
    }

    @Test
    fun `주기가 0이면 예외`() = runTest {
        val error = runCatching { useCase(medicineId = addMedicine(), interval = Duration.ZERO) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private suspend fun addMedicine(): Long = medicineRepository.addWithReminder(
        Medicine(id = 0, name = "진통제", createdAt = Instant.EPOCH),
        Reminder(0, 0, ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = Instant.EPOCH),
    )

    private suspend fun record(medicineId: Long, takenAt: Instant) {
        doseRecordRepository.add(DoseRecord(id = 0, medicineId = medicineId, takenAt = takenAt))
    }
}
