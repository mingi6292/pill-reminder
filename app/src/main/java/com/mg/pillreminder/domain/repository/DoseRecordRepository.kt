package com.mg.pillreminder.domain.repository

import com.mg.pillreminder.domain.model.DoseRecord

interface DoseRecordRepository {
    /** record.id는 무시하고 새 id를 돌려준다. */
    suspend fun add(record: DoseRecord): Long
}
