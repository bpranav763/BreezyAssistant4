package com.breezy.assistant

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val memory = BreezyMemory(this)
        if (!memory.isOnboardingDone()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish(); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                5469
            )
        } else { launch() }
    }

    private fun launch() {
        val memory = BreezyMemory(this)
        val llm = LLMInference(this)

        try {
            val svc = Intent(this, FloatingCircleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
            else startService(svc)
            
            // Start Caller ID Service
            val callerIdSvc = Intent(this, CallerIdService::class.java)
            startService(callerIdSvc)
        } catch (e: Exception) { android.util.Log.e("BREEZY", "Service failed: ${e.message}") }
        
        MorningReportWorker.schedule(this)
        
        startActivity(Intent(this, MainTabActivity::class.java))
        
        // Auto-trigger model download if enabled and missing
        if (memory.isAutoDownloadEnabled() && !llm.isDownloaded()) {
            startActivity(Intent(this, ModelDownloadActivity::class.java))
        }

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5469) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) launch()
            else { Toast.makeText(this, "Overlay needed for Breezy circle", Toast.LENGTH_LONG).show(); launch() }
        }
    }
}
