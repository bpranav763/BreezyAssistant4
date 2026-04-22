package com.breezy.assistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallerIdService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (phoneNumber != null) {
                        onIncomingCall(phoneNumber)
                    }
                }
                TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> {
                    hideCallerPopup()
                }
            }
        }
    }

    private fun onIncomingCall(phoneNumber: String) {
        lifecycleScope.launch {
            val contactName = getContactNameFromPhonebook(phoneNumber)
            if (contactName != null) {
                // Known contact, optionally skip or show "Contact Calling"
                return@launch
            }

            val callerInfo = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@CallerIdService).callerDao().getCallerInfo(phoneNumber)
            }

            if (callerInfo != null) {
                showCallerPopup(phoneNumber, callerInfo.displayName ?: "Breezy ID", callerInfo.spamScore > 5)
            } else {
                showCallerPopup(phoneNumber, "Unknown Number", false)
            }
        }
    }

    private fun getContactNameFromPhonebook(phoneNumber: String): String? {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun showCallerPopup(number: String, name: String, isSpam: Boolean) {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (isSpam) 0xFF7F1D1D.toInt() else 0xFF1E3A8A.toInt())
            setPadding(40, 40, 40, 40)
            
            addView(TextView(context).apply {
                text = if (isSpam) "⚠️ LIKELY SPAM" else "BREEZY CALLER ID"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            
            addView(TextView(context).apply {
                text = name
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 20f
                setPadding(0, 10, 0, 0)
            })
            
            addView(TextView(context).apply {
                text = number
                setTextColor(0xFFD1D5DB.toInt())
                textSize = 14f
                setPadding(0, 5, 0, 0)
            })
        }

        overlayView = root
        windowManager.addView(overlayView, params)
    }

    private fun hideCallerPopup() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        hideCallerPopup()
    }
}
