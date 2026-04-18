package com.breezy.assistant

import android.app.ActivityManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

class HardwareInspector(private val context: Context) {

    data class CpuInfo(
        val hardware: String,
        val processor: String,
        val coreCount: Int,
        val abi: String,
        val maxFreqMHz: String,
        val currentFreqMHz: String,
        val governor: String
    )

    data class GpuInfo(
        val vendor: String,
        val renderer: String,
        val openGlVersion: String,
        val driverVersion: String
    )

    data class RamInfo(
        val totalMb: Long,
        val availableMb: Long,
        val usedPercent: Int,
        val isLowMemory: Boolean,
        val threshold: Long
    )

    data class StorageInfo(
        val totalGb: Float,
        val usedGb: Float,
        val freeGb: Float,
        val usedPercent: Int,
        val externalAvailable: Boolean
    )

    data class DisplayInfo(
        val widthPx: Int,
        val heightPx: Int,
        val densityDpi: Int,
        val refreshRateHz: Float,
        val densityClass: String
    )

    data class CameraInfo(
        val id: String,
        val facing: String,
        val megapixels: Float,
        val maxZoom: Float,
        val flashSupported: Boolean,
        val focalLengths: String
    )

    data class NetworkInfo(
        val wifiSsid: String,
        val wifiBssid: String,
        val wifiFrequencyMHz: Int,
        val wifiSignalDbm: Int,
        val wifiLinkSpeedMbps: Int,
        val ipAddress: String,
        val carrier: String,
        val networkType: String
    )

    data class SensorSummary(
        val count: Int,
        val names: List<String>
    )

    // ── CPU ─────────────────────────────────────────────────────────────────

    fun getCpuInfo(): CpuInfo {
        val cpuInfo = parseProcCpuInfo()
        return CpuInfo(
            hardware  = cpuInfo["Hardware"] ?: cpuInfo["model name"] ?: Build.HARDWARE,
            processor = cpuInfo["Processor"] ?: cpuInfo["CPU part"] ?: Build.BOARD,
            coreCount = Runtime.getRuntime().availableProcessors(),
            abi       = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            maxFreqMHz   = readCpuFreq("cpuinfo_max_freq"),
            currentFreqMHz = readCpuFreq("scaling_cur_freq"),
            governor  = readCpuGovernor()
        )
    }

    private fun parseProcCpuInfo(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            File("/proc/cpuinfo").forEachLine { line ->
                if (line.contains(":")) {
                    val split = line.split(":", limit = 2)
                    if (split.size == 2) {
                        val k = split[0].trim()
                        val v = split[1].trim()
                        if (k.isNotEmpty() && !result.containsKey(k)) result[k] = v
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun readCpuFreq(fileName: String): String {
        return try {
            val hz = File("/sys/devices/system/cpu/cpu0/cpufreq/$fileName")
                .readText().trim().toLongOrNull() ?: return "N/A"
            "${hz / 1000}MHz"
        } catch (_: Exception) { "N/A" }
    }

    private fun readCpuGovernor(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                .readText().trim()
        } catch (_: Exception) { "N/A" }
    }

    // ── GPU ─────────────────────────────────────────────────────────────────

    fun getGpuInfo(): GpuInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val glVersion = am.deviceConfigurationInfo.glEsVersion
        return GpuInfo(
            vendor      = readSysProp("ro.hardware.egl").ifEmpty { readSysProp("ro.hardware") },
            renderer    = readSysProp("ro.gfx.driver.0").ifEmpty {
                            readSysProp("ro.board.platform").ifEmpty { "Unknown" }
                          },
            openGlVersion = "OpenGL ES $glVersion",
            driverVersion = readSysProp("ro.opengles.version").ifEmpty { "N/A" }
        )
    }

    private fun readSysProp(key: String): String {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val method = cls.getMethod("get", String::class.java, String::class.java)
            (method.invoke(null, key, "") as? String)?.trim() ?: ""
        } catch (_: Exception) { "" }
    }

    // ── RAM ─────────────────────────────────────────────────────────────────

    fun getRamInfo(): RamInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalMb = mi.totalMem / (1024 * 1024)
        val availMb = mi.availMem / (1024 * 1024)
        val usedMb  = totalMb - availMb
        return RamInfo(
            totalMb     = totalMb,
            availableMb = availMb,
            usedPercent = (usedMb * 100 / totalMb.coerceAtLeast(1)).toInt(),
            isLowMemory = mi.lowMemory,
            threshold   = mi.threshold / (1024 * 1024)
        )
    }

    // ── Storage ─────────────────────────────────────────────────────────────

    fun getStorageInfo(): StorageInfo {
        val stat  = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes / (1024f * 1024f * 1024f)
        val free  = stat.availableBytes / (1024f * 1024f * 1024f)
        val used  = total - free
        val extAvail = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        return StorageInfo(
            totalGb = total, usedGb = used, freeGb = free,
            usedPercent = ((used / total.coerceAtLeast(0.01f)) * 100).toInt(),
            externalAvailable = extAvail
        )
    }

    // ── Display ─────────────────────────────────────────────────────────────

    fun getDisplayInfo(): DisplayInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            context.display?.refreshRate ?: dm.xdpi
        else @Suppress("DEPRECATION") wm.defaultDisplay.refreshRate
        val densityClass = when {
            dm.densityDpi >= 560 -> "xxxhdpi"
            dm.densityDpi >= 480 -> "xxhdpi"
            dm.densityDpi >= 320 -> "xhdpi"
            dm.densityDpi >= 240 -> "hdpi"
            else -> "mdpi"
        }
        return DisplayInfo(dm.widthPixels, dm.heightPixels, dm.densityDpi,
            refreshRate, densityClass)
    }

    // ── Cameras ─────────────────────────────────────────────────────────────

    fun getCameraInfo(): List<CameraInfo> {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cm.cameraIdList.mapNotNull { id ->
            try {
                val c = cm.getCameraCharacteristics(id)
                val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_BACK  -> "Back"
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    else -> "External"
                }
                val streamMap = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val maxSize = streamMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                    ?.maxByOrNull { it.width.toLong() * it.height }
                val mp = if (maxSize != null)
                    maxSize.width.toLong() * maxSize.height / 1_000_000f else 0f
                val zoomRatio = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
                val flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val focalLens = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.joinToString(", ") { "${String.format("%.1f", it)}mm" } ?: "N/A"
                CameraInfo(id, facing, mp, zoomRatio, flash, focalLens)
            } catch (_: Exception) { null }
        }
    }

    // ── Network ─────────────────────────────────────────────────────────────

    fun getNetworkInfo(): NetworkInfo {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wi = wm.connectionInfo
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val rawIp = wi.ipAddress
        val ip = "%d.%d.%d.%d".format(
            rawIp and 0xff, rawIp shr 8 and 0xff,
            rawIp shr 16 and 0xff, rawIp shr 24 and 0xff
        )
        return NetworkInfo(
            wifiSsid       = wi.ssid?.trim('\u0022') ?: "Not connected",
            wifiBssid      = wi.bssid ?: "N/A",
            wifiFrequencyMHz = wi.frequency,
            wifiSignalDbm  = wi.rssi,
            wifiLinkSpeedMbps = wi.linkSpeed,
            ipAddress      = if (rawIp != 0) ip else "N/A",
            carrier        = tm.networkOperatorName.ifEmpty { "N/A" },
            networkType    = getNetworkTypeName(tm)
        )
    }

    private fun getNetworkTypeName(tm: TelephonyManager): String {
        return try {
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR     -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE    -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA   -> "3G HSPA"
                TelephonyManager.NETWORK_TYPE_UMTS   -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS   -> "2G"
                else -> "Unknown"
            }
        } catch (_: Exception) { "N/A" }
    }

    // ── Sensors ─────────────────────────────────────────────────────────────

    fun getSensorInfo(): SensorSummary {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        return SensorSummary(
            count = sensors.size,
            names = sensors.map { "${it.name} (${sensorTypeName(it.type)})" }.distinct().take(30)
        )
    }

    private fun sensorTypeName(type: Int) = when (type) {
        Sensor.TYPE_ACCELEROMETER         -> "Accelerometer"
        Sensor.TYPE_GYROSCOPE             -> "Gyroscope"
        Sensor.TYPE_MAGNETIC_FIELD        -> "Magnetometer"
        Sensor.TYPE_LIGHT                 -> "Light"
        Sensor.TYPE_PROXIMITY             -> "Proximity"
        Sensor.TYPE_PRESSURE              -> "Barometer"
        Sensor.TYPE_GRAVITY               -> "Gravity"
        Sensor.TYPE_LINEAR_ACCELERATION   -> "Linear Accel"
        Sensor.TYPE_ROTATION_VECTOR       -> "Rotation Vector"
        Sensor.TYPE_AMBIENT_TEMPERATURE   -> "Ambient Temp"
        Sensor.TYPE_STEP_COUNTER          -> "Step Counter"
        Sensor.TYPE_STEP_DETECTOR         -> "Step Detector"
        Sensor.TYPE_HEART_RATE            -> "Heart Rate"
        else -> "Type $type"
    }

    // ── Device summary (for chat responses) ─────────────────────────────────

    fun getDeviceSummary(): String {
        val cpu = getCpuInfo()
        val ram = getRamInfo()
        val storage = getStorageInfo()
        val display = getDisplayInfo()
        return buildString {
            appendLine("\uD83D\uDCF1 ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("\uD83D\uDCC5 Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("\uD83E\uDDE9 CPU: ${cpu.hardware} · ${cpu.coreCount} cores @ ${cpu.maxFreqMHz} · ${cpu.abi}")
            appendLine("\uD83C\uDDAE GPU: ${getGpuInfo().vendor} / ${getGpuInfo().openGlVersion}")
            appendLine("\uD83E\uDDE0 RAM: ${ram.availableMb}MB free / ${ram.totalMb}MB total (${ram.usedPercent}% used)")
            appendLine("\uD83D\uDCC2 Storage: ${String.format("%.1f", storage.freeGb)}GB free / ${String.format("%.1f", storage.totalGb)}GB (${storage.usedPercent}% used)")
            appendLine("\uD83D\uDCFA Display: ${display.widthPx}×${display.heightPx} @ ${String.format("%.0f", display.refreshRateHz)}Hz · ${display.densityClass} (${display.densityDpi}dpi)")
        }.trimEnd()
    }
}
