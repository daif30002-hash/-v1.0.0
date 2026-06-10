package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.HouseConfig
import com.example.data.repository.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class AppScreen {
    DASHBOARD,
    CALCULATOR,
    OVERVIEW,
    CLIMATE,
    VENTILATION,
    PROGRAMS,
    SETTINGS
}

class VentilationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HouseRepository
    val savedConfigs: StateFlow<List<HouseConfig>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HouseRepository(database.houseConfigDao())
        savedConfigs = repository.allConfigs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Navigation and Language
    var currentScreen by mutableStateOf(AppScreen.DASHBOARD)
    var isArabic by mutableStateOf(true) // Toggle state for display language (ar/en)

    // Form states (Inputs)
    var age by mutableStateOf(21)
    var birdCount by mutableStateOf(20000)
    var outsideTemp by mutableStateOf(30f)
    var insideTemp by mutableStateOf(28f)
    var targetTemp by mutableStateOf(24f)
    var insideHumidity by mutableStateOf(65f)

    // House Dimensions / Equipment
    var length by mutableStateOf(120f)
    var width by mutableStateOf(14f)
    var height by mutableStateOf(3f)
    var numFans by mutableStateOf(10)
    var fanCapacity by mutableStateOf(35000f) // m3/h per fan
    var sideInlets by mutableStateOf(40)
    var tunnelInlets by mutableStateOf(8)

    // Unit Preference: Celsius vs Fahrenheit
    var isFahrenheit by mutableStateOf(false)

    // Current editing configuration name (for saving)
    var newPresetName by mutableStateOf("")

    // Preset loaded status
    var selectedPresetId by mutableStateOf<Long?>(null)

    // UI Feedback
    var snackbarMessage by mutableStateOf<String?>(null)

    // Derived standard calculations
    val birdWeight: Float
        get() {
            // standard progression matching Ross 308
            return when {
                age <= 1 -> 0.042f
                age <= 3 -> 0.042f + (age - 1) * 0.019f
                age <= 7 -> 0.080f + (age - 3) * 0.030f
                age <= 14 -> 0.200f + (age - 7) * 0.043f
                age <= 21 -> 0.500f + (age - 14) * 0.071f
                age <= 28 -> 1.000f + (age - 21) * 0.093f
                age <= 35 -> 1.650f + (age - 28) * 0.107f
                age <= 42 -> 2.400f + (age - 35) * 0.114f
                age <= 49 -> 3.200f + (age - 42) * 0.114f
                else -> 4.000f + (age - 49) * 0.114f
            }
        }

    val totalLiveWeightKg: Float
        get() = birdCount * birdWeight

    val volumeM3: Float
        get() = length * width * height

    val crossSectionAreaM2: Float
        get() = width * height

    // Standard capacities definitions
    private val sideInletCapacity = 1500f // m3/h per side inlet
    private val tunnelInletCapacity = 8000f // m3/h per tunnel inlet

    // Dynamic calculations based on current mode
    enum class VentilationMode {
        MINIMUM,
        TRANSITIONAL,
        TUNNEL
    }

    val currentMode: VentilationMode
        get() {
            return when {
                insideTemp <= targetTemp -> VentilationMode.MINIMUM
                insideTemp <= targetTemp + 3f -> VentilationMode.TRANSITIONAL
                else -> VentilationMode.TUNNEL
            }
        }

    // Required volumes
    val minimumVentilationM3H: Float
        get() = totalLiveWeightKg * 1.0f // 1.0 m3/h/kg of bird weight

    val transitionalVentilationM3H: Float
        get() {
            val delta = insideTemp - targetTemp
            return minimumVentilationM3H * (1.0f + max(0f, delta) * 0.25f)
        }

    val tunnelVentilationM3H: Float
        get() {
            // Under full tunnel, air exchange is based on achieving proper air velocity
            // Aim for target speed depending on age, but at least enough to displace house volume
            val targetSpeed = when {
                age < 7 -> 0.1f
                age <= 21 -> 0.5f
                age <= 35 -> 1.5f
                else -> 2.5f // 2.5 m/s target
            }
            return targetSpeed * 3600f * crossSectionAreaM2
        }

    val requiredAirVolumeM3H: Float
        get() {
            return when (currentMode) {
                VentilationMode.MINIMUM -> minimumVentilationM3H
                VentilationMode.TRANSITIONAL -> transitionalVentilationM3H
                VentilationMode.TUNNEL -> {
                    // Maximum of weight requirement and velocity requirement
                    // Wait, transitional is or weight-based is lower than speed-based. Let's make it proportional to temp excess:
                    val delta = insideTemp - targetTemp
                    // speed target based on temp difference
                    val speedFactor = min(1f, max(0f, (delta - 3f) / 5f)) // scales up to +8C
                    val maxSpeed = when {
                        age < 7 -> 0.1f
                        age <= 21 -> 0.5f
                        age <= 35 -> 1.5f
                        else -> 3.0f
                    }
                    val currentTargetSpeed = speedFactor * maxSpeed
                    val volumeBySpeed = currentTargetSpeed * 3600f * crossSectionAreaM2
                    max(minimumVentilationM3H, volumeBySpeed)
                }
            }
        }

    // Inlet calculations
    val sideInletOpenPercentage: Float
        get() {
            if (currentMode == VentilationMode.TUNNEL) return 0f
            val totalCap = sideInlets * sideInletCapacity
            if (totalCap <= 0f) return 0f
            val raw = (requiredAirVolumeM3H / totalCap) * 100f
            return min(100f, max(1f, raw)) // 1% minimum or based on required air volume
        }

    val tunnelInletOpenPercentage: Float
        get() {
            if (currentMode != VentilationMode.TUNNEL) return 0f
            val totalCap = tunnelInlets * tunnelInletCapacity
            if (totalCap <= 0f) return 0f
            val raw = (requiredAirVolumeM3H / totalCap) * 100f
            return min(100f, max(5f, raw))
        }

    // Active fans
    val rawFansNeeded: Float
        get() {
            if (fanCapacity <= 0) return 0f
            return requiredAirVolumeM3H / fanCapacity
        }

    val activeFansCount: Int
        get() {
            val count = if (currentMode == VentilationMode.MINIMUM) {
                // Minimum ventilation uses fractional fan, so physically 1 fan is cycling
                1
            } else {
                ceil(rawFansNeeded).toInt()
            }
            return min(numFans, max(1, count))
        }

    val fanDutyCyclePercentage: Float
        get() {
            if (currentMode != VentilationMode.MINIMUM) return 100f
            if (fanCapacity <= 0) return 0f
            // percentage of 1 fan needed
            val pct = (requiredAirVolumeM3H / fanCapacity) * 100f
            return min(100f, max(2f, pct))
        }

    // Cycle Timer
    val cycleOnSeconds: Int
        get() {
            if (currentMode != VentilationMode.MINIMUM) return 300
            val onSec = (fanDutyCyclePercentage / 100f) * 300f
            return min(300, max(15, onSec.toInt())) // At least 15s to maintain air pressure and prevent fan damage
        }

    val cycleOffSeconds: Int
        get() {
            if (currentMode != VentilationMode.MINIMUM) return 0
            return max(0, 300 - cycleOnSeconds)
        }

    // Air exchanges
    val airChangesPerHour: Float
        get() {
            val vol = volumeM3
            if (vol <= 0) return 0f
            return requiredAirVolumeM3H / vol
        }

    val ventilationPerKg: Float
        get() {
            if (totalLiveWeightKg <= 0) return 0f
            return requiredAirVolumeM3H / totalLiveWeightKg
        }

    // Actual wind velocity inside the house
    val calculatedAirSpeedMS: Float
        get() {
            val area = crossSectionAreaM2
            if (area <= 0) return 0f
            // only tunnel mode creates directional air speed
            if (currentMode != VentilationMode.TUNNEL) {
                return 0.05f // very minimal air movement in side ventilation
            }
            val activeVol = activeFansCount * fanCapacity
            return activeVol / (3600f * area)
        }

    // Wind Chill Temperature reduction
    val windChillReduction: Float
        get() {
            val speed = calculatedAirSpeedMS
            val multiplier = when {
                age < 7 -> 0f      // no wind chill impact, birds too small
                age <= 21 -> 1.2f  // sensitive
                age <= 35 -> 1.5f  // moderate
                else -> 1.7f       // fully feathered birds feel high chill impact
            }
            return speed * multiplier
        }

    val perceivedTemp: Float
        get() = insideTemp - windChillReduction

    // Target Humidity curve
    val targetHumidity: Float
        get() {
            return when {
                age <= 3 -> 65f
                age <= 7 -> 62.5f
                age <= 14 -> 60f
                age <= 28 -> 57.5f
                else -> 60f
            }
        }

    // Warnings and alarms
    data class ClimateAlert(
        val type: AlertType,
        val messageAr: String,
        val messageEn: String
    )

    enum class AlertType {
        ALERT,   // normal warning (Yellow)
        DANGER   // high critical alarm (Red)
    }

    val alerts: List<ClimateAlert>
        get() {
            val list = mutableListOf<ClimateAlert>()

            // Temperature alerts
            val tempDiff = insideTemp - targetTemp
            if (tempDiff >= 5f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "إنذار حراري مرتفع جداً: درجة الحرارة تجاورت الهدف بـ $tempDiff°س!",
                    "High Heat Danger: Inner temp exceeds target by $tempDiff°C!"
                ))
            } else if (tempDiff >= 2.5f) {
                list.add(ClimateAlert(
                    AlertType.ALERT,
                    "تنبيه حراري: درجة الحرارة أعلى من الهدف بـ $tempDiff°س",
                    "Heat Warning: Inner temp is higher than target by $tempDiff°C"
                ))
            } else if (tempDiff < -3f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "إنذار برودة: البيت بارد جداً (درجة الحرارة أقل بـ ${-tempDiff}°س من الهدف)!",
                    "Cold Hazard: Barn ambient is extremely cold (${-tempDiff}°C below target)!"
                ))
            }

            // Humidity alerts
            if (insideHumidity > 80f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "رطوبة مرتفعة جداً (${insideHumidity}%): خطر الإصابة بأمراض تنفسية وتعفن الفرشة",
                    "Critical High Humidity (${insideHumidity}%): Dangerous respiratory disease risk & wet litter"
                ))
            } else if (insideHumidity > 72f) {
                list.add(ClimateAlert(
                    AlertType.ALERT,
                    "رطوبة مرتفعة تنبيهية (${insideHumidity}%): جودة الهواء تنخفض",
                    "Warning High Humidity (${insideHumidity}%): Discomfort & poor air quality"
                ))
            } else if (insideHumidity < 40f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "رطوبة منخفضة جداً (${insideHumidity}%): خطر الجفاف والغبار الشديد في العنبر",
                    "Critical Dry Air (${insideHumidity}%): Extreme dust & chick dehydration hazard!"
                ))
            }

            // Wind Speed safety check
            val speed = calculatedAirSpeedMS
            if (age < 14 && speed > 0.5f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "سرعة هواء خطيرة على طيور صغيرة: $speed م/ث! خطر الصدمة الحرارية والبرد",
                    "Dangerous Draft Speed for Young Broilers: $speed m/s! Risk of thermal chilling."
                ))
            } else if (age < 7 && speed > 0.1f) {
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "سرعة هواء غير مقبولة في الأسبوع الأول! يجب عدم توجيه هواء مباشر على الصيصان",
                    "Lethal draft risk to day-old chicks! Side ventilation only."
                ))
            }

            // Fan limits
            if (rawFansNeeded > numFans) {
                val deficit = ceil(rawFansNeeded - numFans).toInt()
                list.add(ClimateAlert(
                    AlertType.DANGER,
                    "عجز في المراوح: نحتاج $deficit مروحة إضافية لتحقيق التهوية كافية وعمل التبريد!",
                    "Fan Power Deficit: Short of $deficit fans to achieve the required ventilation target!"
                ))
            }

            return list
        }

    // Preset management functions (Local DB Room Integration)
    fun saveCurrentConfiguration(presetName: String) {
        if (presetName.isBlank()) {
            snackbarMessage = if (isArabic) "يرجى إدخال اسم للبرنامج المحفوظ" else "Please enter configuration name"
            return
        }

        val config = HouseConfig(
            id = selectedPresetId ?: 0,
            name = presetName,
            age = age,
            birdCount = birdCount,
            outsideTemp = outsideTemp,
            insideTemp = insideTemp,
            targetTemp = targetTemp,
            insideHumidity = insideHumidity,
            length = length,
            width = width,
            height = height,
            numFans = numFans,
            fanCapacity = fanCapacity,
            sideInlets = sideInlets,
            tunnelInlets = tunnelInlets,
            isFahrenheit = isFahrenheit,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                if (config.id > 0) {
                    repository.updateConfig(config)
                    snackbarMessage = if (isArabic) "تم تحديث الإعدادات: $presetName بنجاح!" else "Preset $presetName updated successfully!"
                } else {
                    val newId = repository.insertConfig(config)
                    selectedPresetId = newId
                    snackbarMessage = if (isArabic) "تم حفظ الإعدادات: $presetName بنجاح!" else "Preset $presetName saved successfully!"
                }
            } catch (e: Exception) {
                snackbarMessage = if (isArabic) "خطأ في الحفظ: ${e.message}" else "Save failed: ${e.message}"
            }
        }
    }

    fun loadConfiguration(config: HouseConfig) {
        selectedPresetId = config.id
        newPresetName = config.name
        age = config.age
        birdCount = config.birdCount
        outsideTemp = config.outsideTemp
        insideTemp = config.insideTemp
        targetTemp = config.targetTemp
        insideHumidity = config.insideHumidity
        length = config.length
        width = config.width
        height = config.height
        numFans = config.numFans
        fanCapacity = config.fanCapacity
        sideInlets = config.sideInlets
        tunnelInlets = config.tunnelInlets
        isFahrenheit = config.isFahrenheit

        snackbarMessage = if (isArabic) "تم تحميل الإعداد: ${config.name}" else "Loaded configuration: ${config.name}"
    }

    fun deleteConfiguration(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteConfigById(id)
                if (selectedPresetId == id) {
                    selectedPresetId = null
                    newPresetName = ""
                }
                snackbarMessage = if (isArabic) "تم حذف الإعداد بنجاح" else "Configuration deleted successfully"
            } catch (e: Exception) {
                snackbarMessage = if (isArabic) "خطأ في الحذف: ${e.message}" else "Delete failed: ${e.message}"
            }
        }
    }

    fun loadDefaultPreset() {
        // Recalls a safe default
        selectedPresetId = null
        newPresetName = ""
        age = 21
        birdCount = 20000
        outsideTemp = 32f
        insideTemp = 28f
        targetTemp = 23f
        insideHumidity = 65f
        length = 120f
        width = 14f
        height = 3.2f
        numFans = 10
        fanCapacity = 38000f
        sideInlets = 40
        tunnelInlets = 8
        isFahrenheit = false
    }

    fun formatTemp(celsius: Float): String {
        val value = if (isFahrenheit) (celsius * 1.8f + 32f) else celsius
        val unit = if (isFahrenheit) "°F" else "°C"
        return String.format("%.1f%s", value, unit)
    }
}
