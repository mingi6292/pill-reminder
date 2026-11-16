package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.DueReminder
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.DefaultTimeZoneRule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class HandleReminderFiredUseCaseTest {

    @get:Rule
    val timeZoneRule = DefaultTimeZoneRule(SEOUL)

    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()

    @Test
    fun `울린 알림의 약 이름을 돌려준다`() = runTest {
        val reminderId = saveDaily("비타민", seoul("2026-11-16T08:00"))

        val due = fireAt(seoul("2026-11-16T08:00"), reminderId)

        assertEquals(DueReminder(reminderId, "비타민"), due)
    }

    @Test
    fun `매일 알림은 다음 날 같은 시각으로 저장하고 예약한다`() = runTest {
        val reminderId = saveDaily("비타민", seoul("2026-11-16T08:00"))

        fireAt(seoul("2026-11-16T08:00"), reminderId)

        val tomorrow = seoul("2026-11-17T08:00")
        assertEquals(tomorrow, repository.reminders.single().nextTriggerAt)
        assertEquals(mapOf(reminderId to tomorrow), alarmScheduler.scheduled)
    }

    @Test
    fun `조금 일찍 울려도 같은 날 다시 예약하지 않는다`() = runTest {
        val reminderId = saveDaily("비타민", seoul("2026-11-16T08:00"))

        fireAt(seoul("2026-11-16T07:59:59"), reminderId)

        assertEquals(seoul("2026-11-17T08:00"), alarmScheduler.scheduled[reminderId])
    }

    @Test
    fun `늦게 울려도 다음 날 같은 시각으로 예약한다`() = runTest {
        val reminderId = saveDaily("비타민", seoul("2026-11-16T08:00"))

        fireAt(seoul("2026-11-16T08:40"), reminderId)

        assertEquals(seoul("2026-11-17T08:00"), alarmScheduler.scheduled[reminderId])
    }

    @Test
    fun `주기 알림은 예정 시각에서 주기만큼 뒤로 예약한다`() = runTest {
        val reminderId = save("진통제", ReminderSchedule.Interval(Duration.ofHours(4)), seoul("2026-11-16T12:00"))

        fireAt(seoul("2026-11-16T12:00"), reminderId)

        assertEquals(seoul("2026-11-16T16:00"), alarmScheduler.scheduled[reminderId])
    }

    @Test
    fun `1회 알림은 울린 뒤 지우고 다시 예약하지 않는다`() = runTest {
        val reminderId = save("진통제", ReminderSchedule.Once, seoul("2026-11-16T12:00"))

        val due = fireAt(seoul("2026-11-16T12:00"), reminderId)

        assertEquals("진통제", due?.medicineName)
        assertTrue(repository.reminders.isEmpty())
        assertTrue(alarmScheduler.scheduled.isEmpty())
    }

    @Test
    fun `이미 지운 알림이면 null이고 예약하지 않는다`() = runTest {
        val due = fireAt(seoul("2026-11-16T08:00"), reminderId = 99)

        assertNull(due)
        assertTrue(alarmScheduler.scheduled.isEmpty())
    }

    private suspend fun fireAt(now: Instant, reminderId: Long): DueReminder? = HandleReminderFiredUseCase(
        repository,
        repository.reminderRepository,
        alarmScheduler,
        Clock.fixed(now, ZoneOffset.UTC),
    )(reminderId)

    private suspend fun saveDaily(name: String, nextTriggerAt: Instant): Long =
        save(name, ReminderSchedule.Daily(LocalTime.of(8, 0)), nextTriggerAt)

    private suspend fun save(name: String, schedule: ReminderSchedule, nextTriggerAt: Instant): Long {
        val medicineId = repository.addWithReminder(
            Medicine(id = 0, name = name, createdAt = Instant.EPOCH),
            Reminder(id = 0, medicineId = 0, schedule = schedule, nextTriggerAt = nextTriggerAt),
        )
        return repository.reminders.single { it.medicineId == medicineId }.id
    }

    private fun seoul(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(SEOUL).toInstant()

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
