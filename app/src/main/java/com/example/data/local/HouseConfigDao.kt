package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HouseConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseConfigDao {
    @Query("SELECT * FROM house_configs ORDER BY timestamp DESC")
    fun getAllConfigs(): Flow<List<HouseConfig>>

    @Query("SELECT * FROM house_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Long): HouseConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: HouseConfig): Long

    @Update
    suspend fun updateConfig(config: HouseConfig)

    @Query("DELETE FROM house_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("DELETE FROM house_configs")
    suspend fun deleteAllConfigs()
}
