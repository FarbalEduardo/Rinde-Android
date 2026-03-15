package com.farbalapps.rinde.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.farbalapps.rinde.ui.components.AuthBackground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.app.Activity
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import com.farbalapps.rinde.domain.util.Resource

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val resetState by viewModel.resetPasswordState.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(resetState) {
        resetState?.let {
            when (it) {
                is Resource.Success -> {
                    Toast.makeText(context, "Reset email sent successfully!", Toast.LENGTH_LONG).show()
                    showResetDialog = false
                    viewModel.clearResetPasswordState()
                }
                is Resource.Error -> {
                    Toast.makeText(context, it.message ?: "Failed to send reset email", Toast.LENGTH_LONG).show()
                    viewModel.clearResetPasswordState()
                }
                is Resource.Loading -> { /* Handled in dialog */ }
                else -> { /* Fallback for safe compile */ }
            }
        }
    }

    LoginContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::onLoginClick,
        onGoogleSignInClick = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                    val idToken = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                    viewModel.onGoogleSignInResult(idToken)
                } catch (e: Exception) {
                    Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onForgotPasswordClick = { showResetDialog = true },
        onSignUpClick = onSignUpClick,
        onBackClick = onBackClick
    )

    if (showResetDialog) {
        ResetPasswordDialog(
            isOpen = true,
            isLoading = resetState is Resource.Loading,
            onDismiss = { showResetDialog = false },
            onConfirm = { email -> viewModel.resetPassword(email) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    state: LoginUIState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { }, // Título vacío como solicitó el usuario
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AuthBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(dimensionResource(id = R.dimen.padding_large)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 400.dp) // Responsive width limit
                        .padding(horizontal = dimensionResource(id = R.dimen.padding_xlarge))
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.login_welcome),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_small))
                    )
                    Text(
                        text = stringResource(id = R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            bottom = dimensionResource(id = R.dimen.padding_xlarge)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    var passwordVisible by remember { mutableStateOf(false) }

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
                            if (state.emailError != null) {
                                Text(text = state.emailError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            errorTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLeadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_small)))

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
                            if (state.passwordError != null) {
                                Text(text = state.passwordError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            errorTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLeadingIconColor = MaterialTheme.colorScheme.error,
                            errorTrailingIconColor = MaterialTheme.colorScheme.error
                        )
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
                            .height(dimensionResource(id = R.dimen.button_height_standard))
                            .testTag("login_button"),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.card_corner_radius)), // M3-style rounded corners
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 8.dp
                        )
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

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                    Text(stringResource(id = R.string.social_connect), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

                    OutlinedButton(
                        onClick = onGoogleSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = stringResource(id = R.string.social_google),
                            modifier = Modifier.size(dimensionResource(id = R.dimen.social_icon_size)),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("G") }
                                withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("o") }
                                withStyle(SpanStyle(color = Color(0xFFFBBC05))) { append("o") }
                                withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("g") }
                                withStyle(SpanStyle(color = Color(0xFF34A853))) { append("l") }
                                withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("e") }
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }


                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.new_here), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onSignUpClick) {
                            Text(
                                text = stringResource(id = R.string.create_account),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


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
                        "Enter your email to receive a reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = null 
                        },
                        label = { Text(stringResource(id = R.string.label_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(emailError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
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


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    RindeTheme {
        LoginContent(
            state = LoginUIState(email = "test@example.com"),
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClick = {},
            onGoogleSignInClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentEmptyPreview() {
    LoginContent(
        state = LoginUIState(),
        onEmailChanged = {},
        onPasswordChanged = {},
        onLoginClick = {},
        onGoogleSignInClick = {},
        onForgotPasswordClick = {},
        onSignUpClick = {},
        onBackClick = {}
    )
}
