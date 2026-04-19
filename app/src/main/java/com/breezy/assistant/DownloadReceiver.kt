package com.breezy.assistant

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = manager.query(query)

            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))

                if (status == DownloadManager.STATUS_SUCCESSFUL && title.contains("Breezy Brain")) {
                    Log.i("BreezyDownload", "Background download complete!")
                    BreezyMemory(context).saveFact("ai_model_ready", "true")
                    
                    // Notify the user or refresh the engine
                    val llm = LLMInference(context)
                    llm.ensureLoaded()
                }
            }
            cursor.close()
        }
    }
}
