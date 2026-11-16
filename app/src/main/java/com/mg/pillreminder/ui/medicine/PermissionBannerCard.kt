package com.mg.pillreminder.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mg.pillreminder.R

@Composable
fun PermissionBannerCard(banner: PermissionBanner, onClick: () -> Unit) {
    val (title, message) = when (banner) {
        PermissionBanner.Notification -> R.string.permission_notification_title to R.string.permission_notification_message
        PermissionBanner.ExactAlarm -> R.string.permission_exact_alarm_title to R.string.permission_exact_alarm_message
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
