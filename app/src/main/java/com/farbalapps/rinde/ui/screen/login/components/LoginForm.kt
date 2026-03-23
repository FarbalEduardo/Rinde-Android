package com.farbalapps.rinde.ui.screen.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.login.LoginUIState

@Composable
fun LoginForm(
    state: LoginUIState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        EmailInputField(state = state, onEmailChanged = onEmailChanged)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_small)))

        PasswordInputField(
            state = state, 
            onPasswordChanged = onPasswordChanged,
            passwordVisible = passwordVisible,
            onVisibilityChanged = { passwordVisible = !passwordVisible }
        )

        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(id = R.string.btn_forgot_password), color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

        AnimatedVisibility(visible = state.loginError != null) {
            Text(
                text = state.loginError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium))
            )
        }

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button"),
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.large
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    stringResource(id = R.string.btn_sign_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginFormPreview() {
    LoginForm(
        state = LoginUIState(),
        onEmailChanged = {},
        onPasswordChanged = {},
        onLoginClick = {},
        onForgotPasswordClick = {}
    )
}

@Composable
private fun EmailInputField(state: LoginUIState, onEmailChanged: (String) -> Unit) {
    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChanged,
        label = { Text(stringResource(id = R.string.label_email)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_input"),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
        isError = state.emailError != null,
        supportingText = {
            state.emailError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        },
        leadingIcon = {
            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}

@Composable
private fun PasswordInputField(
    state: LoginUIState,
    onPasswordChanged: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityChanged: () -> Unit
) {
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChanged,
        label = { Text(stringResource(id = R.string.label_password)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("password_input"),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
        isError = state.passwordError != null,
        supportingText = {
            state.passwordError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            IconButton(onClick = onVisibilityChanged) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}