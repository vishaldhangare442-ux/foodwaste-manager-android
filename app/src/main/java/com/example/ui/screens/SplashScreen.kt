package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated Flash / Splash Screen for FoodWaste Rescue.
 * Displays brand logo, ambient aura glow, animated launch checklist,
 * progress indicator, and transition to the main app or authentication.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isReadyToEnter by remember { mutableStateOf(false) }

    val steps = listOf(
        "🌱 Calibrating Smart Pantry & Expiry Radar...",
        "🏢 Connecting Verified NGO Shelters & Food Banks...",
        "📍 Loading Real-Time Surplus Food Drop-offs...",
        "✨ Ready to Rescue Surplus Food!"
    )

    // Smooth progressive loading animation
    LaunchedEffect(Unit) {
        // Step 0
        progress = 0.25f
        currentStepIndex = 0
        delay(600)

        // Step 1
        progress = 0.55f
        currentStepIndex = 1
        delay(650)

        // Step 2
        progress = 0.85f
        currentStepIndex = 2
        delay(600)

        // Step 3: Complete
        progress = 1.0f
        currentStepIndex = 3
        isReadyToEnter = true
        delay(700)

        // Auto navigate
        onFinish()
    }

    // Infinite breathing animation for the central glow & emblem
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    // Animated progress state
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    // Deep eco-friendly luxury gradient background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF062E1E), // Deep Forest Night
            Color(0xFF0A3D27), // Lush Pine
            Color(0xFF0F4B32), // Emerald Dark
            Color(0xFF062115)  // Deep Basalt
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("flash_splash_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow tapping anywhere to bypass or proceed immediately
                onFinish()
            }
    ) {
        // Atmospheric floating light orbs drawn via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left emerald orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22C55E).copy(alpha = auraAlpha * 0.45f), Color.Transparent),
                    center = Offset(width * 0.2f, height * 0.15f),
                    radius = width * 0.55f
                ),
                center = Offset(width * 0.2f, height * 0.15f),
                radius = width * 0.55f
            )

            // Center-right golden amber glow orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = auraAlpha * 0.30f), Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.5f),
                    radius = width * 0.5f
                ),
                center = Offset(width * 0.85f, height * 0.5f),
                radius = width * 0.5f
            )

            // Bottom cyan eco orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = auraAlpha * 0.35f), Color.Transparent),
                    center = Offset(width * 0.3f, height * 0.85f),
                    radius = width * 0.6f
                ),
                center = Offset(width * 0.3f, height * 0.85f),
                radius = width * 0.6f
            )
        }

        // Top Header Bar: Skip / Enter button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Version Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80))
                    )
                    Text(
                        text = "LIVE NETWORK v2.4",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Quick Skip Button
            TextButton(
                onClick = onFinish,
                modifier = Modifier.testTag("btn_splash_skip")
            ) {
                Text(
                    text = "Skip >",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Central Pulsing Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
            ) {
                // Outer Glow Ring
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.size(180.dp)
                ) {}

                // Middle Translucent Ring with Accent Border
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F3D2A).copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                    modifier = Modifier.size(145.dp)
                ) {}

                // Core Solid Icon Badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF059669),
                    shadowElevation = 14.dp,
                    modifier = Modifier.size(105.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "FoodWaste Rescue Logo",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Decorative Satellite Floating Badges
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Diversity3,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title & Branding
            Text(
                text = "FoodWaste Rescue",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Zero-Waste Pantry • NGO Rescue • Community Sharing",
                color = Color(0xFFA7F3D0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Value Prop Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashFeaturePill(icon = Icons.Default.Eco, label = "Zero Waste")
                SplashFeaturePill(icon = Icons.Default.Diversity3, label = "NGO Network")
                SplashFeaturePill(icon = Icons.Default.Fastfood, label = "Surplus Meals")
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Progress & Status Section
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = steps.getOrElse(currentStepIndex) { "Loading..." },
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            color = Color(0xFF6EE7B7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Linear Progress Bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive "Get Started" CTA Button
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_splash_continue")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isReadyToEnter) "Enter Food Rescue App" else "Get Started Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Footer
        Text(
            text = "Every meal rescued feeds a neighbor and protects our planet.",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp)
        )
    }
}

@Composable
private fun SplashFeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6EE7B7),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
