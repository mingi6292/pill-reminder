package com.mg.pillreminder.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mg.pillreminder.R
import java.time.LocalTime

private const val DEFAULT_HOUR = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineSheet(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, time: LocalTime) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val timeState = rememberTimePickerState(initialHour = DEFAULT_HOUR, initialMinute = 0, is24Hour = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.medicine_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TimePicker(state = timeState)
            Button(
                onClick = { onSave(name, LocalTime.of(timeState.hour, timeState.minute)) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.medicine_save))
            }
        }
    }
}
