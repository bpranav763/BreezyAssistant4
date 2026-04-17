package com.breezy.assistant

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class SystemControls(private val context: Context) {

  fun setDND(enable: Boolean): String {
      val nm = context.getSystemService(NotificationManager::class.java)
      return if (nm.isNotificationPolicyAccessGranted) {
          nm.setInterruptionFilter(
              if (enable) NotificationManager.INTERRUPTION_FILTER_NONE
              else NotificationManager.INTERRUPTION_FILTER_ALL
          )
          if (enable) "Do Not Disturb is on. Peace and quiet."
          else "DND off. You're reachable again."
      } else {
          val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(intent)
          "I need permission for DND. Opening settings — enable Breezy then try again."
      }
  }

  fun setVolume(percent: Int): String {
      val audio = context.getSystemService(AudioManager::class.java)
      val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
      val target = (max * percent / 100f).toInt().coerceIn(0, max)
      audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
      return "Volume set to $percent%."
  }

  fun openWifiSettings(): String {
      val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
      return "Opening WiFi settings for you."
  }

  fun openBluetoothSettings(): String {
      val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
      return "Opening Bluetooth settings."
  }

  fun setBrightness(percent: Int): String {
      return if (Settings.System.canWrite(context)) {
          val value = (255 * percent / 100f).toInt().coerceIn(0, 255)
          Settings.System.putInt(
              context.contentResolver,
              Settings.System.SCREEN_BRIGHTNESS, value
          )
          "Brightness set to $percent%."
      } else {
          val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(intent)
          "Need permission to change brightness. Tap Breezy in settings and allow."
      }
  }
}
