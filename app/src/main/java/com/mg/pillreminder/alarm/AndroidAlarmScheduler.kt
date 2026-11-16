package com.mg.pillreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminderId: Long, triggerAt: Instant) {
        val operation = alarmIntent(reminderId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val triggerAtMillis = triggerAt.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        } else {
            // 정확한 알람 권한이 없으면 조금 늦더라도 울리게 한다. 권한 안내는 권한 처리 단계에서 한다.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    override fun cancel(reminderId: Long) {
        val operation = alarmIntent(reminderId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    // requestCode로 알림마다 다른 PendingIntent를 만든다. extra만 다르면 같은 PendingIntent로 취급된다.
    private fun alarmIntent(reminderId: Long, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        reminderId.toInt(),
        AlarmReceiver.intent(context, reminderId),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )
}
