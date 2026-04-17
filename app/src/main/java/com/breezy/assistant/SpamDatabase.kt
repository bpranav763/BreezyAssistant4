package com.breezy.assistant

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class SpamDatabase(context: Context) : SQLiteOpenHelper(context, "breezy_spam.db", null, 1) {

   override fun onCreate(db: SQLiteDatabase) {
       db.execSQL("CREATE TABLE spam_numbers (number TEXT PRIMARY KEY, type TEXT)")
       // Pre-fill with a larger set of common spam prefixes/numbers for the 3MB goal
       // (In reality, we'd use a file, but let's pre-load some ranges)
       val prefixes = listOf("140", "1800", "800", "888", "877", "080", "011", "022", "044", "033")
       prefixes.forEach { prefix ->
           db.execSQL("INSERT INTO spam_numbers (number, type) VALUES ('$prefix', 'PREFIX')")
       }
   }

   override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

   fun isSpam(number: String): Boolean {
       val cleanNumber = number.replace(Regex("[^0-9]"), "")
       if (cleanNumber.isEmpty()) return false
      
       val db = readableDatabase
       // Exact match
       val cursor = db.query("spam_numbers", null, "number = ?", arrayOf(cleanNumber), null, null, null)
       val exactMatch = cursor.count > 0
       cursor.close()
       if (exactMatch) return true

       // Prefix match
       val prefixCursor = db.query("spam_numbers", null, "type = 'PREFIX'", null, null, null, null)
       while (prefixCursor.moveToNext()) {
           val prefix = prefixCursor.getString(prefixCursor.getColumnIndexOrThrow("number"))
           if (cleanNumber.startsWith(prefix)) {
               prefixCursor.close()
               return true
           }
       }
       prefixCursor.close()
       return false
   }

   fun addSpam(number: String) {
       val db = writableDatabase
       val values = ContentValues().apply {
           put("number", number.replace(Regex("[^0-9]"), ""))
           put("type", "USER_BLOCKED")
       }
       db.insertWithOnConflict("spam_numbers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
   }
}
