package com.mg.pillreminder.domain.model

import java.time.Instant

data class Medicine(
    val id: Long,
    val name: String,
    val createdAt: Instant,
)

/** 약 목록 화면용. 알림이 없으면 nextTriggerAt은 null. */
data class MedicineWithNextReminder(
    val medicine: Medicine,
    val nextTriggerAt: Instant?,
)
