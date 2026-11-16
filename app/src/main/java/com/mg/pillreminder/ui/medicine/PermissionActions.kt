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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.mg.pillreminder.permission.PermissionSettings

/** 권한 요청 대화상자와 설정 화면 이동처럼 Activity가 있어야 하는 일만 맡는다. */
class PermissionActions(
    private val context: Context,
    private val askLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val bannerLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    fun askNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun resolve(banner: PermissionBanner) {
        when (banner) {
            PermissionBanner.Notification -> resolveNotification()
            PermissionBanner.ExactAlarm -> openExactAlarmSettings()
        }
    }

    private fun resolveNotification() {
        // 12 이하는 런타임 권한이 없어 사용자가 설정에서 직접 켜야 한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bannerLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(PermissionSettings.appNotificationSettings(context))
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(PermissionSettings.exactAlarmSettings(context))
        }
    }
}

@Composable
fun rememberPermissionActions(onResult: () -> Unit): PermissionActions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val askLauncher = rememberLauncherForActivityResult(RequestPermission()) { onResult() }
    val bannerLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        onResult()
        // 다시 물을 수 없는 상태면 시스템이 대화상자 없이 바로 거부를 돌려주므로, 그때 설정 화면으로 보낸다.
        if (!granted && !canAskNotificationAgain(activity)) {
            context.startActivity(PermissionSettings.appNotificationSettings(context))
        }
    }
    return remember(context, askLauncher, bannerLauncher) {
        PermissionActions(context, askLauncher, bannerLauncher)
    }
}

private fun canAskNotificationAgain(activity: Activity?): Boolean = activity != null &&
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
