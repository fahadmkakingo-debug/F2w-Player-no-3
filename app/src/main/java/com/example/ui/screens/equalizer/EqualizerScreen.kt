package com.example.ui.screens.equalizer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.EqualizerManager
import kotlin.math.roundToInt

private val AccentGreen = Color(0xFF22C55E)
private val UnselectedPillBg = Color(0xFF262626)
private val TrackInactiveColor = Color(0xFF383838)
private val BgColor = Color(0xFF000000)

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eqManager = remember { EqualizerManager.getInstance(context) }

    val isEnabled by eqManager.isEnabled.collectAsState()
    val currentPreset by eqManager.currentPreset.collectAsState()
    val bandLevels by eqManager.bandLevels.collectAsState()
    val currentReverb by eqManager.currentReverb.collectAsState()
    val bassBoostStrength by eqManager.bassBoostStrength.collectAsState()
    val virtualizerStrength by eqManager.virtualizerStrength.collectAsState()

    var showNewPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("equalizer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Bar: [<- Back] "Equalizer" [Green Toggle Switch]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("eq_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Equalizer",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Green master toggle switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { eqManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentGreen,
                        uncheckedThumbColor = Color(0xFF9E9E9E),
                        uncheckedTrackColor = Color(0xFF383838),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("eq_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Container for all controls that dim when disabled
            val controlsAlpha = if (isEnabled) 1.0f else 0.38f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(controlsAlpha)
            ) {
                // 2. Preset Section Header: "Preset" and "+" Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Preset",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = {
                            if (isEnabled) {
                                newPresetName = "My Preset"
                                showNewPresetDialog = true
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Preset",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Preset Pills (Horizontal Scroll)
                val presetScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(presetScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EqualizerManager.PRESET_NAMES.forEach { presetName ->
                        val isSelected = presetName.equals(currentPreset, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) AccentGreen else UnselectedPillBg)
                                .clickable(enabled = isEnabled) {
                                    eqManager.setPreset(presetName)
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = presetName,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. 5-Band Vertical Equalizer Section
                // Frequencies: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EqualizerManager.FREQUENCIES.forEachIndexed { index, freqLabel ->
                        val levelDb = bandLevels.getOrElse(index) { 0 }
                        VerticalEqualizerBandColumn(
                            frequencyLabel = freqLabel,
                            levelDb = levelDb,
                            enabled = isEnabled,
                            onLevelChange = { newDb ->
                                eqManager.setBandLevel(index, newDb)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 4. Reverb Section Header & Scrollable Pills
                Text(
                    text = "Reverb",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                val reverbScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(reverbScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EqualizerManager.REVERB_PRESETS.forEach { reverbName ->
                        val isSelected = reverbName.equals(currentReverb, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) AccentGreen else UnselectedPillBg)
                                .clickable(enabled = isEnabled) {
                                    eqManager.setReverb(reverbName)
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reverbName,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 5. Bass Boost Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Bass Boost",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = bassBoostStrength.toFloat(),
                        onValueChange = { eqManager.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = AccentGreen,
                            inactiveTrackColor = TrackInactiveColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("eq_bass_boost_slider")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Virtualizer Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Virtualizer",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = virtualizerStrength.toFloat(),
                        onValueChange = { eqManager.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = AccentGreen,
                            inactiveTrackColor = TrackInactiveColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("eq_virtualizer_slider")
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Save Custom Preset Dialog
    if (showNewPresetDialog) {
        AlertDialog(
            onDismissRequest = { showNewPresetDialog = false },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Save Custom Preset",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Hifadhi mipangilio ya sasa ya Equalizer kama preset maalum:",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Color(0xFF555555),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            eqManager.setPreset("Custom")
                            Toast.makeText(context, "Preset \"$newPresetName\" imehifadhiwa!", Toast.LENGTH_SHORT).show()
                            showNewPresetDialog = false
                        }
                    }
                ) {
                    Text("Hifadhi", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPresetDialog = false }) {
                    Text("Ghairi", color = Color(0xFF9E9E9E))
                }
            }
        )
    }
}

/**
 * Vertical Equalizer Slider Column for a single frequency band.
 * Range: -15 dB (bottom) to +15 dB (top).
 */
@Composable
private fun VerticalEqualizerBandColumn(
    frequencyLabel: String,
    levelDb: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val trackHeightDp = 220.dp
    val thumbDiameterDp = 22.dp

    val dbText = when {
        levelDb > 0 -> "+${levelDb}dB"
        else -> "${levelDb}dB"
    }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top dB level label
        Text(
            text = dbText,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.height(20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Vertical Track with Thumb
        BoxWithConstraints(
            modifier = Modifier
                .width(44.dp)
                .height(trackHeightDp),
            contentAlignment = Alignment.Center
        ) {
            val totalHeightPx = with(density) { (trackHeightDp - thumbDiameterDp).toPx() }
            val thumbRadiusPx = with(density) { (thumbDiameterDp / 2).toPx() }

            // Fraction from bottom (0f = -15dB, 0.5f = 0dB, 1f = +15dB)
            val fraction = ((levelDb + 15) / 30f).coerceIn(0f, 1f)

            // Y offset in px from top to center of thumb
            // When fraction == 1 (top): y = 0
            // When fraction == 0 (bottom): y = totalHeightPx
            val currentThumbY = totalHeightPx * (1f - fraction)

            fun calculateLevelFromY(yPx: Float) {
                if (!enabled) return
                val clampedY = yPx.coerceIn(0f, totalHeightPx)
                val newFraction = 1f - (clampedY / totalHeightPx)
                val calculatedDb = ((newFraction * 30f) - 15f).roundToInt().coerceIn(-15, 15)
                onLevelChange(calculatedDb)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        if (enabled) {
                            detectTapGestures { offset ->
                                calculateLevelFromY(offset.y - thumbRadiusPx)
                            }
                        }
                    }
                    .pointerInput(enabled) {
                        if (enabled) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                calculateLevelFromY(change.position.y - thumbRadiusPx)
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Inactive upper track (grey)
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(with(density) { (currentThumbY + thumbRadiusPx).toDp() })
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(TrackInactiveColor)
                        .align(Alignment.TopCenter)
                )

                // Active lower track (bright green)
                val activeHeightPx = totalHeightPx - currentThumbY + thumbRadiusPx
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(with(density) { activeHeightPx.toDp() })
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(AccentGreen)
                        .align(Alignment.BottomCenter)
                )

                // White circular thumb with shadow
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, currentThumbY.roundToInt()) }
                        .size(thumbDiameterDp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Frequency Label (e.g. "60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        Text(
            text = frequencyLabel,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
