package com.mg.pillreminder.data.repository

import com.mg.pillreminder.data.local.DoseRecordDao
import com.mg.pillreminder.data.local.toEntity
import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import javax.inject.Inject

class DoseRecordRepositoryImpl @Inject constructor(
    private val doseRecordDao: DoseRecordDao,
) : DoseRecordRepository {

    override suspend fun add(record: DoseRecord): Long = doseRecordDao.insert(record.toEntity())
}
