package com.mg.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.notification.ReminderNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 알림의 "먹었어" 버튼. 기록하고 알림을 닫는다. */
@AndroidEntryPoint
class DoseTakenReceiver : BroadcastReceiver() {

    @Inject lateinit var recordDose: RecordDoseUseCase

    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, NO_ID)
        val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, NO_ID)
        if (reminderId == NO_ID || medicineId == NO_ID) return
        launchAsync {
            recordDose(medicineId)
            notifier.dismiss(reminderId)
        }
    }

    companion object {
        private const val EXTRA_REMINDER_ID = "reminderId"
        private const val EXTRA_MEDICINE_ID = "medicineId"
        private const val NO_ID = -1L

        fun intent(context: Context, reminderId: Long, medicineId: Long): Intent =
            Intent(context, DoseTakenReceiver::class.java)
                .putExtra(EXTRA_REMINDER_ID, reminderId)
                .putExtra(EXTRA_MEDICINE_ID, medicineId)
    }
}
