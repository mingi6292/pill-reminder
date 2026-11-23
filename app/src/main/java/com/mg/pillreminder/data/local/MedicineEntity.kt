package com.mg.pillreminder.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import java.time.Instant

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    // v3 마이그레이션이 기존 행을 0으로 채우므로 엔티티 스키마에도 같은 기본값을 둔다.
    @ColumnInfo(defaultValue = "0") val intervalSuggestionDismissed: Boolean = false,
)

data class MedicineWithNextTriggerRow(
    @Embedded val medicine: MedicineEntity,
    val nextTriggerAt: Long?,
)

fun MedicineEntity.toDomain() = Medicine(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAt),
    intervalSuggestionDismissed = intervalSuggestionDismissed,
)

fun Medicine.toEntity() = MedicineEntity(
    name = name,
    createdAt = createdAt.toEpochMilli(),
    intervalSuggestionDismissed = intervalSuggestionDismissed,
)

fun MedicineWithNextTriggerRow.toDomain() = MedicineWithNextReminder(
    medicine = medicine.toDomain(),
    nextTriggerAt = nextTriggerAt?.let(Instant::ofEpochMilli),
)
