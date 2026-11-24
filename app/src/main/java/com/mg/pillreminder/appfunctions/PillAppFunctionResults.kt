package com.mg.pillreminder.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import java.time.Instant

/**
 * 복용을 기록한 결과.
 *
 * @property medicineName 앱에 등록된 약 이름
 * @property takenAt 기록된 복용 시각
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RecordedDose(
    val medicineName: String,
    val takenAt: Instant,
)

/**
 * 마지막 복용 조회 결과.
 *
 * @property medicineName 앱에 등록된 약 이름
 * @property takenAt 마지막으로 복용한 시각. 복용 기록이 한 번도 없으면 null
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class LastDose(
    val medicineName: String,
    val takenAt: Instant?,
)

/**
 * 1회 알림 예약 결과.
 *
 * @property medicineName 앱에 등록된 약 이름
 * @property triggerAt 알림이 울릴 시각
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ScheduledReminder(
    val medicineName: String,
    val triggerAt: Instant,
)
