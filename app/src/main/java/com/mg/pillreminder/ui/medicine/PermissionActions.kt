package com.mg.pillreminder.ui.medicine

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.mg.pillreminder.permission.PermissionSettings

/** 권한 요청 대화상자와 설정 화면 이동처럼 Activity가 있어야 하는 일만 맡는다. */
class PermissionActions(
    private val context: Context,
    private val activity: Activity?,
    private val onAskStarted: () -> Unit,
    private val askLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val bannerLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val couldAskBeforeBannerRequest: MutableState<Boolean>,
) {
    fun askNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        onAskStarted()
        askLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun resolve(banner: PermissionBanner) {
        when (banner) {
            PermissionBanner.Notification -> resolveNotification()
            PermissionBanner.ExactAlarm -> openExactAlarmSettings()
        }
    }

    private fun resolveNotification() {
        // 12 이하는 런타임 권한이 없어 사용자가 설정에서 직접 켜야 한다.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.startActivity(PermissionSettings.appNotificationSettings(context))
            return
        }
        couldAskBeforeBannerRequest.value = canAskNotificationAgain(activity)
        bannerLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(PermissionSettings.exactAlarmSettings(context))
        }
    }
}

@Composable
fun rememberPermissionActions(
    onAskStarted: () -> Unit,
    onAskFinished: () -> Unit,
    onBannerRequestFinished: () -> Unit,
): PermissionActions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    // 대화상자가 떠 있는 동안 화면이 회전해도 요청 전 상태를 잃지 않게 저장한다.
    val couldAskBefore = rememberSaveable { mutableStateOf(false) }
    val askLauncher = rememberLauncherForActivityResult(RequestPermission()) { onAskFinished() }
    val bannerLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        onBannerRequestFinished()
        if (shouldOpenNotificationSettings(granted, couldAskBefore.value, canAskNotificationAgain(activity))) {
            context.startActivity(PermissionSettings.appNotificationSettings(context))
        }
    }
    return remember(context, activity, askLauncher, bannerLauncher) {
        PermissionActions(context, activity, onAskStarted, askLauncher, bannerLauncher, couldAskBefore)
    }
}

private fun canAskNotificationAgain(activity: Activity?): Boolean = activity != null &&
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
