package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeAlarmScheduler
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

class ScheduleOnceReminderUseCaseTest {

    private val now = Instant.parse("2026-11-20T02:20:00Z")
    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val useCase = ScheduleOnceReminderUseCase(
        repository,
        repository.reminderRepository,
        alarmScheduler,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `지금부터 N시간 뒤 1회 알림을 저장하고 예약한다`() = runTest {
        val medicineId = addDailyMedicine("생리통약")

        val triggerAt = useCase(medicineId, Duration.ofHours(4))

        val expected = Instant.parse("2026-11-20T06:20:00Z")
        assertEquals(expected, triggerAt)
        val once = repository.reminders.single { it.schedule == ReminderSchedule.Once }
        assertEquals(medicineId, once.medicineId)
        assertEquals(expected, once.nextTriggerAt)
        assertEquals(expected, alarmScheduler.scheduled[once.id])
    }

    @Test
    fun `매일 알림은 그대로 둔다`() = runTest {
        val medicineId = addDailyMedicine("생리통약")
        val daily = repository.reminders.single()

        useCase(medicineId, Duration.ofHours(1))

        assertTrue(daily in repository.reminders)
    }

    @Test
    fun `이미 1회 알림이 있으면 새 시각으로 바꾸고 이전 예약은 취소한다`() = runTest {
        val medicineId = addDailyMedicine("생리통약")
        useCase(medicineId, Duration.ofHours(1))
        val previous = repository.reminders.single { it.schedule == ReminderSchedule.Once }

        useCase(medicineId, Duration.ofHours(3))

        val current = repository.reminders.single { it.schedule == ReminderSchedule.Once }
        assertEquals(Instant.parse("2026-11-20T05:20:00Z"), current.nextTriggerAt)
        assertNull(alarmScheduler.scheduled[previous.id])
        assertEquals(current.nextTriggerAt, alarmScheduler.scheduled[current.id])
    }

    @Test
    fun `다른 약의 1회 알림은 건드리지 않는다`() = runTest {
        val painkiller = addDailyMedicine("생리통약")
        val vitamin = addDailyMedicine("비타민")
        useCase(vitamin, Duration.ofHours(2))

        useCase(painkiller, Duration.ofHours(1))

        val onceMedicines = repository.reminders.filter { it.schedule == ReminderSchedule.Once }.map { it.medicineId }
        assertEquals(setOf(painkiller, vitamin), onceMedicines.toSet())
    }

    @Test
    fun `지워진 약이면 예약하지 않고 null`() = runTest {
        val triggerAt = useCase(medicineId = 99, after = Duration.ofHours(1))

        assertNull(triggerAt)
        assertTrue(repository.reminders.isEmpty())
        assertTrue(alarmScheduler.scheduled.isEmpty())
    }

    @Test
    fun `0이나 음수 시간은 받지 않는다`() = runTest {
        val medicineId = addDailyMedicine("생리통약")

        listOf(Duration.ZERO, Duration.ofHours(-1)).forEach { after ->
            val error = runCatching { useCase(medicineId, after) }.exceptionOrNull()
            assertTrue("after=$after", error is IllegalArgumentException)
        }
        assertTrue(alarmScheduler.scheduled.isEmpty())
    }

    private suspend fun addDailyMedicine(name: String): Long = repository.addWithReminder(
        Medicine(id = 0, name = name, createdAt = Instant.EPOCH),
        Reminder(id = 0, medicineId = 0, schedule = ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt = now),
    )
}
