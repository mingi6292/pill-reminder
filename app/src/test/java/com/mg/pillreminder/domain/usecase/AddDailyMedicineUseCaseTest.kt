package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class AddDailyMedicineUseCaseTest {

    private val now = seoul("2026-11-16T07:59")
    private val repository = FakeMedicineRepository()
    private val useCase = AddDailyMedicineUseCase(repository, Clock.fixed(now, ZoneOffset.UTC))
    private lateinit var originalZone: TimeZone

    // UseCase가 사용 시점의 ZoneId.systemDefault()를 쓰므로 테스트 동안 서울로 고정한다.
    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(SEOUL))
    }

    @After
    fun tearDown() = TimeZone.setDefault(originalZone)

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
    }

    private fun seoul(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(SEOUL).toInstant()

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
