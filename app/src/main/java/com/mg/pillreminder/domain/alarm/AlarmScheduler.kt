package com.mg.pillreminder.domain.alarm

import java.time.Instant

/** 알림 시각에 앱을 깨우는 예약. 같은 reminderId로 다시 예약하면 기존 예약을 덮어쓴다. */
interface AlarmScheduler {
    fun schedule(reminderId: Long, triggerAt: Instant)

    fun cancel(reminderId: Long)
}
