package com.developersbeeh.pharmaflow

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.features.auth.AccessDeniedScreen
import com.developersbeeh.pharmaflow.features.auth.ForgotPasswordScreen
import com.developersbeeh.pharmaflow.features.auth.LoginScreen
import com.developersbeeh.pharmaflow.features.auth.RegisterScreen
import com.developersbeeh.pharmaflow.features.auth.SplashScreen
import com.developersbeeh.pharmaflow.ui.navigation.AppNavigator
import com.developersbeeh.pharmaflow.ui.theme.PharmaFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PharmaFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel = hiltViewModel<MainViewModel>()
                    val startDestination by viewModel.startDestination.collectAsState()

                    var isRegistering by remember { mutableStateOf(false) }
                    var isRecovering by remember { mutableStateOf(false) }

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { }
                    )

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    when (val dest = startDestination) {
                        is StartDestination.Loading -> {
                            SplashScreen()
                        }
                        is StartDestination.Login -> {
                            when {
                                isRegistering -> {
                                    RegisterScreen(
                                        onRegisterSuccess = {
                                            isRegistering = false
                                            viewModel.checkAuthStatus()
                                        },
                                        onBackToLogin = { isRegistering = false }
                                    )
                                }
                                isRecovering -> {
                                    ForgotPasswordScreen(onBackClick = { isRecovering = false })
                                }
                                else -> {
                                    LoginScreen(
                                        onLoginSuccess = { viewModel.checkAuthStatus() },
                                        onNavigateToRegister = { isRegistering = true },
                                        onNavigateToForgot = { isRecovering = true }
                                    )
                                }
                            }
                        }
                        // Agora pega o "reason" (motivo)
                        is StartDestination.AccessDenied -> {
                            AccessDeniedScreen(
                                reason = dest.reason,
                                onLogout = { viewModel.logout() }
                            )
                        }
                        is StartDestination.Home -> {
                            navigator.NavigateToHome()
                        }
                    }
                }
            }
        }
    }
}