package com.mg.pillreminder.ui.medicine

import com.mg.pillreminder.domain.permission.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionBannerTest {

    private val allAllowed = PermissionStatus(notificationsAllowed = true, exactAlarmsAllowed = true)
    private val noneAllowed = PermissionStatus(notificationsAllowed = false, exactAlarmsAllowed = false)

    @Test
    fun `약이 없으면 권한이 없어도 배너를 띄우지 않는다`() {
        assertNull(bannerFor(noneAllowed, hasMedicines = false))
    }

    @Test
    fun `모두 허용이면 배너가 없다`() {
        assertNull(bannerFor(allAllowed))
    }

    @Test
    fun `알림과 정확한 알람이 모두 막혀 있으면 알림 배너를 먼저 띄운다`() {
        assertEquals(PermissionBanner.Notification, bannerFor(noneAllowed))
    }

    @Test
    fun `정확한 알람만 막혀 있으면 정확한 알람 배너를 띄운다`() {
        assertEquals(PermissionBanner.ExactAlarm, bannerFor(allAllowed.copy(exactAlarmsAllowed = false)))
    }

    @Test
    fun `알림 권한 대화상자가 떠 있는 동안에는 배너를 숨긴다`() {
        assertNull(bannerFor(noneAllowed, askingNotification = true))
    }

    @Test
    fun `첫 약 등록이고 알림이 꺼져 있으면 등록 시 권한을 묻는다`() {
        assertTrue(shouldAskNotificationOnAdd(noneAllowed, hasMedicines = false))
    }

    @Test
    fun `이미 약이 있으면 등록 시 권한을 묻지 않는다`() {
        assertFalse(shouldAskNotificationOnAdd(noneAllowed, hasMedicines = true))
    }

    @Test
    fun `알림이 허용되어 있으면 등록 시 권한을 묻지 않는다`() {
        assertFalse(shouldAskNotificationOnAdd(allAllowed, hasMedicines = false))
    }

    @Test
    fun `대화상자에서 다시 거부하면 설정으로 보내지 않는다`() {
        assertFalse(shouldOpenNotificationSettings(granted = false, couldAskBefore = true, canAskAfter = false))
    }

    @Test
    fun `처음 물어서 거부하면 설정으로 보내지 않는다`() {
        assertFalse(shouldOpenNotificationSettings(granted = false, couldAskBefore = false, canAskAfter = true))
    }

    @Test
    fun `다시 물을 수 없는 상태에서 거부되면 설정으로 보낸다`() {
        assertTrue(shouldOpenNotificationSettings(granted = false, couldAskBefore = false, canAskAfter = false))
    }

    @Test
    fun `허용하면 설정으로 보내지 않는다`() {
        assertFalse(shouldOpenNotificationSettings(granted = true, couldAskBefore = false, canAskAfter = false))
    }

    private fun bannerFor(status: PermissionStatus, hasMedicines: Boolean = true, askingNotification: Boolean = false) =
        permissionBannerFor(status, hasMedicines, askingNotification)
}
