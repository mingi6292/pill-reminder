package com.mg.pillreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseRecordDao {
    @Insert
    suspend fun insert(record: DoseRecordEntity): Long

    /** 같은 시각에 두 번 기록됐으면 나중에 넣은 것을 고른다. */
    @Query(
        """
        SELECT * FROM dose_records
        WHERE medicineId IN (:medicineIds)
        ORDER BY takenAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun findLatest(medicineIds: List<Long>): DoseRecordEntity?

    @Query(
        """
        SELECT takenAt FROM dose_records
        WHERE medicineId = :medicineId
        ORDER BY takenAt DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun findRecentTakenAt(medicineId: Long, limit: Int): List<Long>

    @Query("SELECT medicineId, MAX(takenAt) AS takenAt FROM dose_records GROUP BY medicineId")
    fun observeLastTakenAt(): Flow<List<LastTakenRow>>
}

data class LastTakenRow(
    val medicineId: Long,
    val takenAt: Long,
)
