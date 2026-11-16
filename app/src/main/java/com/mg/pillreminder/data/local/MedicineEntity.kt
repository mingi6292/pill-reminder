package com.mg.pillreminder.data.local

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
)

data class MedicineWithNextTriggerRow(
    @Embedded val medicine: MedicineEntity,
    val nextTriggerAt: Long?,
)

fun MedicineEntity.toDomain() = Medicine(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Medicine.toEntity() = MedicineEntity(
    name = name,
    createdAt = createdAt.toEpochMilli(),
)

fun MedicineWithNextTriggerRow.toDomain() = MedicineWithNextReminder(
    medicine = medicine.toDomain(),
    nextTriggerAt = nextTriggerAt?.let(Instant::ofEpochMilli),
)
