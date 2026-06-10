package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "house_configs")
data class HouseConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int = 21,
    val birdCount: Int = 20000,
    val outsideTemp: Float = 25f,
    val insideTemp: Float = 24f,
    val targetTemp: Float = 22f,
    val insideHumidity: Float = 60f,
    val length: Float = 120f,
    val width: Float = 12f,
    val height: Float = 3f,
    val numFans: Int = 10,
    val fanCapacity: Float = 35000f,
    val sideInlets: Int = 40,
    val tunnelInlets: Int = 8,
    val isFahrenheit: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
