package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Navy850
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(
    isDataReady: Boolean,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val strokeProgress = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.5f) }
    val glowAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.85f) }
    val ringAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(22f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val loaderProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Tahap 1: Emblem & cahaya Emerald muncul lembut
        launch {
            logoAlpha.animateTo(1f, tween(durationMillis = 380, easing = LinearOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            glowAlpha.animateTo(0.55f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        }
        launch {
            glowScale.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
        }

        // Tahap 2: Garis logo Fintek tergambar mulus
        delay(180)
        launch {
            strokeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }

        // Tahap 3: Judul & tagline muncul halus ke atas
        delay(160)
        launch {
            titleAlpha.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
        launch {
            titleOffsetY.animateTo(0f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }

        delay(150)
        launch {
            subtitleAlpha.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }

        // Gelombang cincin halus + progress bar bawah
        launch {
            ringAlpha.snapTo(0.45f)
            launch {
                ringScale.animateTo(1.75f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
            }
            ringAlpha.animateTo(0f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
        }
        loaderProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    // Tunggu minimal animasi selesai DAN data lokal siap
    LaunchedEffect(isDataReady) {
        if (isDataReady) {
            delay(1650)
            onSplashFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C182E), Navy950, Navy900)
                )
            )
            .testTag("animated_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial Emerald glow behind logo
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .scale(glowScale.value)
                .alpha(glowAlpha.value)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Emerald500.copy(alpha = 0.38f),
                        Emerald600.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // Expanding pulse ring
        Canvas(
            modifier = Modifier
                .size(116.dp)
                .scale(ringScale.value)
                .alpha(ringAlpha.value)
        ) {
            drawRoundRect(
                color = Emerald400,
                cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Animated Emblem Box
            Surface(
                modifier = Modifier
                    .size(104.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                shape = RoundedCornerShape(28.dp),
                color = Navy850,
                shadowElevation = 18.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF152440), Color(0xFF0B1426))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val w = size.width
                        val h = size.height
                        val progress = strokeProgress.value

                        // Outer wallet fold path
                        val walletPath = Path().apply {
                            moveTo(w * 0.62f, h * 0.34f)
                            lineTo(w * 0.28f, h * 0.34f)
                            cubicTo(
                                w * 0.16f, h * 0.34f,
                                w * 0.16f, h * 0.50f,
                                w * 0.28f, h * 0.50f
                            )
                            lineTo(w * 0.80f, h * 0.50f)
                            lineTo(w * 0.80f, h * 0.82f)
                            lineTo(w * 0.24f, h * 0.82f)
                            cubicTo(
                                w * 0.16f, h * 0.82f,
                                w * 0.16f, h * 0.74f,
                                w * 0.16f, h * 0.66f
                            )
                            lineTo(w * 0.16f, h * 0.42f)
                        }

                        val walletMeasure = PathMeasure()
                        walletMeasure.setPath(walletPath, false)
                        val animatedWalletPath = Path()
                        walletMeasure.getSegment(
                            0f,
                            walletMeasure.length * (progress * 1.15f).coerceIn(0f, 1f),
                            animatedWalletPath,
                            true
                        )

                        drawPath(
                            path = animatedWalletPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Emerald400, Emerald500)
                            ),
                            style = Stroke(
                                width = 5.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Upward growth arrow stem + head
                        val arrowProgress = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        if (arrowProgress > 0f) {
                            val arrowPath = Path().apply {
                                moveTo(w * 0.68f, h * 0.50f)
                                lineTo(w * 0.68f, h * 0.14f)
                                moveTo(w * 0.52f, h * 0.29f)
                                lineTo(w * 0.68f, h * 0.13f)
                                lineTo(w * 0.84f, h * 0.29f)
                            }
                            val arrowMeasure = PathMeasure()
                            arrowMeasure.setPath(arrowPath, false)
                            val animatedArrowPath = Path()
                            arrowMeasure.getSegment(
                                0f,
                                arrowMeasure.length * arrowProgress,
                                animatedArrowPath,
                                true
                            )
                            drawPath(
                                path = animatedArrowPath,
                                color = Emerald400,
                                style = Stroke(
                                    width = 5.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Subtle clasp pill inside wallet
                        if (progress > 0.65f) {
                            val claspAlpha = ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
                            drawRoundRect(
                                color = Emerald400.copy(alpha = claspAlpha),
                                topLeft = Offset(w * 0.64f, h * 0.62f),
                                size = Size(w * 0.16f, h * 0.09f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Wordmark
            Text(
                text = "Fintek",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .padding(top = titleOffsetY.value.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(subtitleAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Emerald400)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keuangan Pribadi • 100% Offline",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Bottom sleek animated loading bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp)
                .alpha(subtitleAlpha.value)
        ) {
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .width((124f * loaderProgress.value).dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Emerald600, Emerald400)
                            )
                        )
                )
            }
        }
    }
}
