package com.mg.pillreminder.ui.medicine

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.mg.pillreminder.domain.permission.PermissionStatus
import com.mg.pillreminder.domain.usecase.AddDailyMedicineUseCase
import com.mg.pillreminder.domain.usecase.DeleteMedicineUseCase
import com.mg.pillreminder.domain.usecase.ObserveMedicinesUseCase
import com.mg.pillreminder.fake.FakeAlarmScheduler
import com.mg.pillreminder.fake.FakeDoseRecordRepository
import com.mg.pillreminder.fake.FakeMedicineRepository
import com.mg.pillreminder.fake.FakePermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineViewModelTest {

    private val repository = FakeMedicineRepository()
    private val alarmScheduler = FakeAlarmScheduler()
    private val permissionChecker = FakePermissionChecker()
    private val clock = Clock.fixed(Instant.parse("2026-11-16T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var viewModel: MedicineViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = MedicineViewModel(
            ObserveMedicinesUseCase(repository, FakeDoseRecordRepository()),
            AddDailyMedicineUseCase(repository, repository.reminderRepository, alarmScheduler, clock),
            DeleteMedicineUseCase(repository, repository.reminderRepository, alarmScheduler),
            permissionChecker,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `처음에는 로딩 상태`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `등록한 약이 다음 알림 시각과 함께 목록에 나온다`() = runTest {
        viewModel.uiState.test {
            assertEquals(emptyList<MedicineItem>(), awaitLoaded().medicines)

            viewModel.addMedicine("비타민", LocalTime.of(8, 0))

            val item = awaitItem().medicines.single()
            assertEquals("비타민", item.name)
            assertNotNull(item.nextReminderAt)
        }
    }

    @Test
    fun `이름이 공백이면 저장하지 않는다`() = runTest {
        viewModel.addMedicine("  ", LocalTime.of(8, 0))

        assertTrue(repository.medicines.isEmpty())
    }

    @Test
    fun `삭제하면 목록에서 빠진다`() = runTest {
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.addMedicine("비타민", LocalTime.of(8, 0))
            val id = awaitItem().medicines.single().id

            viewModel.deleteMedicine(id)

            assertEquals(emptyList<MedicineItem>(), awaitItem().medicines)
        }
    }

    @Test
    fun `알림이 꺼진 채 첫 약을 등록하면 알림 배너가 뜬다`() = runTest {
        permissionChecker.status = PermissionStatus(notificationsAllowed = false, exactAlarmsAllowed = true)
        viewModel.refreshPermissions()

        viewModel.uiState.test {
            assertTrue(awaitLoaded().askNotificationOnAdd)

            viewModel.addMedicine("비타민", LocalTime.of(8, 0))

            val state = awaitItem()
            assertEquals(PermissionBanner.Notification, state.permissionBanner)
            assertFalse(state.askNotificationOnAdd)
        }
    }

    @Test
    fun `권한 대화상자가 떠 있는 동안은 배너를 숨기고 닫히면 띄운다`() = runTest {
        permissionChecker.status = PermissionStatus(notificationsAllowed = false, exactAlarmsAllowed = true)
        viewModel.refreshPermissions()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onNotificationRequestStarted()
            viewModel.addMedicine("비타민", LocalTime.of(8, 0))
            assertNull(expectMostRecentItem().permissionBanner)

            viewModel.onNotificationRequestFinished()

            assertEquals(PermissionBanner.Notification, awaitItem().permissionBanner)
        }
    }

    @Test
    fun `권한을 다시 읽으면 배너가 갱신된다`() = runTest {
        permissionChecker.status = PermissionStatus(notificationsAllowed = true, exactAlarmsAllowed = false)
        viewModel.refreshPermissions()
        viewModel.addMedicine("비타민", LocalTime.of(8, 0))

        viewModel.uiState.test {
            assertEquals(PermissionBanner.ExactAlarm, awaitLoaded().permissionBanner)

            permissionChecker.status = PermissionStatus(notificationsAllowed = true, exactAlarmsAllowed = true)
            viewModel.refreshPermissions()

            assertNull(awaitItem().permissionBanner)
        }
    }

    private suspend fun ReceiveTurbine<MedicineUiState>.awaitLoaded(): MedicineUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
