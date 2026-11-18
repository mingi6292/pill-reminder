package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeDoseRecordRepository : DoseRecordRepository {

    private val state = MutableStateFlow(emptyList<DoseRecord>())
    private var nextId = 1L

    val records: List<DoseRecord> get() = state.value

    override suspend fun add(record: DoseRecord): Long {
        val id = nextId++
        state.update { it + record.copy(id = id) }
        return id
    }

    override suspend fun findLatest(medicineIds: List<Long>): DoseRecord? = records
        .filter { it.medicineId in medicineIds }
        .maxWithOrNull(compareBy<DoseRecord> { it.takenAt }.thenBy { it.id })

    override fun observeLastTakenAt(): Flow<Map<Long, Instant>> = state.map { list ->
        list.groupBy { it.medicineId }.mapValues { (_, records) -> records.maxOf { it.takenAt } }
    }
}
