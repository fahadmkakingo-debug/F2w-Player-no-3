package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences

class VideoSettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("f2w_video_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SHOW_STATUS_BAR = "show_status_bar"
        private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
        private const val KEY_RESUME_PLAYBACK = "resume_playback"
        private const val KEY_AUTO_MINIPLAYER = "auto_miniplayer"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_GESTURE_CONTROL = "gesture_control"
        private const val KEY_SEEK_INTERVAL = "seek_interval"
        private const val KEY_DOUBLE_TAP_SEEK = "double_tap_seek"
        private const val KEY_LONG_PRESS_SPEED_UP = "long_press_speed_up"
        private const val KEY_LONG_PRESS_SPEED = "long_press_speed"
        private const val KEY_LONG_PRESS_VIBRATION = "long_press_vibration"
        private const val KEY_TAP_RATIOS_DIRECTLY = "tap_ratios_directly"
        private const val KEY_REMEMBER_BG_PLAY = "remember_bg_play"
        private const val KEY_REMEMBER_RATIO = "remember_ratio"
        private const val KEY_REMEMBER_SPEED = "remember_speed"
        private const val KEY_REMEMBER_BRIGHTNESS = "remember_brightness"
    }

    var showStatusBarDuringPlayback: Boolean
        get() = prefs.getBoolean(KEY_SHOW_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_STATUS_BAR, value).apply()

    var screenOrientation: String
        get() = prefs.getString(KEY_SCREEN_ORIENTATION, "Sensor / Auto") ?: "Sensor / Auto"
        set(value) = prefs.edit().putString(KEY_SCREEN_ORIENTATION, value).apply()

    var resumePlayback: String
        get() = prefs.getString(KEY_RESUME_PLAYBACK, "Always") ?: "Always"
        set(value) = prefs.edit().putString(KEY_RESUME_PLAYBACK, value).apply()

    var autoMiniplayer: Boolean
        get() = prefs.getBoolean(KEY_AUTO_MINIPLAYER, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_MINIPLAYER, value).apply()

    var autoPlayNext: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, value).apply()

    var gestureControl: Boolean
        get() = prefs.getBoolean(KEY_GESTURE_CONTROL, true)
        set(value) = prefs.edit().putBoolean(KEY_GESTURE_CONTROL, value).apply()

    var seekIntervalSeconds: Int
        get() = prefs.getInt(KEY_SEEK_INTERVAL, 10)
        set(value) = prefs.edit().putInt(KEY_SEEK_INTERVAL, value).apply()

    var doubleTapToSeek: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_TAP_SEEK, true)
        set(value) = prefs.edit().putBoolean(KEY_DOUBLE_TAP_SEEK, value).apply()

    var longPressSpeedUp: Boolean
        get() = prefs.getBoolean(KEY_LONG_PRESS_SPEED_UP, true)
        set(value) = prefs.edit().putBoolean(KEY_LONG_PRESS_SPEED_UP, value).apply()

    var longPressSpeedMultiplier: Float
        get() = prefs.getFloat(KEY_LONG_PRESS_SPEED, 2.0f)
        set(value) = prefs.edit().putFloat(KEY_LONG_PRESS_SPEED, value).apply()

    var longPressVibration: Boolean
        get() = prefs.getBoolean(KEY_LONG_PRESS_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_LONG_PRESS_VIBRATION, value).apply()

    var tapRatiosToSwitchDirectly: Boolean
        get() = prefs.getBoolean(KEY_TAP_RATIOS_DIRECTLY, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_RATIOS_DIRECTLY, value).apply()

    var rememberBackgroundPlay: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_BG_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_BG_PLAY, value).apply()

    var rememberRatio: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_RATIO, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_RATIO, value).apply()

    var rememberSpeed: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_SPEED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_SPEED, value).apply()

    var rememberBrightness: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_BRIGHTNESS, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_BRIGHTNESS, value).apply()
}
