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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }
    
    LoginContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::onLoginClick
    )
}


@Composable
fun LoginContent(
    state: LoginUIState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFBBDEFB),
                        Color(0xFF90CAF9)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChanged,
                        label = { Text("Username or Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = state.emailError != null,
                        supportingText = {
                            if (state.emailError != null) {
                                Text(text = state.emailError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            errorTextColor = Color.Black,
                            focusedLabelColor = Color(0xFF0D6CF2),
                            unfocusedLabelColor = Color.DarkGray,
                            errorLabelColor = Color(0xFFB00020),
                            focusedBorderColor = Color(0xFF0D6CF2),
                            unfocusedBorderColor = Color.LightGray,
                            errorBorderColor = Color(0xFFB00020),
                            errorLeadingIconColor = Color(0xFFB00020)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChanged,
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = state.passwordError != null,
                        supportingText = {
                            if (state.passwordError != null) {
                                Text(text = state.passwordError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.DarkGray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            errorTextColor = Color.Black,
                            focusedLabelColor = Color(0xFF0D6CF2),
                            unfocusedLabelColor = Color.DarkGray,
                            errorLabelColor = Color(0xFFB00020),
                            focusedBorderColor = Color(0xFF0D6CF2),
                            unfocusedBorderColor = Color.LightGray,
                            errorBorderColor = Color(0xFFB00020),
                            errorLeadingIconColor = Color(0xFFB00020),
                            errorTrailingIconColor = Color(0xFFB00020)
                        )
                    )



                    TextButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot?", color = Color(0xFF0D6CF2))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.loginError != null) {
                        Text(
                            text = state.loginError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(28.dp), // M3-style rounded corners
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D6CF2),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("SOCIAL CONNECT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier.weight(1.0f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                        OutlinedButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier.weight(1.0f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_facebook),
                                contentDescription = "Facebook Logo",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Facebook",
                                color = Color(0xFF1877F2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    Row {
                        Text("New here? ", color = Color.Gray)
                        Text(
                            text = "Create Account",
                            color = Color(0xFF0D6CF2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
            onLoginClick = {}
        )
    }
}
