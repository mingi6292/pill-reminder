package com.mg.pillreminder.ui.medicine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.pillreminder.R
import com.mg.pillreminder.ui.theme.PillReminderTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NEXT_REMINDER_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d HH:mm")

@Composable
fun MedicineListScreen(viewModel: MedicineViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MedicineListContent(
        uiState = uiState,
        onAddMedicine = viewModel::addMedicine,
        onDeleteMedicine = viewModel::deleteMedicine,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListContent(
    uiState: MedicineUiState,
    onAddMedicine: (name: String, time: LocalTime) -> Unit,
    onDeleteMedicine: (medicineId: Long) -> Unit,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.medicine_list_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddSheet = true }) {
                Text(stringResource(R.string.medicine_add))
            }
        },
    ) { innerPadding ->
        MedicineListBody(uiState, onDeleteMedicine, innerPadding)
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
                MedicineRow(medicine, onDelete = { onDeleteMedicine(medicine.id) })
            }
        }
    }
}

@Composable
private fun MedicineRow(medicine: MedicineItem, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(medicine.name) },
        supportingContent = { Text(nextReminderText(medicine.nextReminderAt)) },
        trailingContent = {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.medicine_delete)) }
        },
    )
}

@Composable
private fun nextReminderText(nextReminderAt: Instant?): String {
    if (nextReminderAt == null) return stringResource(R.string.medicine_no_reminder)
    // 사용자가 타임존을 바꿀 수 있어 그릴 때마다 시스템 타임존으로 바꾼다.
    val formatted = NEXT_REMINDER_FORMAT.format(nextReminderAt.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.medicine_next_reminder, formatted)
}

@Preview(showBackground = true)
@Composable
private fun MedicineListContentPreview() {
    PillReminderTheme {
        MedicineListContent(
            uiState = MedicineUiState(
                isLoading = false,
                medicines = listOf(
                    MedicineItem(1, "비타민", Instant.parse("2026-11-16T23:00:00Z")),
                    MedicineItem(2, "유산균", null),
                ),
            ),
            onAddMedicine = { _, _ -> },
            onDeleteMedicine = {},
        )
    }
}
