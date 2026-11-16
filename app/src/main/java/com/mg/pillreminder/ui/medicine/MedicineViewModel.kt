package com.mg.pillreminder.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.usecase.AddDailyMedicineUseCase
import com.mg.pillreminder.domain.usecase.DeleteMedicineUseCase
import com.mg.pillreminder.domain.usecase.ObserveMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    observeMedicines: ObserveMedicinesUseCase,
    private val addDailyMedicine: AddDailyMedicineUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
) : ViewModel() {

    val uiState: StateFlow<MedicineUiState> = observeMedicines()
        .map { list -> MedicineUiState(isLoading = false, medicines = list.map { it.toItem() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MedicineUiState())

    fun addMedicine(name: String, time: LocalTime) {
        // 화면에서 저장 버튼을 막지만, 다른 입구에서 와도 UseCase 예외로 앱이 죽지 않게 한 번 더 거른다.
        if (name.isBlank()) return
        viewModelScope.launch { addDailyMedicine(name, time) }
    }

    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch { deleteMedicineUseCase(medicineId) }
    }

    private fun MedicineWithNextReminder.toItem() = MedicineItem(
        id = medicine.id,
        name = medicine.name,
        nextReminderAt = nextTriggerAt,
    )

    private companion object {
        // 화면 회전 같은 짧은 구성 변경 동안은 구독을 유지한다.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
