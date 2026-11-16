package com.mg.pillreminder.alarm

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * onReceive가 돌아간 뒤에도 [block]이 끝날 때까지 프로세스를 살려 둔다.
 * DB 작업은 메인 스레드에서 못 하고, goAsync 없이 띄운 코루틴은 프로세스가 죽으면 사라지기 때문이다.
 * 시스템 제한(약 10초) 안에 끝나는 짧은 작업에만 쓴다.
 */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
