package com.breezy.assistant

import android.content.Context
import android.graphics.Color
import androidx.core.view.WindowCompat

object ThemeManager {

    enum class Theme { MIDNIGHT_BLUE, DEEP_BLUE, SOFT_BLUE, OIL_PASTELS_BLUE }

    fun getBackgroundColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE -> 0xFF0A1628.toInt()      // Deep navy
            Theme.DEEP_BLUE -> 0xFF0B1A30.toInt()          // Oceanic dark
            Theme.SOFT_BLUE -> 0xFFF0F5FF.toInt()          // Very light blue-white
            Theme.OIL_PASTELS_BLUE -> 0xFFE8F0FE.toInt()   // Soft pastel blue
        }
    }

    fun getCardColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE -> 0xFF111F3D.toInt()
            Theme.DEEP_BLUE -> 0xFF132442.toInt()
            Theme.SOFT_BLUE -> 0xFFFFFFFF.toInt()
            Theme.OIL_PASTELS_BLUE -> 0xFFF5F8FF.toInt()
        }
    }

    fun getAccentColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE -> 0xFF1D4ED8.toInt()      // Classic Breezy blue
            Theme.DEEP_BLUE -> 0xFF2563EB.toInt()          // Brighter blue
            Theme.SOFT_BLUE -> 0xFF3B82F6.toInt()          // Soft blue
            Theme.OIL_PASTELS_BLUE -> 0xFF4F46E5.toInt()   // Indigo-blue
        }
    }

    fun getTextPrimary(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE, Theme.DEEP_BLUE -> 0xFFFFFFFF.toInt()
            Theme.SOFT_BLUE, Theme.OIL_PASTELS_BLUE -> 0xFF1E293B.toInt()
        }
    }

    fun getTextSecondary(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE, Theme.DEEP_BLUE -> 0xFFB0C4DE.toInt()
            Theme.SOFT_BLUE, Theme.OIL_PASTELS_BLUE -> 0xFF475569.toInt()
        }
    }

    fun getDividerColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.MIDNIGHT_BLUE, Theme.DEEP_BLUE -> 0xFF1A2F5E.toInt()
            Theme.SOFT_BLUE, Theme.OIL_PASTELS_BLUE -> 0xFFCBD5E1.toInt()
        }
    }

    fun getBubbleColor(context: Context): Int = getAccentColor(context)

    fun getCrisisColor(context: Context): Int = 0xFF1E3A8A.toInt() // Deep blue

    fun getWarningColor(context: Context): Int = 0xFF3B82F6.toInt() // Light blue

    fun getCornerRadius(context: Context): Float {
        return when (getCurrentTheme(context)) {
            Theme.OIL_PASTELS_BLUE -> 24f
            else -> 16f
        }
    }

    fun getCurrentTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("theme", "MIDNIGHT_BLUE") ?: "MIDNIGHT_BLUE"
        return try {
            Theme.valueOf(name)
        } catch (e: Exception) {
            Theme.MIDNIGHT_BLUE
        }
    }

    fun applyTheme(activity: BaseActivity, theme: Theme) {
        activity.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
            .edit().putString("theme", theme.name).apply()
        activity.recreate()
    }

    fun updateSystemBars(activity: android.app.Activity) {
        val isLight = getCurrentTheme(activity) == Theme.SOFT_BLUE || getCurrentTheme(activity) == Theme.OIL_PASTELS_BLUE
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}
