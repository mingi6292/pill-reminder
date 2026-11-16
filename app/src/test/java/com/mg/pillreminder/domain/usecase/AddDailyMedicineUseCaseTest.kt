package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.DefaultTimeZoneRule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AddDailyMedicineUseCaseTest {

    @get:Rule
    val timeZoneRule = DefaultTimeZoneRule(SEOUL)

    private val now = seoul("2026-11-16T07:59")
    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val useCase = AddDailyMedicineUseCase(
        repository,
        repository.reminderRepository,
        alarmScheduler,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `약과 매일 알림을 저장한다`() = runTest {
        val id = useCase("비타민", LocalTime.of(8, 0))

        val medicine = repository.medicines.single()
        val reminder = repository.reminders.single()
        assertEquals(id, medicine.id)
        assertEquals("비타민", medicine.name)
        assertEquals(now, medicine.createdAt)
        assertEquals(ReminderSchedule.Daily(LocalTime.of(8, 0)), reminder.schedule)
    }

    @Test
    fun `시각 전이면 다음 알림은 오늘`() = runTest {
        useCase("비타민", LocalTime.of(8, 0))

        assertEquals(seoul("2026-11-16T08:00"), repository.reminders.single().nextTriggerAt)
    }

    @Test
    fun `시각이 지났으면 다음 알림은 내일`() = runTest {
        useCase("비타민", LocalTime.of(7, 30))

        assertEquals(seoul("2026-11-17T07:30"), repository.reminders.single().nextTriggerAt)
    }

    @Test
    fun `저장한 알림을 다음 알림 시각에 예약한다`() = runTest {
        useCase("비타민", LocalTime.of(8, 0))

        val reminder = repository.reminders.single()
        assertEquals(mapOf(reminder.id to seoul("2026-11-16T08:00")), alarmScheduler.scheduled)
    }

    @Test
    fun `이름 앞뒤 공백은 지운다`() = runTest {
        useCase("  비타민 ", LocalTime.of(8, 0))

        assertEquals("비타민", repository.medicines.single().name)
    }

    @Test
    fun `같은 이름도 따로 저장한다`() = runTest {
        useCase("비타민", LocalTime.of(8, 0))
        useCase("비타민", LocalTime.of(20, 0))

        assertEquals(2, repository.medicines.size)
    }

    @Test
    fun `이름이 공백이면 저장하지 않고 예외`() = runTest {
        val result = runCatching { useCase("   ", LocalTime.of(8, 0)) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(repository.medicines.isEmpty())
        assertTrue(alarmScheduler.scheduled.isEmpty())
    }

    private fun seoul(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(SEOUL).toInstant()

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
