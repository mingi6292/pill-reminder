package com.mg.pillreminder.ui.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.pillreminder.R

/**
 * 마이크 버튼이나 진입점으로 열린다. 열리면 권한을 확인하고 바로 듣기 시작한다.
 * [onRegisterMedicine]은 "등록 안 된 약을 새로 등록할까요?"에 응이라고 답했을 때 부른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSheet(
    onDismiss: () -> Unit,
    onRegisterMedicine: (name: String) -> Unit,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnRegister by rememberUpdatedState(onRegisterMedicine)
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) viewModel.start() else viewModel.onPermissionDenied()
    }

    // 권한 대화상자를 거쳐도 시트는 한 번만 열린 것이라 시작은 한 번만 한다.
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    LaunchedEffect(viewModel) { viewModel.registrationRequests.collect { latestOnRegister(it) } }
    DisposableEffect(viewModel) { onDispose { viewModel.stop() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        VoiceSheetContent(uiState, onMicClick = viewModel::listenAgain)
    }
}

@Composable
fun VoiceSheetContent(uiState: VoiceUiState, onMicClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(statusText(uiState), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        uiState.heard?.let { Text("\"$it\"", textAlign = TextAlign.Center) }
        uiState.reply?.let { Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
        FilledIconButton(onClick = onMicClick, enabled = uiState.phase == VoicePhase.Idle) {
            Icon(painterResource(R.drawable.ic_mic), contentDescription = stringResource(R.string.voice_listen))
        }
    }
}

@Composable
private fun statusText(uiState: VoiceUiState): String = when {
    uiState.problem == VoiceProblem.NoSpeech -> stringResource(R.string.voice_no_speech)
    uiState.problem == VoiceProblem.PermissionDenied -> stringResource(R.string.voice_permission_denied)
    uiState.problem == VoiceProblem.Unavailable -> stringResource(R.string.voice_unavailable)
    uiState.phase == VoicePhase.Listening -> stringResource(R.string.voice_listening)
    uiState.phase == VoicePhase.Thinking -> stringResource(R.string.voice_thinking)
    uiState.phase == VoicePhase.Speaking -> stringResource(R.string.voice_speaking)
    else -> stringResource(R.string.voice_idle)
}
