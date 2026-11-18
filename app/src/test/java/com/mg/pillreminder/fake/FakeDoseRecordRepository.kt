package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.repository.DoseRecordRepository

class FakeDoseRecordRepository : DoseRecordRepository {

    val records = mutableListOf<DoseRecord>()
    private var nextId = 1L

    override suspend fun add(record: DoseRecord): Long {
        val id = nextId++
        records += record.copy(id = id)
        return id
    }
}
