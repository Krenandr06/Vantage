package com.vantage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vantage.data.VantagePrefs
import com.vantage.ui.navigation.Routes
import com.vantage.ui.screen.*
import com.vantage.ui.theme.VantageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = VantagePrefs(this)

        setContent {
            VantageTheme {
                val navController = rememberNavController()
                val startDest = if (prefs.onboardingComplete) Routes.HOME else Routes.SPLASH

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    enterTransition = { fadeIn(tween(360)) + slideInHorizontally { it / 4 } },
                    exitTransition = { fadeOut(tween(220)) },
                    popEnterTransition = { fadeIn(tween(360)) + slideInHorizontally { -it / 4 } },
                    popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally { it / 4 } },
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }

                    composable(Routes.WELCOME) {
                        WelcomeScreen {
                            navController.navigate(Routes.LOCATION)
                        }
                    }

                    composable(Routes.LOCATION) {
                        LocationScreen(
                            onAllow = {
                                prefs.locationEnabled = true
                                prefs.onboardingComplete = true
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                            onSkip = {
                                prefs.onboardingComplete = true
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.HOME) {
                        HomeScreen(
                            onScenePicker = { navController.navigate(Routes.SCENE_PICKER) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }

                    composable(Routes.SCENE_PICKER) {
                        ScenePickerScreen(
                            onBack = { navController.popBackStack() },
                            onSceneSelected = { sceneId ->
                                navController.navigate(Routes.preview(sceneId))
                            },
                        )
                    }

                    composable(
                        Routes.PREVIEW,
                        arguments = listOf(navArgument("sceneId") { type = NavType.StringType }),
                    ) { entry ->
                        val sceneId = entry.arguments?.getString("sceneId") ?: "forest"
                        var showCustomize by remember { mutableStateOf(false) }

                        PreviewScreen(
                            sceneId = sceneId,
                            onBack = { navController.popBackStack() },
                            onCustomize = { showCustomize = true },
                        )

                        if (showCustomize) {
                            CustomizeSheet(onDismiss = { showCustomize = false })
                        }
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
