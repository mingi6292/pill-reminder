package com.mg.pillreminder.ui.medicine

import com.mg.pillreminder.domain.permission.PermissionStatus

enum class PermissionBanner { Notification, ExactAlarm }

/**
 * 등록한 약이 없으면 배너를 띄우지 않는다. 앱을 열자마자 권한을 조르지 않으려는 것이다.
 * 알림이 꺼져 있으면 정확한 알람도 의미가 없어 알림 배너를 먼저 띄운다.
 */
fun permissionBannerFor(status: PermissionStatus, hasMedicines: Boolean): PermissionBanner? = when {
    !hasMedicines -> null
    !status.notificationsAllowed -> PermissionBanner.Notification
    !status.exactAlarmsAllowed -> PermissionBanner.ExactAlarm
    else -> null
}

/** 첫 약을 등록할 때만 알림 권한을 묻는다. 그 뒤로는 배너로 안내한다. */
fun shouldAskNotificationOnAdd(status: PermissionStatus, hasMedicines: Boolean): Boolean =
    !hasMedicines && !status.notificationsAllowed
