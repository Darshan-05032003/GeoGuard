package com.example.geoguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geoguard.ui.auth.AuthViewModel
import com.example.geoguard.ui.auth.LoginScreen
import com.example.geoguard.ui.home.HomeScreen
import com.example.geoguard.ui.theme.GeoGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GeoGuardApp()
                }
            }
        }
    }
}

@Composable
fun GeoGuardApp(authViewModel: AuthViewModel = viewModel()) {
    val uiState by authViewModel.uiState.collectAsState()

    if (uiState.isLoggedIn) {
        HomeScreen(onLogout = authViewModel::logout)
    } else {
        LoginScreen(
            uiState = uiState,
            onLogin = authViewModel::login,
            onRegister = authViewModel::register
        )
    }
}