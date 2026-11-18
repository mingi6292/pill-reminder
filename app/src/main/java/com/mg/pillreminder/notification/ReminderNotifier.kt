package com.mg.pillreminder.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mg.pillreminder.MainActivity
import com.mg.pillreminder.R
import com.mg.pillreminder.alarm.DoseTakenReceiver
import com.mg.pillreminder.domain.model.DueReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun show(reminder: DueReminder) {
        // 권한 요청은 화면에서 한다. 여기서는 권한이 없으면 조용히 넘어간다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        notificationManager.notify(notificationId(reminder.reminderId), buildNotification(reminder))
    }

    fun dismiss(reminderId: Long) = notificationManager.cancel(notificationId(reminderId))

    private fun notificationId(reminderId: Long) = reminderId.toInt()

    // 이미 있는 채널은 다시 만들어도 바뀌지 않으므로 매번 호출해도 된다.
    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.reminder_channel_name))
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(reminder: DueReminder) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(reminder.medicineName)
        .setContentText(context.getString(R.string.reminder_notification_text))
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(openAppIntent())
        .addAction(0, context.getString(R.string.reminder_action_taken), doseTakenIntent(reminder))
        .setAutoCancel(true)
        .build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // requestCode가 같으면 PendingIntent가 덮어써져 다른 알림의 extras로 기록된다. 알림 id로 나눈다.
    private fun doseTakenIntent(reminder: DueReminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId(reminder.reminderId),
        DoseTakenReceiver.intent(context, reminder.reminderId, reminder.medicineId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "medication_reminder"
    }
}
