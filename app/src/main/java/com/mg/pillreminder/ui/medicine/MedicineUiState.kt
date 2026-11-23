package com.mg.pillreminder.ui.medicine

import java.time.Duration
import java.time.Instant

data class MedicineUiState(
    val isLoading: Boolean = true,
    val medicines: List<MedicineItem> = emptyList(),
    val permissionBanner: PermissionBanner? = null,
    val askNotificationOnAdd: Boolean = false,
)

/**
 * 알림이 없으면 nextReminderAt, 복용 기록이 없으면 lastTakenAt, 제안할 주기가 없으면 suggestedInterval이 null.
 * 표시 형식은 화면에서 사용 시점 타임존으로 정한다.
 */
data class MedicineItem(
    val id: Long,
    val name: String,
    val nextReminderAt: Instant?,
    val lastTakenAt: Instant? = null,
    val suggestedInterval: Duration? = null,
)
