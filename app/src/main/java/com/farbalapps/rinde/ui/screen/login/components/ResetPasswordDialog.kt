package com.farbalapps.rinde.ui.screen.login.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R

@Composable
fun ResetPasswordDialog(
    isOpen: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(id = R.string.btn_forgot_password)) },
            text = {
                Column {
                    Text(
                        text = "Enter your email to receive a reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    EmailInputDialogField(
                        email = email,
                        emailError = emailError,
                        onEmailChange = { 
                            email = it
                            emailError = null 
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.contains("@") && email.contains(".")) {
                            onConfirm(email)
                        } else {
                            emailError = "Please enter a valid email address"
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun EmailInputDialogField(
    email: String,
    emailError: String?,
    onEmailChange: (String) -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(id = R.string.label_email)) },
        modifier = Modifier.fillMaxWidth(),
        isError = emailError != null,
        supportingText = {
            emailError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}
