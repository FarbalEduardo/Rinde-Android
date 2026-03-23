package com.farbalapps.rinde.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.util.Resource
import com.farbalapps.rinde.ui.components.AuthBackground
import com.farbalapps.rinde.ui.screen.login.components.*
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

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
                else -> {}
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
                    val result = credentialManager.getCredential(context, request)
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

    ResetPasswordDialog(
        isOpen = showResetDialog,
        isLoading = resetState is Resource.Loading,
        onDismiss = { showResetDialog = false },
        onConfirm = { email -> viewModel.resetPassword(email) }
    )
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
                title = { },
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
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LoginBodyContent(
            state = state,
            onEmailChanged = onEmailChanged,
            onPasswordChanged = onPasswordChanged,
            onLoginClick = onLoginClick,
            onForgotPasswordClick = onForgotPasswordClick,
            onGoogleSignInClick = onGoogleSignInClick,
            onSignUpClick = onSignUpClick
        )
    }
}

@Composable
fun SignUpOption(onSignUpClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.new_here),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onSignUpClick) {
            Text(
                text = stringResource(id = R.string.create_account),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun LoginBodyContent(
    state: LoginUIState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp) // Optimized for phones and tablets
                    .padding(horizontal = dimensionResource(id = R.dimen.padding_xlarge))
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginHeader()
                
                LoginForm(
                    state = state,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClick = onLoginClick,
                    onForgotPasswordClick = onForgotPasswordClick
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                SocialButtons(onGoogleSignInClick = onGoogleSignInClick)

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                SignUpOption(onSignUpClick = onSignUpClick)
            }
        }
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
