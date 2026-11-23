package com.mg.pillreminder.domain.usecase

import com.mg.pillreminder.domain.model.MedicineOverview
import com.mg.pillreminder.domain.repository.DoseRecordRepository
import com.mg.pillreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * 약 목록. 약·알림·복용 기록 중 하나라도 바뀌면 다시 흘려보낸다.
 * 주기 제안도 그때마다 다시 계산해 기록이 쌓이거나 수락·거절하면 목록에 바로 반영된다.
 */
class ObserveMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseRecordRepository: DoseRecordRepository,
    private val getIntervalSuggestion: GetIntervalSuggestionUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<MedicineOverview>> = combine(
        medicineRepository.observeWithNextReminder(),
        doseRecordRepository.observeLastTakenAt(),
    ) { medicines, lastTakenAt ->
        medicines.map { MedicineOverview(it.medicine, it.nextTriggerAt, lastTakenAt[it.medicine.id]) }
    }.mapLatest { overviews ->
        // 약 수가 적어 약마다 기록 4개를 읽어도 부담이 없다.
        overviews.map { it.copy(suggestedInterval = getIntervalSuggestion(it.medicine.id)) }
    }
}
