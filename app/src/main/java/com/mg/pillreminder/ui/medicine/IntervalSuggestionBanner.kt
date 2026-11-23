package com.mg.pillreminder.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mg.pillreminder.R
import java.time.Duration

/** 약 행 아래에 붙는 "N시간마다 알림 받을까요?" 배너. */
@Composable
fun IntervalSuggestionBanner(
    interval: Duration,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.interval_suggestion_message, intervalText(interval)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReject) { Text(stringResource(R.string.interval_suggestion_reject)) }
            TextButton(onClick = onAccept) { Text(stringResource(R.string.interval_suggestion_accept)) }
        }
    }
}

// 간격은 30분 단위로 반올림돼 들어오므로 시간과 분만 표시하면 된다.
@Composable
private fun intervalText(interval: Duration): String {
    val hours = interval.toHours()
    val minutes = interval.toMinutes() % 60
    return if (minutes == 0L) {
        stringResource(R.string.interval_hours, hours)
    } else {
        stringResource(R.string.interval_hours_minutes, hours, minutes)
    }
}
