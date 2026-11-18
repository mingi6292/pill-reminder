package com.mg.pillreminder.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mg.pillreminder.domain.model.DoseRecord
import java.time.Instant

/** 약을 지우면 기록도 같이 지운다. 기록만 남아도 다시 볼 방법이 없기 때문이다. */
@Entity(
    tableName = "dose_records",
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
data class DoseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val takenAt: Long,
)

fun DoseRecord.toEntity() = DoseRecordEntity(
    medicineId = medicineId,
    takenAt = takenAt.toEpochMilli(),
)

fun DoseRecordEntity.toDomain() = DoseRecord(
    id = id,
    medicineId = medicineId,
    takenAt = Instant.ofEpochMilli(takenAt),
)
