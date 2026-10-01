package com.ideasi.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SelectedApp::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun selectedAppDao(): SelectedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ideas_i_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
