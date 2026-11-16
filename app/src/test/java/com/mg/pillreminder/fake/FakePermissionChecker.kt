package com.mg.pillreminder.fake

import com.mg.pillreminder.domain.permission.PermissionChecker
import com.mg.pillreminder.domain.permission.PermissionStatus

class FakePermissionChecker(
    var status: PermissionStatus = PermissionStatus(notificationsAllowed = true, exactAlarmsAllowed = true),
) : PermissionChecker {
    override fun status(): PermissionStatus = status
}
