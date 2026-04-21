package com.breezy.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log

class BreezyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BreezyGhost"
        var instance: BreezyAccessibilityService? = null
        
        // Messaging Whitelist (Supported Apps)
        private val WHITELIST_PACKAGES = setOf(
            "com.whatsapp",
            "com.facebook.orca",        // Messenger
            "org.telegram.messenger",
            "org.thoughtcrime.securesms", // Signal
            "com.google.android.apps.messaging", // Google Messages
            "com.google.android.gm"      // Gmail
        )
    }

    override fun onServiceConnected() {
        instance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        Log.d(TAG, "Breezy Ghost Mode Active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We can monitor window changes here if needed
    }

    override fun onInterrupt() {}

    /**
     * The "Ghost Hand": Automatically finds a text field, types the message, 
     * and clicks the send button in supported apps.
     */
    fun typeAndSend(packageName: String, message: String): Boolean {
        if (!WHITELIST_PACKAGES.contains(packageName)) {
            Log.w(TAG, "Package not in Ghost Hand whitelist: $packageName")
            return false
        }

        val rootNode = rootInActiveWindow ?: return false
        
        // 1. Find the input field
        val inputNode = findInputNode(rootNode)
        if (inputNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            // 2. Find and click the send button
            val sendButton = findSendButton(rootNode)
            sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        
        return false
    }

    /**
     * "Safe Word" Wipe: Rapidly clicks 'Back' and home to hide chat content 
     * and clears the system notifications to prevent peeking.
     */
    fun performPanicWipe() {
        // Clear notifications
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        Thread.sleep(200) // Small wait for drawer to open
        performGlobalAction(GLOBAL_ACTION_BACK) // Close it immediately
        
        // Return to home screen instantly
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Additional: Lock screen if allowed (requires API 28+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    private fun findInputNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            if (node.isEditable) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for common "Send" button identifiers or content descriptions
        val sendHints = listOf("send", "submit", "post", "publish", "✈️")
        val queue = mutableListOf(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            val desc = node.contentDescription?.toString()?.lowercase()
            val text = node.text?.toString()?.lowercase()
            
            if (node.isClickable && (sendHints.any { desc?.contains(it) == true } || sendHints.any { text?.contains(it) == true })) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
