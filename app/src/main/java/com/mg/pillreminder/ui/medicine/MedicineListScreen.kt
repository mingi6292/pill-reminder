package com.mg.pillreminder.ui.medicine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.pillreminder.R
import com.mg.pillreminder.ui.theme.PillReminderTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NEXT_REMINDER_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d HH:mm")
private val REMIND_LATER_HOURS = 1L..4L

@Composable
fun MedicineListScreen(viewModel: MedicineViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.reminderScheduled.collect { triggerAt ->
            val formatted = formatShortDateTime(triggerAt, Instant.now(), ZoneId.systemDefault())
            snackbarHostState.showSnackbar(resources.getString(R.string.medicine_remind_later_scheduled, formatted))
        }
    }
    // 설정 화면에서 권한을 바꾸고 돌아올 수 있어 화면에 돌아올 때마다 다시 읽는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }
    val permissionActions = rememberPermissionActions(
        onAskStarted = viewModel::onNotificationRequestStarted,
        onAskFinished = viewModel::onNotificationRequestFinished,
        onBannerRequestFinished = viewModel::refreshPermissions,
    )

    MedicineListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddMedicine = { name, time ->
            // 약이 저장되기 전에 요청 중 상태를 켜야 배너가 대화상자 뒤에 잠깐도 보이지 않는다.
            if (uiState.askNotificationOnAdd) permissionActions.askNotification()
            viewModel.addMedicine(name, time)
        },
        onDeleteMedicine = viewModel::deleteMedicine,
        onRemindLater = viewModel::remindLater,
        onAcceptInterval = viewModel::acceptIntervalSuggestion,
        onRejectInterval = viewModel::rejectIntervalSuggestion,
        onPermissionBannerClick = permissionActions::resolve,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListContent(
    uiState: MedicineUiState,
    snackbarHostState: SnackbarHostState,
    onAddMedicine: (name: String, time: LocalTime) -> Unit,
    onDeleteMedicine: (medicineId: Long) -> Unit,
    onRemindLater: (medicineId: Long, after: Duration) -> Unit,
    onAcceptInterval: (medicineId: Long, interval: Duration) -> Unit,
    onRejectInterval: (medicineId: Long) -> Unit,
    onPermissionBannerClick: (PermissionBanner) -> Unit,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.medicine_list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.medicine_add)) },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(top = innerPadding.calculateTopPadding())) {
            uiState.permissionBanner?.let { banner ->
                PermissionBannerCard(banner, onClick = { onPermissionBannerClick(banner) })
            }
            MedicineListBody(
                uiState = uiState,
                onDeleteMedicine = onDeleteMedicine,
                onRemindLater = onRemindLater,
                onAcceptInterval = onAcceptInterval,
                onRejectInterval = onRejectInterval,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            )
        }
    }

    if (showAddSheet) {
        AddMedicineSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, time ->
                onAddMedicine(name, time)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun MedicineListBody(
    uiState: MedicineUiState,
    onDeleteMedicine: (Long) -> Unit,
    onRemindLater: (Long, Duration) -> Unit,
    onAcceptInterval: (Long, Duration) -> Unit,
    onRejectInterval: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val centered = Modifier
        .fillMaxSize()
        .padding(contentPadding)
    when {
        uiState.isLoading -> Box(centered, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.medicines.isEmpty() -> Box(centered, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.medicine_list_empty), textAlign = TextAlign.Center)
        }
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            items(uiState.medicines, key = { it.id }) { medicine ->
                Column {
                    MedicineRow(
                        medicine = medicine,
                        onDelete = { onDeleteMedicine(medicine.id) },
                        onRemindLater = { after -> onRemindLater(medicine.id, after) },
                    )
                    medicine.suggestedInterval?.let { interval ->
                        IntervalSuggestionBanner(
                            interval = interval,
                            onAccept = { onAcceptInterval(medicine.id, interval) },
                            onReject = { onRejectInterval(medicine.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineRow(medicine: MedicineItem, onDelete: () -> Unit, onRemindLater: (Duration) -> Unit) {
    ListItem(
        headlineContent = { Text(medicine.name) },
        supportingContent = {
            Column {
                Text(nextReminderText(medicine.nextReminderAt))
                medicine.lastTakenAt?.let { Text(lastTakenText(it)) }
            }
        },
        trailingContent = {
            Row {
                RemindLaterButton(onRemindLater)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.medicine_delete))
                }
            }
        },
    )
}

@Composable
private fun RemindLaterButton(onRemindLater: (Duration) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.medicine_remind_later))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            REMIND_LATER_HOURS.forEach { hours ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.medicine_remind_after_hours, hours)) },
                    onClick = {
                        expanded = false
                        onRemindLater(Duration.ofHours(hours))
                    },
                )
            }
        }
    }
}

@Composable
private fun nextReminderText(nextReminderAt: Instant?): String {
    if (nextReminderAt == null) return stringResource(R.string.medicine_no_reminder)
    // 사용자가 타임존을 바꿀 수 있어 그릴 때마다 시스템 타임존으로 바꾼다.
    val formatted = NEXT_REMINDER_FORMAT.format(nextReminderAt.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.medicine_next_reminder, formatted)
}

@Composable
private fun lastTakenText(lastTakenAt: Instant): String {
    val formatted = formatShortDateTime(lastTakenAt, Instant.now(), ZoneId.systemDefault())
    return stringResource(R.string.medicine_last_taken, formatted)
}

@Preview(showBackground = true)
@Composable
private fun MedicineListContentPreview() {
    PillReminderTheme {
        MedicineListContent(
            uiState = MedicineUiState(
                isLoading = false,
                medicines = listOf(
                    MedicineItem(1, "비타민", Instant.parse("2026-11-16T23:00:00Z"), Instant.parse("2026-11-15T23:05:00Z")),
                    MedicineItem(2, "유산균", null),
                    MedicineItem(
                        id = 3,
                        name = "진통제",
                        nextReminderAt = null,
                        lastTakenAt = Instant.parse("2026-11-16T03:00:00Z"),
                        suggestedInterval = Duration.ofHours(4),
                    ),
                ),
                permissionBanner = PermissionBanner.ExactAlarm,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAddMedicine = { _, _ -> },
            onDeleteMedicine = {},
            onRemindLater = { _, _ -> },
            onAcceptInterval = { _, _ -> },
            onRejectInterval = {},
            onPermissionBannerClick = {},
        )
    }
}
