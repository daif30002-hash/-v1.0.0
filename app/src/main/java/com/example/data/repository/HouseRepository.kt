package com.example.data.repository

import com.example.data.local.HouseConfigDao
import com.example.data.model.HouseConfig
import kotlinx.coroutines.flow.Flow

class HouseRepository(private val houseConfigDao: HouseConfigDao) {
    val allConfigs: Flow<List<HouseConfig>> = houseConfigDao.getAllConfigs()

    suspend fun getConfigById(id: Long): HouseConfig? {
        return houseConfigDao.getConfigById(id)
    }

    suspend fun insertConfig(config: HouseConfig): Long {
        return houseConfigDao.insertConfig(config)
    }

    suspend fun updateConfig(config: HouseConfig) {
        houseConfigDao.updateConfig(config)
    }

    suspend fun deleteConfigById(id: Long) {
        houseConfigDao.deleteConfigById(id)
    }

    suspend fun deleteAllConfigs() {
        houseConfigDao.deleteAllConfigs()
    }
}
