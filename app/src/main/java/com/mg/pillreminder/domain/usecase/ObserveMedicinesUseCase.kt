package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.MedicineOverview
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseRecordRepository: DoseRecordRepository,
) {
    operator fun invoke(): Flow<List<MedicineOverview>> = combine(
        medicineRepository.observeWithNextReminder(),
        doseRecordRepository.observeLastTakenAt(),
    ) { medicines, lastTakenAt ->
        medicines.map { MedicineOverview(it.medicine, it.nextTriggerAt, lastTakenAt[it.medicine.id]) }
    }
}
