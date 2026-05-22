package com.farbalapps.rinde.ui.screen.signup

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.components.AuthBackground
import com.farbalapps.rinde.ui.screen.signup.components.*
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSignUpSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
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
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(id = R.dimen.padding_large)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensionResource(id = R.dimen.max_width_phone))
                        .padding(horizontal = dimensionResource(id = R.dimen.padding_large))
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.signup_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_xlarge))
                    )
                    
                    SignUpFormFields(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it }
                    )

                    TermsAndPrivacyCheckbox(
                        checked = agreeToTerms,
                        onCheckedChange = { agreeToTerms = it },
                        onPrivacyPolicyClick = onPrivacyPolicyClick
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                    Button(
                        onClick = { 
                            if (agreeToTerms) {
                                viewModel.signUp(fullName, email, password)
                            } else {
                                val message = context.getString(R.string.error_agree_to_terms)
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(id = R.dimen.button_height_standard)),
                        enabled = !state.isLoading,
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium)),
                                strokeWidth = dimensionResource(id = R.dimen.stroke_medium)
                            )
                        } else {
                            Text(
                                stringResource(id = R.string.btn_sign_up),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.error != null) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_small))
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                    Text(
                        text = stringResource(id = R.string.social_connect),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = dimensionResource(id = R.dimen.text_size_small).value.sp
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_medium)))

                    GoogleSignUpButton(
                        onTokenReceived = { token -> viewModel.onGoogleSignInResult(token) },
                        onError = { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() }
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_large)))

                    LoginRedirectSection(onSignInClick = onSignInClick)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    RindeTheme {
        SignUpScreen(onBackClick = {}, onSignInClick = {}, onSignUpSuccess = {}, onPrivacyPolicyClick = {})
    }
}
