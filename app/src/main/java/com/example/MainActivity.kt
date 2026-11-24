package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FintekApp
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.theme.FintekTheme
import com.example.ui.viewmodel.FintekViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FintekViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            FintekTheme(
                themeMode = themeMode,
                forceDarkSystemBars = showSplash
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = !showSplash,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        )
                    ) {
                        FintekApp(viewModel = viewModel)
                    }

                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        ) + scaleOut(
                            targetScale = 1.06f,
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        )
                    ) {
                        AnimatedSplashScreen(
                            isDataReady = !uiState.isLoading,
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }
}
