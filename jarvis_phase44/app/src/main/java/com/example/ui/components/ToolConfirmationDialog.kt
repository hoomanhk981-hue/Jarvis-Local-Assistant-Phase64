package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.assistant.ConfirmationManager

@Composable
fun ToolConfirmationDialog(
    request: ConfirmationManager.Request,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text("تأیید عملیات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(request.summary, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "سطح حساسیت: ${request.risk.name}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "این تأیید فقط برای همین عملیات و تا ۲ دقیقه معتبر است.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onApprove) { Text("تأیید می‌کنم") }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) { Text("لغو") }
        }
    )
}
