package com.mg.pillreminder.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** ReminderSchedule은 type + timeOfDayMinutes + intervalMinutes로 펼쳐 저장한다. 변환은 ReminderMapper. */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicineId")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val type: String,
    val timeOfDayMinutes: Int?,
    val intervalMinutes: Long?,
    val nextTriggerAt: Long,
)
