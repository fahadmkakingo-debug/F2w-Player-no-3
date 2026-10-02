package com.example.data.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages audio equalization, bass boost, virtualizer, and reverb effects
 * applying them in real-time to active audio sessions (both video and music playback).
 */
class EqualizerManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences = appContext.getSharedPreferences("xplayer_equalizer_prefs", Context.MODE_PRIVATE)

    // State flows for UI observing
    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(prefs.getString(KEY_PRESET, "Custom") ?: "Custom")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    // 5 band levels in dB (-15 to +15)
    private val _bandLevels = MutableStateFlow(loadBandLevels())
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    // Reverb preset name
    private val _currentReverb = MutableStateFlow(prefs.getString(KEY_REVERB, "None") ?: "None")
    val currentReverb: StateFlow<String> = _currentReverb.asStateFlow()

    // Bass boost strength (0 to 1000)
    private val _bassBoostStrength = MutableStateFlow(prefs.getInt(KEY_BASS_BOOST, 0))
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    // Virtualizer strength (0 to 1000)
    private val _virtualizerStrength = MutableStateFlow(prefs.getInt(KEY_VIRTUALIZER, 0))
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    // Hardware Audio Effects
    private var activeSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    init {
        // Initialize with default global audio session 0
        bindAudioSession(0)
    }

    private fun loadBandLevels(): List<Int> {
        val b0 = prefs.getInt(KEY_BAND_0, 0)
        val b1 = prefs.getInt(KEY_BAND_1, 0)
        val b2 = prefs.getInt(KEY_BAND_2, 0)
        val b3 = prefs.getInt(KEY_BAND_3, 0)
        val b4 = prefs.getInt(KEY_BAND_4, 0)
        return listOf(b0, b1, b2, b3, b4)
    }

    @Synchronized
    fun bindAudioSession(sessionId: Int) {
        if (activeSessionId == sessionId && equalizer != null) {
            applyAllSettings()
            return
        }

        releaseEffects()
        activeSessionId = sessionId

        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Equalizer for session $sessionId: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _isEnabled.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize BassBoost for session $sessionId: ${e.message}")
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Virtualizer for session $sessionId: ${e.message}")
        }

        try {
            presetReverb = PresetReverb(0, sessionId).apply {
                enabled = _isEnabled.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize PresetReverb for session $sessionId: ${e.message}")
        }

        applyAllSettings()
    }

    @Synchronized
    fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) {}
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {}
        bassBoost = null

        try {
            virtualizer?.release()
        } catch (e: Exception) {}
        virtualizer = null

        try {
            presetReverb?.release()
        } catch (e: Exception) {}
        presetReverb = null
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {}
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {}
        try {
            virtualizer?.enabled = enabled
        } catch (e: Exception) {}
        try {
            presetReverb?.enabled = enabled
        } catch (e: Exception) {}

        if (enabled) {
            applyAllSettings()
        }
    }

    fun setPreset(presetName: String) {
        _currentPreset.value = presetName
        prefs.edit().putString(KEY_PRESET, presetName).apply()

        val presetBands = PRESETS[presetName]
        if (presetBands != null) {
            _bandLevels.value = presetBands
            saveBandLevels(presetBands)
            applyBandsToHardware()
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        if (bandIndex !in 0..4) return
        val clamped = levelDb.coerceIn(-15, 15)
        val updated = _bandLevels.value.toMutableList()
        updated[bandIndex] = clamped
        _bandLevels.value = updated
        _currentPreset.value = "Custom"

        prefs.edit().putString(KEY_PRESET, "Custom").apply()
        saveBandLevels(updated)
        applyBandsToHardware()
    }

    fun setReverb(reverbName: String) {
        _currentReverb.value = reverbName
        prefs.edit().putString(KEY_REVERB, reverbName).apply()
        applyReverbToHardware()
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        prefs.edit().putInt(KEY_BASS_BOOST, clamped).apply()
        applyBassBoostToHardware()
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        prefs.edit().putInt(KEY_VIRTUALIZER, clamped).apply()
        applyVirtualizerToHardware()
    }

    private fun saveBandLevels(bands: List<Int>) {
        prefs.edit()
            .putInt(KEY_BAND_0, bands.getOrElse(0) { 0 })
            .putInt(KEY_BAND_1, bands.getOrElse(1) { 0 })
            .putInt(KEY_BAND_2, bands.getOrElse(2) { 0 })
            .putInt(KEY_BAND_3, bands.getOrElse(3) { 0 })
            .putInt(KEY_BAND_4, bands.getOrElse(4) { 0 })
            .apply()
    }

    private fun applyAllSettings() {
        if (!_isEnabled.value) return
        applyBandsToHardware()
        applyBassBoostToHardware()
        applyVirtualizerToHardware()
        applyReverbToHardware()
    }

    private fun applyBandsToHardware() {
        val eq = equalizer ?: return
        if (!_isEnabled.value) return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minRange = eq.bandLevelRange[0] // in mB (typically -1500)
            val maxRange = eq.bandLevelRange[1] // in mB (typically +1500)

            for (i in 0 until minOf(numBands, 5)) {
                val db = _bandLevels.value.getOrElse(i) { 0 }
                // Convert dB to millibels (1 dB = 100 mB)
                val mB = (db * 100).toShort().coerceIn(minRange, maxRange)
                eq.setBandLevel(i.toShort(), mB)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying band levels to equalizer: ${e.message}")
        }
    }

    private fun applyBassBoostToHardware() {
        val bb = bassBoost ?: return
        if (!_isEnabled.value) return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(_bassBoostStrength.value.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying bass boost: ${e.message}")
        }
    }

    private fun applyVirtualizerToHardware() {
        val virt = virtualizer ?: return
        if (!_isEnabled.value) return
        try {
            if (virt.strengthSupported) {
                virt.setStrength(_virtualizerStrength.value.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying virtualizer: ${e.message}")
        }
    }

    private fun applyReverbToHardware() {
        val rev = presetReverb ?: return
        if (!_isEnabled.value) return
        try {
            val presetCode = when (_currentReverb.value) {
                "Small Room" -> PresetReverb.PRESET_SMALLROOM
                "Medium Room" -> PresetReverb.PRESET_MEDIUMROOM
                "Large Room" -> PresetReverb.PRESET_LARGEROOM
                "Medium Hall" -> PresetReverb.PRESET_MEDIUMHALL
                "Large Hall" -> PresetReverb.PRESET_LARGEHALL
                "Plate" -> PresetReverb.PRESET_PLATE
                else -> PresetReverb.PRESET_NONE
            }
            rev.preset = presetCode
        } catch (e: Exception) {
            Log.w(TAG, "Error applying reverb: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "EqualizerManager"

        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BAND_0 = "eq_band_0"
        private const val KEY_BAND_1 = "eq_band_1"
        private const val KEY_BAND_2 = "eq_band_2"
        private const val KEY_BAND_3 = "eq_band_3"
        private const val KEY_BAND_4 = "eq_band_4"
        private const val KEY_REVERB = "eq_reverb"
        private const val KEY_BASS_BOOST = "eq_bass_boost"
        private const val KEY_VIRTUALIZER = "eq_virtualizer"

        // 5 Frequencies: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
        val FREQUENCIES = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

        val PRESET_NAMES = listOf(
            "Custom",
            "Normal",
            "Classical",
            "Dance",
            "Flat",
            "Folk",
            "Heavy Metal",
            "Hip Hop",
            "Jazz",
            "Pop",
            "Rock",
            "Vocal"
        )

        val REVERB_PRESETS = listOf(
            "None",
            "Small Room",
            "Medium Room",
            "Large Room",
            "Medium Hall",
            "Large Hall",
            "Plate"
        )

        // Preset band gains in dB (-15 to +15)
        val PRESETS = mapOf(
            "Normal" to listOf(0, 0, 0, 0, 0),
            "Classical" to listOf(5, 3, -2, 4, 4),
            "Dance" to listOf(6, 0, 2, 4, 1),
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Folk" to listOf(3, 0, 0, 2, -1),
            "Heavy Metal" to listOf(4, 1, 9, 3, 0),
            "Hip Hop" to listOf(5, 3, 0, 1, 3),
            "Jazz" to listOf(4, 2, -2, 2, 5),
            "Pop" to listOf(-1, 2, 5, 1, -2),
            "Rock" to listOf(5, 3, -1, 3, 5),
            "Vocal" to listOf(-2, 1, 5, 2, -1)
        )

        @Volatile
        private var instance: EqualizerManager? = null

        fun getInstance(context: Context): EqualizerManager {
            return instance ?: synchronized(this) {
                instance ?: EqualizerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
