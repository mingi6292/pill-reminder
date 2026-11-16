package com.mg.pillreminder.data.repository

import com.mg.pillreminder.data.local.MedicineDao
import com.mg.pillreminder.data.local.toDomain
import com.mg.pillreminder.data.local.toEntity
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicineRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
) : MedicineRepository {

    override fun observeWithNextReminder(): Flow<List<MedicineWithNextReminder>> =
        medicineDao.observeWithNextTrigger().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(medicine: Medicine): Long = medicineDao.insert(medicine.toEntity())

    override suspend fun delete(medicineId: Long) = medicineDao.deleteById(medicineId)
}
