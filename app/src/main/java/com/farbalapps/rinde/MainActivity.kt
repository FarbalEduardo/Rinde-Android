package com.farbalapps.rinde

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farbalapps.rinde.ui.screen.login.LoginScreen
import com.farbalapps.rinde.ui.screen.login.LoginViewModel
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.ui.screen.welcome.WelcomeScreen
import com.farbalapps.rinde.ui.screen.signup.SignUpScreen
import com.farbalapps.rinde.ui.screen.privacy.PrivacyPolicyScreen
import com.farbalapps.rinde.ui.screen.home.HomeScreen
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            RindeTheme {
                val navController = rememberNavController()
                val isLoggedByFirebase = authRepository.isUserLoggedIn()
                val isLoggedBySessionManager by sessionManager.isUserLoggedIn.collectAsState(initial = false)
                
                val startDestination = remember {
                    if (isLoggedByFirebase || isLoggedBySessionManager) "home" else "welcome"
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("welcome") {
                        WelcomeScreen(
                            onSignInClick = { navController.navigate("login") },
                            onSignUpClick = { navController.navigate("signup") }
                        )
                    }

                    composable("signup") {
                        SignUpScreen(
                            onBackClick = { navController.popBackStack() },
                            onPrivacyPolicyClick = { navController.navigate("privacy_policy") },
                            onSignInClick = {
                                navController.navigate("login") {
                                    popUpTo("welcome")
                                }
                            },
                            onSignUpSuccess = {
                                navController.navigate("home") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("login") {
                        val loginViewModel: LoginViewModel = hiltViewModel()
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onSignUpClick = {
                                navController.navigate("signup")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onLogout = {
                                authRepository.logout()
                                navController.navigate("welcome") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("privacy_policy") {
                        PrivacyPolicyScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RindeTheme {
        Text("Preview")
    }
}