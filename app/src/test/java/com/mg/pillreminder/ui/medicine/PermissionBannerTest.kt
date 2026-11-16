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
        assertNull(permissionBannerFor(noneAllowed, hasMedicines = false))
    }

    @Test
    fun `모두 허용이면 배너가 없다`() {
        assertNull(permissionBannerFor(allAllowed, hasMedicines = true))
    }

    @Test
    fun `알림과 정확한 알람이 모두 막혀 있으면 알림 배너를 먼저 띄운다`() {
        assertEquals(PermissionBanner.Notification, permissionBannerFor(noneAllowed, hasMedicines = true))
    }

    @Test
    fun `정확한 알람만 막혀 있으면 정확한 알람 배너를 띄운다`() {
        val status = allAllowed.copy(exactAlarmsAllowed = false)

        assertEquals(PermissionBanner.ExactAlarm, permissionBannerFor(status, hasMedicines = true))
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
}
