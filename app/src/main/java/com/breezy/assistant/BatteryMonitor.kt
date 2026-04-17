package com.breezy.assistant

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryMonitor(private val context: Context) {

  data class BatteryData(
      val level: Int,
      val temperature: Float,
      val isCharging: Boolean,
      val voltage: Int,
      val health: Int
  )

  fun getBatteryData(): BatteryData {
      val intent = context.registerReceiver(
          null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      )
      val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
      val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
      val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
      val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0

      return BatteryData(level, temp, isCharging, voltage, health)
  }

  fun getBatteryResponse(name: String = ""): String {
      val data = getBatteryData()
      val n = if (name.isNotEmpty()) " $name" else ""

      return when {
          data.isCharging && data.level >= 80 ->
              "Hey$n, battery's at ${data.level}% and still charging. Want me to cap it at 80% to protect your battery long term?"
          data.isCharging ->
              "Charging at ${data.level}%$n. Temperature is ${data.temperature}°C — looking good."
          data.level <= 15 ->
              "Battery's at ${data.level}%$n — getting low. Find a charger soon."
          data.level <= 30 ->
              "Down to ${data.level}%$n. Worth keeping a charger nearby."
          data.temperature > 42f ->
              "Your phone's running warm at ${data.temperature}°C$n. Close some apps to cool it down."
          else ->
              "Battery's at ${data.level}%$n, ${data.temperature}°C — all good."
      }
  }

  fun isChargerSafe(): Boolean {
      val data = getBatteryData()
      return data.voltage in 3500..5500 && data.temperature < 45f
  }
}
