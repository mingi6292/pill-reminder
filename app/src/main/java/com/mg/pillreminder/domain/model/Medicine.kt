package com.mg.pillreminder.domain.model

import java.time.Duration
import java.time.Instant

data class Medicine(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    /** 주기 알림 제안을 거절했는지. 거절은 영구다(design.md 8장). */
    val intervalSuggestionDismissed: Boolean = false,
)

/** 약 목록 화면용. 알림이 없으면 nextTriggerAt은 null. */
data class MedicineWithNextReminder(
    val medicine: Medicine,
    val nextTriggerAt: Instant?,
)

/**
 * 약 목록 화면용. 알림이 없으면 nextTriggerAt, 복용 기록이 없으면 lastTakenAt,
 * 제안할 주기가 없으면 suggestedInterval이 null.
 */
data class MedicineOverview(
    val medicine: Medicine,
    val nextTriggerAt: Instant?,
    val lastTakenAt: Instant?,
    val suggestedInterval: Duration? = null,
)
