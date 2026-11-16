package com.mg.pillreminder.ui.medicine

import com.mg.pillreminder.domain.permission.PermissionStatus

enum class PermissionBanner { Notification, ExactAlarm }

/**
 * 등록한 약이 없으면 배너를 띄우지 않는다. 앱을 열자마자 권한을 조르지 않으려는 것이다.
 * 권한 대화상자가 떠 있는 동안에도 같은 내용을 겹쳐 보이지 않게 숨긴다.
 * 알림이 꺼져 있으면 정확한 알람도 의미가 없어 알림 배너를 먼저 띄운다.
 */
fun permissionBannerFor(
    status: PermissionStatus,
    hasMedicines: Boolean,
    askingNotification: Boolean,
): PermissionBanner? = when {
    !hasMedicines || askingNotification -> null
    !status.notificationsAllowed -> PermissionBanner.Notification
    !status.exactAlarmsAllowed -> PermissionBanner.ExactAlarm
    else -> null
}

/** 첫 약을 등록할 때만 알림 권한을 묻는다. 그 뒤로는 배너로 안내한다. */
fun shouldAskNotificationOnAdd(status: PermissionStatus, hasMedicines: Boolean): Boolean =
    !hasMedicines && !status.notificationsAllowed

/**
 * 거부 결과만으로는 사용자가 대화상자에서 방금 거부했는지, 시스템이 대화상자 없이 거부했는지 알 수 없다.
 * 요청 전후 모두 다시 물을 수 없는 상태였을 때만 대화상자가 안 뜬 것으로 보고 설정 화면으로 보낸다.
 */
fun shouldOpenNotificationSettings(granted: Boolean, couldAskBefore: Boolean, canAskAfter: Boolean): Boolean =
    !granted && !couldAskBefore && !canAskAfter
