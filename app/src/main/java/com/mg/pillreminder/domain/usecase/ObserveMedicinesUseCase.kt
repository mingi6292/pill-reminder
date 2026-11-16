package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMedicinesUseCase @Inject constructor(
    private val repository: MedicineRepository,
) {
    operator fun invoke(): Flow<List<MedicineWithNextReminder>> = repository.observeWithNextReminder()
}
