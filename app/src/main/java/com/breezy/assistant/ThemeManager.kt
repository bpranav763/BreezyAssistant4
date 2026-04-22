package com.breezy.assistant

import android.content.Context
import android.graphics.Color
import androidx.core.view.WindowCompat

object ThemeManager {

    enum class Theme { LIGHT, DARK, MIDNIGHT, OIL_PASTELS, DEEP_SPACE, FOREST, SUNSET, PURE_BLACK }

    fun getBackgroundColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.LIGHT -> 0xFFF8F9FA.toInt()
            Theme.DARK -> 0xFF1C1C1E.toInt()
            Theme.OIL_PASTELS -> 0xFFFDF6E3.toInt()  // soft cream
            Theme.MIDNIGHT -> 0xFF0A0F1E.toInt()
            Theme.DEEP_SPACE -> 0xFF0B0C10.toInt()
            Theme.FOREST -> 0xFF0A1C16.toInt()
            Theme.SUNSET -> 0xFF1A0E0B.toInt()
            Theme.PURE_BLACK -> 0xFF000000.toInt()
        }
    }

    fun getCardColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.LIGHT -> 0xFFFFFFFF.toInt()
            Theme.DARK -> 0xFF2C2C2E.toInt()
            Theme.OIL_PASTELS -> 0xFFFFF0D4.toInt()  // soft peach
            Theme.MIDNIGHT -> 0xFF111827.toInt()
            Theme.DEEP_SPACE -> 0xFF1F2833.toInt()
            Theme.FOREST -> 0xFF122E26.toInt()
            Theme.SUNSET -> 0xFF2A1916.toInt()
            Theme.PURE_BLACK -> 0xFF111111.toInt()
        }
    }

    fun getAccentColor(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.LIGHT, Theme.DARK -> 0xFF1D4ED8.toInt()
            Theme.OIL_PASTELS -> 0xFFE07A5F.toInt()  // terracotta
            Theme.MIDNIGHT -> 0xFF1D4ED8.toInt()
            Theme.DEEP_SPACE -> 0xFF66FCF1.toInt()
            Theme.FOREST -> 0xFF34D399.toInt()
            Theme.SUNSET -> 0xFFF87171.toInt()
            Theme.PURE_BLACK -> 0xFF3B82F6.toInt()
        }
    }

    fun getTextPrimary(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.LIGHT -> 0xFF212529.toInt()
            Theme.DARK -> Color.WHITE
            Theme.OIL_PASTELS -> 0xFF3D405B.toInt()  // dark slate
            else -> Color.WHITE
        }
    }

    fun getTextSecondary(context: Context): Int {
        return when (getCurrentTheme(context)) {
            Theme.LIGHT -> 0xFF6C757D.toInt()
            Theme.DARK -> 0xFF9CA3AF.toInt()
            Theme.OIL_PASTELS -> 0xFF7A6C5D.toInt()
            else -> 0xFF9CA3AF.toInt()
        }
    }

    fun getCornerRadius(context: Context): Float {
        return when (getCurrentTheme(context)) {
            Theme.OIL_PASTELS -> 24f  // much softer
            else -> 16f
        }
    }

    fun getCurrentTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("theme", "MIDNIGHT") ?: "MIDNIGHT"
        return Theme.valueOf(name)
    }

    fun applyTheme(activity: BaseActivity, theme: Theme) {
        activity.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
            .edit().putString("theme", theme.name).apply()
        activity.recreate()
    }

    fun updateSystemBars(activity: android.app.Activity) {
        val isLight = getCurrentTheme(activity) == Theme.OIL_PASTELS
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}
