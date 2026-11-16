package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.DefaultTimeZoneRule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class RescheduleAllRemindersUseCaseTest {

    @get:Rule
    val timeZoneRule = DefaultTimeZoneRule(SEOUL)

    private val now = seoul("2026-11-16T07:00")
    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val useCase = RescheduleAllRemindersUseCase(
        repository.reminderRepository,
        alarmScheduler,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `저장된 모든 알림을 다시 예약한다`() = runTest {
        val vitamin = saveDaily("비타민", LocalTime.of(8, 0), seoul("2026-11-16T08:00"))
        val probiotic = saveDaily("유산균", LocalTime.of(21, 0), seoul("2026-11-16T21:00"))

        useCase()

        val expected = mapOf(vitamin to seoul("2026-11-16T08:00"), probiotic to seoul("2026-11-16T21:00"))
        assertEquals(expected, alarmScheduler.scheduled)
    }

    @Test
    fun `꺼져 있는 동안 놓친 알림은 그 시각 그대로 예약해 바로 울리게 한다`() = runTest {
        val missedAt = seoul("2026-11-15T21:00")
        val reminderId = saveDaily("유산균", LocalTime.of(21, 0), missedAt)

        useCase()

        assertEquals(missedAt, alarmScheduler.scheduled[reminderId])
    }

    @Test
    fun `타임존이 바뀌면 매일 알림을 새 타임존의 같은 시각으로 다시 계산한다`() = runTest {
        val reminderId = saveDaily("비타민", LocalTime.of(8, 0), seoul("2026-11-16T08:00"))
        val bangkok = ZoneId.of("Asia/Bangkok")
        TimeZone.setDefault(TimeZone.getTimeZone(bangkok))

        useCase()

        // 서울 07:00 = 방콕 05:00이라 방콕 기준 오늘 08:00이 다음 알림이다.
        val expected = LocalDateTime.parse("2026-11-16T08:00").atZone(bangkok).toInstant()
        assertEquals(expected, repository.reminders.single().nextTriggerAt)
        assertEquals(expected, alarmScheduler.scheduled[reminderId])
    }

    @Test
    fun `1회 알림은 저장된 시각 그대로 예약한다`() = runTest {
        val at = seoul("2026-11-16T12:00")
        val reminderId = save("진통제", ReminderSchedule.Once, at)

        useCase()

        assertEquals(at, alarmScheduler.scheduled[reminderId])
    }

    private suspend fun saveDaily(name: String, time: LocalTime, nextTriggerAt: Instant): Long =
        save(name, ReminderSchedule.Daily(time), nextTriggerAt)

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
