package com.mg.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mg.pillreminder.domain.usecase.HandleReminderFiredUseCase
import com.mg.pillreminder.notification.ReminderNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var handleReminderFired: HandleReminderFiredUseCase

    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, NO_REMINDER_ID)
        if (reminderId == NO_REMINDER_ID) return
        launchAsync {
            handleReminderFired(reminderId)?.let(notifier::show)
        }
    }

    companion object {
        private const val EXTRA_REMINDER_ID = "reminderId"
        private const val NO_REMINDER_ID = -1L

        fun intent(context: Context, reminderId: Long): Intent =
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_REMINDER_ID, reminderId)
    }
}
