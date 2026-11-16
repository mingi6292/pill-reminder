package com.mg.pillreminder.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.pillreminder.domain.model.MedicineWithNextReminder
import com.mg.pillreminder.domain.permission.PermissionChecker
import com.mg.pillreminder.domain.permission.PermissionStatus
import com.mg.pillreminder.domain.usecase.AddDailyMedicineUseCase
import com.mg.pillreminder.domain.usecase.DeleteMedicineUseCase
import com.mg.pillreminder.domain.usecase.ObserveMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    observeMedicines: ObserveMedicinesUseCase,
    private val addDailyMedicine: AddDailyMedicineUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val permissionStatus = MutableStateFlow(permissionChecker.status())

    val uiState: StateFlow<MedicineUiState> = combine(observeMedicines(), permissionStatus) { list, status ->
        toUiState(list, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MedicineUiState())

    /** 설정 화면이나 권한 요청에서 돌아왔을 때 부른다. */
    fun refreshPermissions() {
        permissionStatus.value = permissionChecker.status()
    }

    fun addMedicine(name: String, time: LocalTime) {
        // 화면에서 저장 버튼을 막지만, 다른 입구에서 와도 UseCase 예외로 앱이 죽지 않게 한 번 더 거른다.
        if (name.isBlank()) return
        viewModelScope.launch { addDailyMedicine(name, time) }
    }

    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch { deleteMedicineUseCase(medicineId) }
    }

    private fun toUiState(list: List<MedicineWithNextReminder>, status: PermissionStatus): MedicineUiState {
        val hasMedicines = list.isNotEmpty()
        return MedicineUiState(
            isLoading = false,
            medicines = list.map { it.toItem() },
            permissionBanner = permissionBannerFor(status, hasMedicines),
            askNotificationOnAdd = shouldAskNotificationOnAdd(status, hasMedicines),
        )
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
