package com.breezy.assistant

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caller_info")
data class CallerInfo(
    @PrimaryKey val phoneNumber: String,
    val displayName: String?,
    val spamScore: Int = 0
)
