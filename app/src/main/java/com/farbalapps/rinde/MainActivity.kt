package com.farbalapps.rinde

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.data.repository.FirebaseAuthRepository
import com.farbalapps.rinde.domain.usecase.LoginUseCase
import com.farbalapps.rinde.ui.screen.login.LoginScreen
import com.farbalapps.rinde.ui.screen.login.LoginViewModel
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Manual DI for demonstration (In a real app, use Hilt)
        val authRepository = FirebaseAuthRepository()
        val loginUseCase = LoginUseCase(authRepository)
        val sessionManager = SessionManager(this)
        val loginViewModel = LoginViewModel(loginUseCase, sessionManager)
        
        setContent {
            RindeTheme {
                val navController = rememberNavController()
                val isLoggedByFirebase = authRepository.isUserLoggedIn()
                val isLoggedBySessionManager by sessionManager.isUserLoggedIn.collectAsState(initial = false)
                
                // Decide start destination based on either Firebase or DataStore session
                val startDestination = if (isLoggedByFirebase || isLoggedBySessionManager) "home" else "login"
                
                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen {
                            authRepository.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Welcome to Home Screen!")
            Button(onClick = onLogout, modifier = Modifier.padding(top = 16.dp)) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Helhkjhhkjlo $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RindeTheme {
        Greeting("Android")
    }
}