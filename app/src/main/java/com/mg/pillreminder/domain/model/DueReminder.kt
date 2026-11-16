package com.mg.pillreminder.domain.model

/** 지금 울려야 하는 알림. 알림 표시에 필요한 값만 담는다. */
data class DueReminder(
    val reminderId: Long,
    val medicineName: String,
)
