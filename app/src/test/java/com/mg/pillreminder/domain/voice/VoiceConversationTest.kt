package com.mg.pillreminder.domain.voice

import com.mg.pillreminder.domain.model.DoseRecord
import com.mg.pillreminder.domain.model.Medicine
import com.mg.pillreminder.domain.model.Reminder
import com.mg.pillreminder.domain.model.ReminderSchedule
import com.mg.pillreminder.domain.usecase.GetLastDoseUseCase
import com.mg.pillreminder.domain.usecase.RecordDoseUseCase
import com.mg.pillreminder.domain.usecase.ScheduleOnceReminderUseCase
import com.mg.pillreminder.fake.DefaultTimeZoneRule
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class VoiceConversationTest {

    @get:Rule
    val timeZoneRule = DefaultTimeZoneRule(ZoneId.of("Asia/Seoul"))

    private val medicines = FakeMedicineRepository()
    private val doseRecords = FakeDoseRecordRepository()
    private val alarmScheduler = FakeAlarmScheduler()

    // 서울 오전 11시 20분
    private var now = Instant.parse("2026-11-20T02:20:00Z")
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
        override fun instant() = now
    }

    private val conversation = VoiceConversation(
        medicines,
        RecordDoseUseCase(medicines, doseRecords, clock),
        ScheduleOnceReminderUseCase(medicines, medicines.reminderRepository, alarmScheduler, clock),
        GetLastDoseUseCase(medicines, doseRecords),
        clock,
    )

    @Test
    fun `예시1 - 먹었다고 하면 기록하고 다음 알림을 묻는다`() = runTest {
        val id = addMedicine("생리통약")

        val reply = conversation.reply(VoiceIntent.RecordDose("생리통약"))

        assertEquals("복약 기록을 체크했어요. 다음에 언제 알려드릴까요?", reply.speech)
        assertTrue(reply.keepListening)
        assertEquals(DoseRecord(1, id, now), doseRecords.records.single())
    }

    @Test
    fun `예시2 - 약 이름 없이 N시간 뒤라고 하면 직전 대화의 약으로 예약한다`() = runTest {
        val id = addMedicine("생리통약")
        conversation.reply(VoiceIntent.RecordDose("생리통약"))

        val reply = conversation.reply(VoiceIntent.ScheduleAfter(null, 240))

        assertEquals("4시간 뒤 오후 3시 20분에 알려드릴게요.", reply.speech)
        assertFalse(reply.keepListening)
        val once = medicines.reminders.single { it.schedule == ReminderSchedule.Once }
        assertEquals(id, once.medicineId)
        assertEquals(Instant.parse("2026-11-20T06:20:00Z"), alarmScheduler.scheduled[once.id])
    }

    @Test
    fun `예시3 - 언제 먹었는지 물으면 시각과 지난 시간을 답한다`() = runTest {
        addMedicine("생리통약")
        conversation.reply(VoiceIntent.RecordDose("생리통약"))
        now = Instant.parse("2026-11-20T06:20:00Z")

        val reply = conversation.reply(VoiceIntent.AskLastDose("생리통약"))

        assertEquals("오늘 오전 11시 20분에 드셨어요. 4시간 지났어요.", reply.speech)
    }

    @Test
    fun `예약 시각이 내일이면 날짜를 붙인다`() = runTest {
        addMedicine("생리통약")
        now = Instant.parse("2026-11-20T13:30:00Z") // 서울 22:30

        val reply = conversation.reply(VoiceIntent.ScheduleAfter("생리통약", 150))

        assertEquals("2시간 30분 뒤 내일 오전 1시에 알려드릴게요.", reply.speech)
    }

    @Test
    fun `이전 대화 없이 약 이름이 빠지면 어떤 약인지 묻는다`() = runTest {
        addMedicine("생리통약")

        val reply = conversation.reply(VoiceIntent.ScheduleAfter(null, 60))

        assertEquals("어떤 약인지 같이 말씀해 주세요.", reply.speech)
        assertTrue(medicines.reminders.none { it.schedule == ReminderSchedule.Once })
    }

    @Test
    fun `등록되지 않은 약이면 등록할지 묻고 응이라고 하면 이름을 채운 등록 화면을 연다`() = runTest {
        val ask = conversation.reply(VoiceIntent.RecordDose("비타민D"))
        assertEquals("비타민D, 등록되지 않은 약이에요. 새로 등록할까요?", ask.speech)
        assertTrue(ask.keepListening)

        val answer = conversation.reply(VoiceIntent.Answer(yes = true))

        assertEquals("비타민D", answer.openRegistrationFor)
        assertTrue(doseRecords.records.isEmpty())
    }

    @Test
    fun `등록 질문에 아니라고 하면 아무것도 하지 않는다`() = runTest {
        conversation.reply(VoiceIntent.AskLastDose("비타민D"))

        val answer = conversation.reply(VoiceIntent.Answer(yes = false))

        assertEquals("알겠어요.", answer.speech)
        assertNull(answer.openRegistrationFor)
    }

    @Test
    fun `다른 말을 하고 나면 이전 등록 질문은 잊는다`() = runTest {
        addMedicine("생리통약")
        conversation.reply(VoiceIntent.RecordDose("비타민D"))
        conversation.reply(VoiceIntent.RecordDose("생리통약"))

        val answer = conversation.reply(VoiceIntent.Answer(yes = true))

        assertNull(answer.openRegistrationFor)
    }

    @Test
    fun `복용 기록이 없으면 없다고 답한다`() = runTest {
        addMedicine("생리통약")

        val reply = conversation.reply(VoiceIntent.AskLastDose("생리통약"))

        assertEquals("생리통약, 아직 복용 기록이 없어요.", reply.speech)
    }

    @Test
    fun `해석하지 못한 말은 다시 말해 달라고 한다`() = runTest {
        val reply = conversation.reply(VoiceIntent.Unknown)

        assertEquals("잘 못 알아들었어요. 다시 말씀해 주세요.", reply.speech)
        assertTrue(reply.keepListening)
    }

    private suspend fun addMedicine(name: String): Long = medicines.addWithReminder(
        Medicine(id = 0, name = name, createdAt = now),
        Reminder(0, 0, ReminderSchedule.Daily(LocalTime.of(8, 0)), now),
    )
}
