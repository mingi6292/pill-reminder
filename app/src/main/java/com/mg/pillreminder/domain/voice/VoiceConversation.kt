package com.mg.pillreminder.domain.voice

import com.mg.pillreminder.domain.MedicineNameMatcher
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.usecase.AcceptIntervalSuggestionUseCase
import com.mg.pillreminder.domain.usecase.GetIntervalSuggestionUseCase
import com.mg.pillreminder.domain.usecase.GetLastDoseUseCase
import com.mg.pillreminder.domain.usecase.LastDoseResult
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.domain.usecase.RejectIntervalSuggestionUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * 한 번의 음성 대화. [VoiceIntent]를 UseCase 호출과 응답 문구로 바꾼다.
 * "4시간 뒤에"처럼 약 이름이 빠진 말을 이해하려고 직전에 다룬 약과 답을 기다리는 질문을 기억한다.
 * 대화마다 새로 만들어 쓰므로 싱글턴으로 두지 않는다.
 */
class VoiceConversation @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val recordDose: RecordDoseUseCase,
    private val scheduleOnceReminder: ScheduleOnceReminderUseCase,
    private val getLastDose: GetLastDoseUseCase,
    private val getIntervalSuggestion: GetIntervalSuggestionUseCase,
    private val acceptIntervalSuggestion: AcceptIntervalSuggestionUseCase,
    private val rejectIntervalSuggestion: RejectIntervalSuggestionUseCase,
    private val clock: Clock,
) {
    private var currentMedicineName: String? = null
    private var pendingQuestion: PendingQuestion? = null

    suspend fun reply(intent: VoiceIntent): VoiceReply {
        val question = pendingQuestion
        pendingQuestion = null
        return when (intent) {
            is VoiceIntent.RecordDose -> onRecordDose(intent.medicineName)
            is VoiceIntent.ScheduleAfter -> onScheduleAfter(intent.medicineName, intent.minutes)
            is VoiceIntent.AskLastDose -> onAskLastDose(intent.medicineName)
            is VoiceIntent.Answer -> onAnswer(intent.yes, question)
            VoiceIntent.Unknown -> VoiceReply(Speech.NOT_UNDERSTOOD, keepListening = true)
        }
    }

    private suspend fun onRecordDose(name: String): VoiceReply {
        val medicine = findMedicine(name) ?: return askToRegister(name)
        recordDose(medicine.id) ?: return askToRegister(name)
        currentMedicineName = medicine.name
        // 기록이 반복돼 주기가 보이면 "다음에 언제?"보다 주기 알림을 먼저 제안한다.
        val interval = getIntervalSuggestion(medicine.id) ?: return VoiceReply(Speech.DOSE_RECORDED, keepListening = true)
        pendingQuestion = PendingQuestion.AcceptInterval(medicine.id, interval)
        return VoiceReply(Speech.intervalSuggestion(interval), keepListening = true)
    }

    private suspend fun onScheduleAfter(name: String?, minutes: Int): VoiceReply {
        if (minutes <= 0) return VoiceReply(Speech.NOT_UNDERSTOOD, keepListening = true)
        val targetName = name ?: currentMedicineName
            ?: return VoiceReply(Speech.WHICH_MEDICINE, keepListening = true)
        val medicine = findMedicine(targetName) ?: return askToRegister(targetName)

        val after = Duration.ofMinutes(minutes.toLong())
        val triggerAt = scheduleOnceReminder(medicine.id, after) ?: return askToRegister(targetName)
        currentMedicineName = medicine.name
        return VoiceReply(Speech.scheduled(after, triggerAt, now(), zone()), keepListening = false)
    }

    private suspend fun onAskLastDose(name: String): VoiceReply = when (val result = getLastDose(name)) {
        LastDoseResult.MedicineNotFound -> askToRegister(name)
        LastDoseResult.NoRecord -> {
            currentMedicineName = name
            VoiceReply(Speech.noRecord(name), keepListening = false)
        }
        is LastDoseResult.Taken -> {
            currentMedicineName = result.medicineName
            VoiceReply(Speech.lastTaken(result.takenAt, now(), zone()), keepListening = false)
        }
    }

    private suspend fun onAnswer(yes: Boolean, question: PendingQuestion?): VoiceReply = when {
        question is PendingQuestion.RegisterMedicine && yes ->
            VoiceReply(Speech.OPEN_REGISTRATION, keepListening = false, openRegistrationFor = question.name)
        question is PendingQuestion.AcceptInterval -> onIntervalAnswer(question, yes)
        else -> VoiceReply(Speech.OK, keepListening = false)
    }

    private suspend fun onIntervalAnswer(question: PendingQuestion.AcceptInterval, accept: Boolean): VoiceReply {
        if (!accept) {
            rejectIntervalSuggestion(question.medicineId)
            return VoiceReply(Speech.INTERVAL_REJECTED, keepListening = false)
        }
        val firstAt = acceptIntervalSuggestion(question.medicineId, question.interval)
            ?: return VoiceReply(Speech.INTERVAL_NOT_CREATED, keepListening = false)
        return VoiceReply(Speech.intervalAccepted(question.interval, firstAt, now(), zone()), keepListening = false)
    }

    private fun askToRegister(name: String): VoiceReply {
        pendingQuestion = PendingQuestion.RegisterMedicine(name)
        return VoiceReply(Speech.askToRegister(name), keepListening = true)
    }

    // 같은 이름을 시각별로 여러 번 등록할 수 있다. 기록과 조회는 이름 단위로 묶이니 어느 것을 골라도 되지만,
    // 1회 알림이 약마다 하나라 매번 같은 약을 골라야 "다시 예약"이 이전 예약을 바꾼다.
    private suspend fun findMedicine(name: String): Medicine? =
        MedicineNameMatcher.matches(name, medicineRepository.getAll()).minByOrNull { it.id }

    private fun now(): Instant = Instant.now(clock)

    // Clock은 UTC다. 사용자가 바꿀 수 있는 타임존은 말하는 시점의 값을 쓴다.
    private fun zone(): ZoneId = ZoneId.systemDefault()

    private sealed interface PendingQuestion {
        data class RegisterMedicine(val name: String) : PendingQuestion
        data class AcceptInterval(val medicineId: Long, val interval: Duration) : PendingQuestion
    }
}

internal object Speech {
    const val DOSE_RECORDED = "복약 기록을 체크했어요. 다음에 언제 알려드릴까요?"
    const val WHICH_MEDICINE = "어떤 약인지 같이 말씀해 주세요."
    const val NOT_UNDERSTOOD = "잘 못 알아들었어요. 다시 말씀해 주세요."
    const val OPEN_REGISTRATION = "등록 화면을 열게요. 복용 시각을 골라 주세요."
    const val OK = "알겠어요."
    const val INTERVAL_REJECTED = "알겠어요. 이 약은 다시 제안하지 않을게요."
    const val INTERVAL_NOT_CREATED = "주기 알림을 만들지 못했어요."

    fun intervalSuggestion(interval: Duration): String {
        val every = SpokenTimeFormat.duration(interval)
        return "복약 기록을 체크했어요. 최근 ${every}마다 드셨네요. ${every}마다 알려드릴까요?"
    }

    fun intervalAccepted(interval: Duration, firstAt: Instant, now: Instant, zone: ZoneId): String {
        val day = SpokenTimeFormat.day(firstAt, now, zone).takeUnless { it == "오늘" }?.let { "$it " }.orEmpty()
        return "${SpokenTimeFormat.duration(interval)}마다 알려드릴게요. 첫 알림은 $day${SpokenTimeFormat.clockTime(firstAt, zone)}이에요."
    }

    // 약 이름 뒤 조사(은/는)는 받침에 따라 달라지고 "비타민D"처럼 영문으로 끝나기도 해서 쉼표로 끊는다.
    fun askToRegister(name: String) = "$name, 등록되지 않은 약이에요. 새로 등록할까요?"

    fun noRecord(name: String) = "$name, 아직 복용 기록이 없어요."

    fun scheduled(after: Duration, triggerAt: Instant, now: Instant, zone: ZoneId): String {
        val day = SpokenTimeFormat.day(triggerAt, now, zone).takeUnless { it == "오늘" }?.let { "$it " }.orEmpty()
        return "${SpokenTimeFormat.duration(after)} 뒤 $day${SpokenTimeFormat.clockTime(triggerAt, zone)}에 알려드릴게요."
    }

    fun lastTaken(takenAt: Instant, now: Instant, zone: ZoneId): String {
        val day = SpokenTimeFormat.day(takenAt, now, zone)
        val elapsed = Duration.between(takenAt, now)
        val elapsedText = if (elapsed.toMinutes() < 1) "방금 전이에요." else "${SpokenTimeFormat.duration(elapsed)} 지났어요."
        return "$day ${SpokenTimeFormat.clockTime(takenAt, zone)}에 드셨어요. $elapsedText"
    }
}
