package com.mg.pillreminder.permission

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.mg.pillreminder.domain.permission.PermissionChecker
import com.mg.pillreminder.domain.permission.PermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionChecker {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun status() = PermissionStatus(
        // 12 이하에서도 사용자가 앱 알림을 끌 수 있어 런타임 권한 대신 알림 허용 여부를 본다.
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms(),
    )
}
