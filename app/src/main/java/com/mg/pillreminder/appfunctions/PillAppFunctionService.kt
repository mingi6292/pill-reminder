package com.mg.pillreminder.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 외부 AI 에이전트(Gemini 등)에 복약 기능을 노출하는 입구. 로직은 [PillAppFunctionHandler]와 UseCase에 있다.
 * 구체 서비스 클래스 `PillAppFunctionServiceImpl`과 XML은 KSP가 만든다.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "PillAppFunctionServiceImpl",
    appFunctionXmlFileName = "pill_app_function_service",
)
abstract class PillAppFunctionService : AppFunctionService() {

    @Inject
    internal lateinit var handler: PillAppFunctionHandler

    /**
     * Records that the user has just taken a medicine, using the current time.
     * Use it when the user says they took a medicine. The medicine must already be registered in the app.
     *
     * @param medicineName Name of the registered medicine, e.g. "비타민". Spaces and letter case are ignored.
     * @return The medicine name and the time that was recorded.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun recordDose(medicineName: String): RecordedDose = guarded { handler.recordDose(medicineName) }

    /**
     * Looks up when the user last took a medicine.
     *
     * @param medicineName Name of the registered medicine, e.g. "비타민". Spaces and letter case are ignored.
     * @return The medicine name and the last time it was taken. The time is null when no dose has been recorded yet.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getLastDose(medicineName: String): LastDose = guarded { handler.getLastDose(medicineName) }

    /**
     * Schedules a one-time reminder for a medicine, starting from now.
     * A pending one-time reminder for the same medicine is replaced. Daily reminders are not affected.
     *
     * @param medicineName Name of the registered medicine, e.g. "비타민". Spaces and letter case are ignored.
     * @param minutes Minutes from now until the reminder rings. Must be greater than 0. For "in 2 hours" pass 120.
     * @return The medicine name and the time the reminder will ring.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun scheduleReminderAfter(medicineName: String, minutes: Int): ScheduledReminder =
        guarded { handler.scheduleReminderAfter(medicineName, minutes) }

    private suspend inline fun <T> guarded(block: () -> T): T = try {
        block()
    } catch (error: PillAppFunctionError) {
        throw error.toAppFunctionException()
    }
}

internal fun PillAppFunctionError.toAppFunctionException(): AppFunctionException = when (this) {
    is PillAppFunctionError.MedicineNotRegistered -> AppFunctionElementNotFoundException(message.orEmpty())
    is PillAppFunctionError.InvalidArgument -> AppFunctionInvalidArgumentException(message.orEmpty())
}
