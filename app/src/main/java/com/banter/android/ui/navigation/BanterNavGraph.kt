package com.banter.android.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banter.android.ui.screen.auth.AuthViewModel
import com.banter.android.ui.screen.auth.LoginScreen
import com.banter.android.ui.screen.auth.RegisterScreen
import com.banter.android.ui.screen.auth.ServerSetupScreen
import com.banter.android.ui.screen.main.MainScreen

sealed class Screen(val route: String) {
    object ServerSetup : Screen("server_setup")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
}

@Composable
fun BanterNavGraph() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val startRoute by authVm.startRoute.collectAsState()

    // Show nothing while determining start route (brief splash)
    val start = startRoute ?: return

    NavHost(navController = navController, startDestination = start) {
        composable(Screen.ServerSetup.route) {
            ServerSetupScreen(
                onDone = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ServerSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.ServerSetup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
