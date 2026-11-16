package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.alarm.AlarmScheduler
import java.time.Instant

class FakeAlarmScheduler : AlarmScheduler {

    private val _scheduled = mutableMapOf<Long, Instant>()

    /** reminderId → 예약된 시각 */
    val scheduled: Map<Long, Instant> get() = _scheduled

    override fun schedule(reminderId: Long, triggerAt: Instant) {
        _scheduled[reminderId] = triggerAt
    }

    override fun cancel(reminderId: Long) {
        _scheduled.remove(reminderId)
    }
}
