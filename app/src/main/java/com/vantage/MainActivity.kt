package com.vantage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                val startDest = if (prefs.onboardingComplete) Routes.GALLERY else Routes.SPLASH

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
                                navController.navigate(Routes.GALLERY) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                            onSkip = {
                                prefs.onboardingComplete = true
                                navController.navigate(Routes.GALLERY) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.GALLERY) {
                        GalleryScreen(
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
