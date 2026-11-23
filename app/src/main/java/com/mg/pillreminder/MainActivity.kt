package com.mg.pillreminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mg.pillreminder.ui.medicine.MedicineListScreen
import com.mg.pillreminder.ui.theme.PillReminderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 진입점으로 열릴 때마다 올린다. 화면은 값이 바뀔 때만 음성 시트를 연다.
    private var voiceLaunchRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 회전 등으로 다시 만들어질 때는 처음 열린 요청을 반복하지 않는다.
        if (savedInstanceState == null) requestVoiceIfAsked(intent)
        setContent {
            PillReminderTheme {
                MedicineListScreen(voiceLaunchRequest = voiceLaunchRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestVoiceIfAsked(intent)
    }

    private fun requestVoiceIfAsked(intent: Intent) {
        if (isVoiceLaunch(intent)) voiceLaunchRequest++
    }

    companion object {
        /** 바로가기나 `adb shell am start --ez`가 딥링크 대신 쓸 수 있는 extra. */
        const val EXTRA_START_VOICE = "com.mg.pillreminder.extra.START_VOICE"

        private const val VOICE_SCHEME = "pillreminder"
        private const val VOICE_HOST = "voice"

        fun isVoiceLaunch(intent: Intent): Boolean {
            val data = intent.data
            val byDeepLink = data?.scheme == VOICE_SCHEME && data.host == VOICE_HOST
            return byDeepLink || intent.getBooleanExtra(EXTRA_START_VOICE, false)
        }
    }
}
