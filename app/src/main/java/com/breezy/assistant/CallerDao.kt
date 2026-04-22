package com.breezy.assistant

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CallerDao {
    @Query("SELECT * FROM caller_info WHERE phoneNumber = :number")
    suspend fun getCallerInfo(number: String): CallerInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callerInfo: CallerInfo)
}
