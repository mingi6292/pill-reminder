package com.mg.pillreminder.data.local

import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class ReminderMapperTest {

    @Test
    fun `Daily는 timeOfDayMinutes로 저장하고 되돌린다`() {
        val entity = reminder(ReminderSchedule.Daily(LocalTime.of(8, 30))).toEntity()

        assertEquals("DAILY", entity.type)
        assertEquals(510, entity.timeOfDayMinutes)
        assertEquals(ReminderSchedule.Daily(LocalTime.of(8, 30)), entity.toDomain().schedule)
    }

    @Test
    fun `Once는 스케줄 컬럼 없이 저장하고 되돌린다`() {
        val entity = reminder(ReminderSchedule.Once).toEntity()

        assertEquals("ONCE", entity.type)
        assertEquals(null, entity.timeOfDayMinutes)
        assertEquals(null, entity.intervalMinutes)
        assertEquals(ReminderSchedule.Once, entity.toDomain().schedule)
    }

    @Test
    fun `Interval은 intervalMinutes로 저장하고 되돌린다`() {
        val entity = reminder(ReminderSchedule.Interval(Duration.ofHours(4))).toEntity()

        assertEquals("INTERVAL", entity.type)
        assertEquals(240L, entity.intervalMinutes)
        assertEquals(ReminderSchedule.Interval(Duration.ofHours(4)), entity.toDomain().schedule)
    }

    @Test
    fun `공통 필드를 보존한다`() {
        val original = reminder(ReminderSchedule.Once)

        val restored = original.toEntity().copy(id = 7).toDomain()

        assertEquals(original.copy(id = 7), restored)
    }

    @Test
    fun `알 수 없는 type이면 예외`() {
        val entity = ReminderEntity(
            id = 1, medicineId = 1, type = "WEEKLY",
            timeOfDayMinutes = null, intervalMinutes = null, nextTriggerAt = 0,
        )

        assertThrows(IllegalStateException::class.java) { entity.toDomain() }
    }

    private fun reminder(schedule: ReminderSchedule) = Reminder(
        id = 0,
        medicineId = 3,
        schedule = schedule,
        nextTriggerAt = Instant.parse("2026-11-16T23:00:00Z"),
    )
}
