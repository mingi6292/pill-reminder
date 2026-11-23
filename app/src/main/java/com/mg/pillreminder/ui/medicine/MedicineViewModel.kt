package com.mg.pillreminder.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.pillreminder.domain.model.MedicineOverview
import com.mg.pillreminder.domain.permission.PermissionChecker
import com.mg.pillreminder.domain.permission.PermissionStatus
import com.mg.pillreminder.domain.usecase.AcceptIntervalSuggestionUseCase
import com.mg.pillreminder.domain.usecase.AddDailyMedicineUseCase
import com.mg.pillreminder.domain.usecase.DeleteMedicineUseCase
import com.mg.pillreminder.domain.usecase.ObserveMedicinesUseCase
import com.mg.pillreminder.domain.usecase.RejectIntervalSuggestionUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    observeMedicines: ObserveMedicinesUseCase,
    private val addDailyMedicine: AddDailyMedicineUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
    private val scheduleOnceReminder: ScheduleOnceReminderUseCase,
    private val acceptIntervalSuggestionUseCase: AcceptIntervalSuggestionUseCase,
    private val rejectIntervalSuggestionUseCase: RejectIntervalSuggestionUseCase,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val permissionStatus = MutableStateFlow(permissionChecker.status())

    // 화면 회전으로 Activity가 다시 만들어져도 대화상자는 그대로라 ViewModel이 기억한다.
    private val askingNotification = MutableStateFlow(false)

    val uiState: StateFlow<MedicineUiState> = combine(
        observeMedicines(),
        permissionStatus,
        askingNotification,
    ) { list, status, asking ->
        toUiState(list, status, asking)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MedicineUiState())

    // 안내는 한 번만 보여야 해서 상태가 아니라 이벤트로 낸다. 화면이 잠깐 구독을 끊어도 Channel이 들고 있는다.
    private val reminderScheduledEvents = Channel<Instant>(Channel.BUFFERED)

    /** "나중에 알림"이나 주기 알림의 첫 알림 시각. 화면이 안내 문구로 보여 준다. */
    val reminderScheduled: Flow<Instant> = reminderScheduledEvents.receiveAsFlow()

    /** 설정 화면이나 권한 요청에서 돌아왔을 때 부른다. */
    fun refreshPermissions() {
        permissionStatus.value = permissionChecker.status()
    }

    fun onNotificationRequestStarted() {
        askingNotification.value = true
    }

    fun onNotificationRequestFinished() {
        askingNotification.value = false
        refreshPermissions()
    }

    fun addMedicine(name: String, time: LocalTime) {
        // 화면에서 저장 버튼을 막지만, 다른 입구에서 와도 UseCase 예외로 앱이 죽지 않게 한 번 더 거른다.
        if (name.isBlank()) return
        viewModelScope.launch { addDailyMedicine(name, time) }
    }

    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch { deleteMedicineUseCase(medicineId) }
    }

    fun remindLater(medicineId: Long, after: Duration) {
        viewModelScope.launch {
            scheduleOnceReminder(medicineId, after)?.let { reminderScheduledEvents.send(it) }
        }
    }

    fun acceptIntervalSuggestion(medicineId: Long, interval: Duration) {
        viewModelScope.launch {
            acceptIntervalSuggestionUseCase(medicineId, interval)?.let { reminderScheduledEvents.send(it) }
        }
    }

    fun rejectIntervalSuggestion(medicineId: Long) {
        viewModelScope.launch { rejectIntervalSuggestionUseCase(medicineId) }
    }

    private fun toUiState(
        list: List<MedicineOverview>,
        status: PermissionStatus,
        askingNotification: Boolean,
    ): MedicineUiState {
        val hasMedicines = list.isNotEmpty()
        return MedicineUiState(
            isLoading = false,
            medicines = list.map { it.toItem() },
            permissionBanner = permissionBannerFor(status, hasMedicines, askingNotification),
            askNotificationOnAdd = shouldAskNotificationOnAdd(status, hasMedicines),
        )
    }

    private fun MedicineOverview.toItem() = MedicineItem(
        id = medicine.id,
        name = medicine.name,
        nextReminderAt = nextTriggerAt,
        lastTakenAt = lastTakenAt,
        suggestedInterval = suggestedInterval,
    )

    private companion object {
        // 화면 회전 같은 짧은 구성 변경 동안은 구독을 유지한다.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
