package com.mg.pillreminder.domain.permission

/** 권한 상태는 설정 화면에서 바뀔 수 있어 캐시하지 않고 부를 때마다 읽는다. */
interface PermissionChecker {
    fun status(): PermissionStatus
}

/** 해당 OS 버전에 권한이 없으면 허용된 것으로 본다. */
data class PermissionStatus(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
)
