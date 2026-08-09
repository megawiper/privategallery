package com.privategallery.app.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GalleryConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            GalleryTextActionButton(
                text = confirmLabel,
                onClick = onConfirm
            )
        },
        dismissButton = {
            GalleryTextActionButton(
                text = dismissLabel,
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun GalleryInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    singleLine = true
                )
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            GalleryTextActionButton(text = confirmLabel, onClick = onConfirm)
        },
        dismissButton = {
            GalleryTextActionButton(text = dismissLabel, onClick = onDismiss)
        }
    )
}
