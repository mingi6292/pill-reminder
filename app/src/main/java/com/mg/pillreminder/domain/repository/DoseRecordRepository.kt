package com.mg.pillreminder.domain.repository

import com.mg.pillreminder.domain.model.DoseRecord
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DoseRecordRepository {
    /** record.id는 무시하고 새 id를 돌려준다. */
    suspend fun add(record: DoseRecord): Long

    /** 주어진 약들의 기록 중 가장 최근 1건. 없으면 null. */
    suspend fun findLatest(medicineIds: List<Long>): DoseRecord?

    /** 한 약의 최근 복용 시각 최대 [limit]개. 최근 것이 먼저 온다. */
    suspend fun findRecentTakenAt(medicineId: Long, limit: Int): List<Instant>

    /** 약 id → 마지막 복용 시각. 기록이 없는 약은 빠진다. */
    fun observeLastTakenAt(): Flow<Map<Long, Instant>>
}
