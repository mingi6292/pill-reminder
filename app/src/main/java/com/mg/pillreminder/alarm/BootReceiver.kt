package com.mg.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mg.pillreminder.domain.usecase.RescheduleAllRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 재부팅하면 AlarmManager 예약이 사라지고, 시각·타임존이 바뀌면 매일 알림 시각이 틀어져서 다시 예약한다. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleAllReminders: RescheduleAllRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        launchAsync { rescheduleAllReminders() }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
