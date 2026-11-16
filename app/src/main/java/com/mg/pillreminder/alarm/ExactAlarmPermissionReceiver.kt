package com.mg.pillreminder.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mg.pillreminder.domain.usecase.RescheduleAllRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 권한이 없을 때 부정확한 알람으로 예약해 둔 알림을 정확한 알람으로 바꾼다.
 * 이 브로드캐스트는 권한을 받았을 때만 온다. 권한을 뺏기면 시스템이 앱 예약을 모두 지운다.
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleAllReminders: RescheduleAllRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        launchAsync { rescheduleAllReminders() }
    }
}
