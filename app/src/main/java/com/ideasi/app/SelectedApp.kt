package com.ideasi.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "selected_apps")
data class SelectedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val limitMinutes: Int
)
