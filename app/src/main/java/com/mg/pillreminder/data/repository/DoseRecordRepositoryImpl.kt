package com.mg.pillreminder.data.repository

import com.mg.pillreminder.data.local.DoseRecordDao
import com.mg.pillreminder.data.local.toDomain
import com.mg.pillreminder.data.local.toEntity
import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class DoseRecordRepositoryImpl @Inject constructor(
    private val doseRecordDao: DoseRecordDao,
) : DoseRecordRepository {

    override suspend fun add(record: DoseRecord): Long = doseRecordDao.insert(record.toEntity())

    override suspend fun findLatest(medicineIds: List<Long>): DoseRecord? =
        if (medicineIds.isEmpty()) null else doseRecordDao.findLatest(medicineIds)?.toDomain()

    override suspend fun findRecentTakenAt(medicineId: Long, limit: Int): List<Instant> =
        doseRecordDao.findRecentTakenAt(medicineId, limit).map(Instant::ofEpochMilli)

    override fun observeLastTakenAt(): Flow<Map<Long, Instant>> = doseRecordDao.observeLastTakenAt().map { rows ->
        rows.associate { it.medicineId to Instant.ofEpochMilli(it.takenAt) }
    }
}
