package com.mg.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import com.mg.pillreminder.notification.ReminderNotifier
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import javax.inject.Inject

/** 알림의 "1시간 뒤" 버튼. 1회 알림을 예약하고 지금 알림을 닫는다. */
@AndroidEntryPoint
class RemindLaterReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleOnceReminder: ScheduleOnceReminderUseCase

    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, NO_ID)
        val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, NO_ID)
        if (reminderId == NO_ID || medicineId == NO_ID) return
        launchAsync {
            scheduleOnceReminder(medicineId, REMIND_LATER_AFTER)
            notifier.dismiss(reminderId)
        }
    }

    companion object {
        val REMIND_LATER_AFTER: Duration = Duration.ofHours(1)

        private const val EXTRA_REMINDER_ID = "reminderId"
        private const val EXTRA_MEDICINE_ID = "medicineId"
        private const val NO_ID = -1L

        fun intent(context: Context, reminderId: Long, medicineId: Long): Intent =
            Intent(context, RemindLaterReceiver::class.java)
                .putExtra(EXTRA_REMINDER_ID, reminderId)
                .putExtra(EXTRA_MEDICINE_ID, medicineId)
    }
}
