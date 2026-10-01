package com.ideasi.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedAppDao {

    @Query("SELECT * FROM selected_apps")
    fun getAllSelectedApps(): Flow<List<SelectedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: SelectedApp)

    @Query("DELETE FROM selected_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
