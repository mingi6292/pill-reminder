package com.mg.pillreminder.appfunctions

import com.mg.pillreminder.domain.MedicineNameMatcher
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.usecase.GetLastDoseUseCase
import com.mg.pillreminder.domain.usecase.LastDoseResult
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import java.time.Duration
import javax.inject.Inject

/**
 * AppFunction 입구와 UseCase 사이의 변환만 맡는다. 테스트할 수 있는 부분(약 찾기, 결과 매핑, 오류 구분)을 서비스에서 뺐다.
 * AppFunctionException은 JVM 단위 테스트에서 만들 수 없어(Bundle.EMPTY가 null) 여기서는 [PillAppFunctionError]를 던지고,
 * 서비스가 [toAppFunctionException]으로 바꾼다.
 */
class PillAppFunctionHandler @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val recordDoseUseCase: RecordDoseUseCase,
    private val getLastDoseUseCase: GetLastDoseUseCase,
    private val scheduleOnceReminder: ScheduleOnceReminderUseCase,
) {
    suspend fun recordDose(medicineName: String): RecordedDose {
        val medicine = findMedicine(medicineName)
        val record = recordDoseUseCase(medicine.id) ?: throw notRegistered(medicineName)
        return RecordedDose(medicine.name, record.takenAt)
    }

    suspend fun getLastDose(medicineName: String): LastDose = when (val result = getLastDoseUseCase(medicineName)) {
        LastDoseResult.MedicineNotFound -> throw notRegistered(medicineName)
        LastDoseResult.NoRecord -> LastDose(findMedicine(medicineName).name, takenAt = null)
        is LastDoseResult.Taken -> LastDose(result.medicineName, result.takenAt)
    }

    suspend fun scheduleReminderAfter(medicineName: String, minutes: Int): ScheduledReminder {
        if (minutes <= 0) throw PillAppFunctionError.InvalidArgument("minutes must be greater than 0: $minutes")
        val medicine = findMedicine(medicineName)
        val triggerAt = scheduleOnceReminder(medicine.id, Duration.ofMinutes(minutes.toLong()))
            ?: throw notRegistered(medicineName)
        return ScheduledReminder(medicine.name, triggerAt)
    }

    private suspend fun findMedicine(name: String): Medicine =
        MedicineNameMatcher.findFirst(name, medicineRepository.getAll()) ?: throw notRegistered(name)

    private fun notRegistered(name: String) = PillAppFunctionError.MedicineNotRegistered(
        "'$name' is not a registered medicine. Ask the user to register it in the app first.",
    )
}

sealed class PillAppFunctionError(message: String) : Exception(message) {
    class MedicineNotRegistered(message: String) : PillAppFunctionError(message)
    class InvalidArgument(message: String) : PillAppFunctionError(message)
}
