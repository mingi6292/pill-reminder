package com.mg.pillreminder.ui.voice

import app.cash.turbine.test
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.usecase.GetLastDoseUseCase
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import com.mg.pillreminder.domain.voice.ListenResult
import com.mg.pillreminder.domain.voice.VoiceConversation
import com.mg.pillreminder.domain.voice.VoiceIntent
import com.mg.pillreminder.fake.DefaultTimeZoneRule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import com.mg.pillreminder.fake.FakeSpeechListener
import com.mg.pillreminder.fake.FakeSpeechSpeaker
import com.mg.pillreminder.fake.FakeVoiceIntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    @get:Rule
    val timeZoneRule = DefaultTimeZoneRule(ZoneId.of("Asia/Seoul"))

    private val medicines = FakeMedicineRepository()
    private val doseRecords = FakeDoseRecordRepository()
    private val now = Instant.parse("2026-11-20T02:20:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val speaker = FakeSpeechSpeaker()

    private val parser = FakeVoiceIntentParser(
        mapOf(
            "생리통약 먹었어" to VoiceIntent.RecordDose("생리통약"),
            "4시간 뒤에" to VoiceIntent.ScheduleAfter(null, 240),
            "비타민D 먹었어" to VoiceIntent.RecordDose("비타민D"),
            "응" to VoiceIntent.Answer(yes = true),
        ),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `질문으로 끝난 응답은 이어서 듣고 약 이름 없는 말도 이해한다`() = runTest {
        addMedicine("생리통약")
        val listener = FakeSpeechListener(ListenResult.Heard("생리통약 먹었어"), ListenResult.Heard("4시간 뒤에"))
        val viewModel = viewModel(listener)

        viewModel.start()

        assertEquals(2, listener.listenCount)
        assertEquals(
            listOf("복약 기록을 체크했어요. 다음에 언제 알려드릴까요?", "4시간 뒤 오후 3시 20분에 알려드릴게요."),
            speaker.spoken,
        )
        assertEquals(VoicePhase.Idle, viewModel.uiState.value.phase)
        assertEquals("4시간 뒤에", viewModel.uiState.value.heard)
    }

    @Test
    fun `말이 없으면 안내를 보이고 멈춘다`() = runTest {
        val listener = FakeSpeechListener(ListenResult.NoSpeech)
        val viewModel = viewModel(listener)

        viewModel.start()

        assertEquals(VoiceProblem.NoSpeech, viewModel.uiState.value.problem)
        assertEquals(VoicePhase.Idle, viewModel.uiState.value.phase)
        assertEquals(1, listener.listenCount)
    }

    @Test
    fun `등록 안 된 약을 응으로 승인하면 이름을 담아 등록 요청을 낸다`() = runTest {
        val viewModel = viewModel(FakeSpeechListener(ListenResult.Heard("비타민D 먹었어"), ListenResult.Heard("응")))

        viewModel.registrationRequests.test {
            viewModel.start()
            assertEquals("비타민D", awaitItem())
        }
    }

    @Test
    fun `권한이 없으면 권한 안내를 보인다`() = runTest {
        val viewModel = viewModel(FakeSpeechListener())

        viewModel.onPermissionDenied()

        assertEquals(VoiceProblem.PermissionDenied, viewModel.uiState.value.problem)
    }

    @Test
    fun `멈추면 TTS를 놓고 상태를 비운다`() = runTest {
        val viewModel = viewModel(FakeSpeechListener(ListenResult.Heard("생리통약 먹었어")))
        viewModel.start()

        viewModel.stop()

        assertEquals(1, speaker.shutdownCount)
        assertNull(viewModel.uiState.value.heard)
    }

    private fun viewModel(listener: FakeSpeechListener): VoiceViewModel {
        val conversation = Provider {
            VoiceConversation(
                medicines,
                RecordDoseUseCase(medicines, doseRecords, clock),
                ScheduleOnceReminderUseCase(medicines, medicines.reminderRepository, FakeAlarmScheduler(), clock),
                GetLastDoseUseCase(medicines, doseRecords),
                clock,
            )
        }
        return VoiceViewModel(listener, speaker, parser, conversation)
    }

    private suspend fun addMedicine(name: String) {
        medicines.addWithReminder(
            Medicine(id = 0, name = name, createdAt = now),
            Reminder(0, 0, ReminderSchedule.Daily(LocalTime.of(8, 0)), now),
        )
    }
}
