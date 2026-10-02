package com.example.ui.screens.welcome

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCyanPrimary
import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType

/**
 * First-Launch Permission Onboarding Screen.
 * Uses Image 2's organic fluid blue/indigo gradient background with floating spheres,
 * combined with Image 1's permission explanation details.
 * When the user taps "Endelea na Toa Ruhusa", a dark shadow mask dims the background
 * while presenting the native Android system permission prompt.
 */
@Composable
fun FirstLaunchPermissionScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRequestingPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isRequestingPermission = false
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("first_launch_permission_screen")
    ) {
        // 1. Organic Fluid Blue Gradient Background (Image 2 style)
        AbstractFluidBlueBackground(modifier = Modifier.fillMaxSize())

        // 2. Main Content Layout (Image 1 details)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .graphicsLayer {
                    // Dim/Shadow content when permission prompt is active
                    if (isRequestingPermission) {
                        alpha = 0.25f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glowing Blue Media Icon Orb
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = CircleShape,
                            spotColor = F2WCyanPrimary,
                            ambientColor = F2WCyanPrimary
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF0077B6),
                                    Color(0xFF03045E)
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PermMedia,
                        contentDescription = "Media Permission",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Main Title
                Text(
                    text = "Ruhusa ya Kusoma Media",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle Description
                Text(
                    text = "F2W Player inahitaji ruhusa ya kusoma mafaili ya video na audio kwenye simu yako ili uweze kutazama na kusikiliza kwa ubora wa juu.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 2 Feature Cards explaining speed & safety
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionFeatureCard(
                        icon = Icons.Filled.PlayCircleFilled,
                        title = "Uchezaji wa Haraka wa Video",
                        description = "Inasoma format zote za video (MP4, MKV, AVI, WebM) moja kwa moja."
                    )

                    PermissionFeatureCard(
                        icon = Icons.Filled.Lock,
                        title = "Faragha na Usalama 100%",
                        description = "Mafaili yako yanabaki kwenye simu yako pekee na hayatumwi popote."
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Cyan Action Button: Endelea na Toa Ruhusa
                Button(
                    onClick = {
                        isRequestingPermission = true
                        val required = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.ALL_MEDIA)
                        permissionLauncher.launch(required)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = F2WCyanPrimary),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = F2WCyanPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Endelea na Toa Ruhusa",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Skip / Go to Home Button
                Text(
                    text = "Nitaweka Baadaye (Nenda Home)",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
                            onClick = onComplete
                        )
                        .padding(8.dp)
                )
            }
        }

        // 3. Kivuli / Dark Dim Shadow Mask over background when requesting permissions
        AnimatedVisibility(
            visible = isRequestingPermission,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = F2WCyanPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Inaomba ruhusa ya simu...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AbstractFluidBlueBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Deep Indigo/Blue Base Gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E1B4B),
                    Color(0xFF0B132B),
                    Color(0xFF030712)
                )
            )
        )

        // Large Organic Fluid Waves (Image 2 style)
        val path1 = Path().apply {
            moveTo(0f, h * 0.15f)
            cubicTo(w * 0.35f, h * 0.05f, w * 0.75f, h * 0.35f, w, h * 0.22f)
            lineTo(w, h * 0.72f)
            cubicTo(w * 0.6f, h * 0.88f, w * 0.2f, h * 0.68f, 0f, h * 0.80f)
            close()
        }
        drawPath(
            path = path1,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E3A8A).copy(alpha = 0.65f),
                    Color(0xFF2563EB).copy(alpha = 0.50f),
                    Color(0xFF1D4ED8).copy(alpha = 0.70f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        val path2 = Path().apply {
            moveTo(w, h * 0.08f)
            cubicTo(w * 0.8f, h * 0.26f, w * 0.4f, h * 0.12f, 0f, h * 0.40f)
            lineTo(0f, 0f)
            lineTo(w, 0f)
            close()
        }
        drawPath(
            path = path2,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3B82F6).copy(alpha = 0.45f),
                    Color(0xFF1E40AF).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(w * 0.8f, h * 0.1f),
                radius = w * 0.85f
            )
        )

        // Floating Gradient Spheres / Orbs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.60f),
                    Color(0xFF1E3A8A).copy(alpha = 0.20f),
                    Color.Transparent
                )
            ),
            radius = w * 0.32f,
            center = Offset(w * 0.18f, h * 0.14f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF60A5FA).copy(alpha = 0.50f),
                    Color(0xFF2563EB).copy(alpha = 0.18f),
                    Color.Transparent
                )
            ),
            radius = w * 0.40f,
            center = Offset(w * 0.75f, h * 0.44f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1D4ED8).copy(alpha = 0.65f),
                    Color(0xFF0F172A).copy(alpha = 0.25f),
                    Color.Transparent
                )
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.86f)
        )
    }
}

@Composable
private fun PermissionFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.80f))
            .border(0.8.dp, Color(0xFF2563EB).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(F2WCyanPrimary.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = F2WCyanPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
