package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HouseConfig
import com.example.ui.components.BarnLayoutVisualizer
import com.example.ui.components.ClimateCurveChart
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.VentilationViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: VentilationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Support RTL dynamically when Arabic is toggled
                val layoutDirection = if (viewModel.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color(0xFF111122) // Pure professional dark background
                    ) { innerPadding ->
                        MainResponsiveLayout(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainResponsiveLayout(
    viewModel: VentilationViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val savedConfigs by viewModel.savedConfigs.collectAsState(initial = emptyList())

    // Show feedback messages
    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage?.let {
            // Can show a toast or local view indicator
            // Reset message
            viewModel.snackbarMessage = null
        }
    }

    // Responsive design decision: Sidebar vs BottomBar
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 800.dp

        if (isTablet) {
            // Horizontal Split: Sidebar + Primary Content Panel
            Row(modifier = Modifier.fillMaxSize()) {
                SidebarNavigationPanel(
                    currentScreen = viewModel.currentScreen,
                    isArabic = viewModel.isArabic,
                    onNavigate = { viewModel.currentScreen = it }
                )
                VerticalDivider(color = Color(0xFF1E1E30), thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TopHeaderBar(viewModel = viewModel, savedConfigs = savedConfigs)
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenCoordinator(viewModel = viewModel, savedConfigs = savedConfigs)
                    }
                }
            }
        } else {
            // Vertical Split: TopHeader + Primary Content Panel + Bottom Bar
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeaderBar(viewModel = viewModel, savedConfigs = savedConfigs)
                Box(modifier = Modifier.weight(1f)) {
                    ScreenCoordinator(viewModel = viewModel, savedConfigs = savedConfigs)
                }
                BottomNavigationBarPanel(
                    currentScreen = viewModel.currentScreen,
                    isArabic = viewModel.isArabic,
                    onNavigate = { viewModel.currentScreen = it }
                )
            }
        }
    }
}

/**
 * Common Top Bar with Language switch, Preset Dropdowns, and current Mode indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeaderBar(
    viewModel: VentilationViewModel,
    savedConfigs: List<HouseConfig>
) {
    var expandedPresetMenu by remember { mutableStateOf(false) }

    val modeLabel = if (viewModel.isArabic) {
        when (viewModel.currentMode) {
            VentilationViewModel.VentilationMode.MINIMUM -> "التهوية الدنيا"
            VentilationViewModel.VentilationMode.TRANSITIONAL -> "التهوية الانتقالية"
            VentilationViewModel.VentilationMode.TUNNEL -> "التهوية النفقية"
        }
    } else {
        when (viewModel.currentMode) {
            VentilationViewModel.VentilationMode.MINIMUM -> "Minimum Vent"
            VentilationViewModel.VentilationMode.TRANSITIONAL -> "Transitional Vent"
            VentilationViewModel.VentilationMode.TUNNEL -> "Tunnel Vent"
        }
    }

    val modeColors = when (viewModel.currentMode) {
        VentilationViewModel.VentilationMode.MINIMUM -> Color(0xFF10B981) // Green
        VentilationViewModel.VentilationMode.TRANSITIONAL -> Color(0xFFF59E0B) // Yellow
        VentilationViewModel.VentilationMode.TUNNEL -> Color(0xFF00ADB5) // Cyan/Blue
    }

    Surface(
        color = Color(0xFF161630),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Name / Logo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (viewModel.isArabic) "نظام تهوية الدواجن SKOV" else "SKOV Poultry Ventilation",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Active Mode Label Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(modeColors.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = modeColors,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Quick Preset Selection Config Icon
            Box {
                IconButton(onClick = { expandedPresetMenu = !expandedPresetMenu }) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Presets",
                        tint = Color.Cyan
                    )
                }
                DropdownMenu(
                    expanded = expandedPresetMenu,
                    onDismissRequest = { expandedPresetMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E38))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (viewModel.isArabic) "حمل الوضع القياسي" else "Load Standard Defaults",
                                color = Color.White
                            )
                        },
                        onClick = {
                            viewModel.loadDefaultPreset()
                            expandedPresetMenu = false
                        }
                    )
                    Divider(color = Color(0x33FFFFFF))

                    if (savedConfigs.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (viewModel.isArabic) "لا توجد حظائر محفوظة" else "No saved presets",
                                    color = Color.LightGray
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        savedConfigs.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.name, color = Color.White) },
                                onClick = {
                                    viewModel.loadConfiguration(config)
                                    expandedPresetMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Language Switcher Buttons
            TextButton(
                onClick = { viewModel.isArabic = !viewModel.isArabic },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3F3F5F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = if (viewModel.isArabic) "English" else "العربية",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Coordinate screen displays
 */
@Composable
fun ScreenCoordinator(
    viewModel: VentilationViewModel,
    savedConfigs: List<HouseConfig>
) {
    when (viewModel.currentScreen) {
        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
        AppScreen.CALCULATOR -> CalculatorScreen(viewModel = viewModel)
        AppScreen.OVERVIEW -> OverviewScreen(viewModel = viewModel)
        AppScreen.CLIMATE -> ClimateScreen(viewModel = viewModel)
        AppScreen.VENTILATION -> VentilationDetailsScreen(viewModel = viewModel)
        AppScreen.PROGRAMS -> ProgramsScreen(viewModel = viewModel)
        AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, savedConfigs = savedConfigs)
    }
}

/**
 * 🏠 Dashboard Screen (لوحة التحكم)
 */
@Composable
fun DashboardScreen(viewModel: VentilationViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick welcome header
        Text(
            text = if (viewModel.isArabic) "ملخص حالة العنبر" else "Barn Summary Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Alerts and Warnings Panel
        val activeAlerts = viewModel.alerts
        if (activeAlerts.isNotEmpty()) {
            Text(
                text = if (viewModel.isArabic) "التنبيهات البرمجية النشطة (${activeAlerts.size})" else "Active Alerts (${activeAlerts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )
            activeAlerts.forEach { alert ->
                AlertCard(alert = alert, isArabic = viewModel.isArabic)
            }
        } else {
            // Safe normal state card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3E22)),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Safe",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (viewModel.isArabic) "جميع المؤشرات مستقرة وطبيعية" else "All Environmental Systems Normal",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (viewModel.isArabic) "العوامل الجوية والتهوية تلبي المتطلبات العلمية للطير." else "Environmental factors meet perfect biological specifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Live stats grid
        Text(
            text = if (viewModel.isArabic) "المؤشرات الرئيسية السريعة" else "Key Live Indicators",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardMetricCard(
                axisLabel = if (viewModel.isArabic) "درجة الحرارة" else "Inside Temperature",
                currentVal = viewModel.formatTemp(viewModel.insideTemp),
                secondaryVal = if (viewModel.isArabic) "المستهدفة: ${viewModel.formatTemp(viewModel.targetTemp)}" else "Target: ${viewModel.formatTemp(viewModel.targetTemp)}",
                color = if (kotlin.math.abs(viewModel.insideTemp - viewModel.targetTemp) < 2.5f) Color.Green else Color.Yellow,
                icon = Icons.Filled.DeviceThermostat,
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                axisLabel = if (viewModel.isArabic) "الرطوبة النسبية" else "Relative Humidity",
                currentVal = String.format("%.1f%%", viewModel.insideHumidity),
                secondaryVal = if (viewModel.isArabic) "المستهدفة: %.1f%%".format(viewModel.targetHumidity) else "Target: %.1f%%".format(viewModel.targetHumidity),
                color = if (viewModel.insideHumidity in 40f..80f) Color.Green else Color.Red,
                icon = Icons.Filled.WaterDrop,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardMetricCard(
                axisLabel = if (viewModel.isArabic) "سرعة الهواء (الرياح)" else "Interal Wind Speed",
                currentVal = String.format("%.2f m/s", viewModel.calculatedAirSpeedMS),
                secondaryVal = if (viewModel.isArabic) "وزن الطائر: %.3f كجم".format(viewModel.birdWeight) else "Bird Weight: %.3f kg".format(viewModel.birdWeight),
                color = Color.Cyan,
                icon = Icons.Filled.Air,
                modifier = Modifier.weight(1f)
            )

            val modeString = if (viewModel.isArabic) {
                when (viewModel.currentMode) {
                    VentilationViewModel.VentilationMode.MINIMUM -> "دنيا (دورية)"
                    VentilationViewModel.VentilationMode.TRANSITIONAL -> "انتقالية مستمرة"
                    VentilationViewModel.VentilationMode.TUNNEL -> "نفقية كاملة"
                }
            } else {
                viewModel.currentMode.name
            }

            DashboardMetricCard(
                axisLabel = if (viewModel.isArabic) "نمط التهوية النشط" else "Ventilation Program",
                currentVal = modeString,
                secondaryVal = if (viewModel.isArabic) "${viewModel.activeFansCount} مراوح تعمل" else "${viewModel.activeFansCount} active fans",
                color = Color.Green,
                icon = Icons.Filled.RotateRight,
                modifier = Modifier.weight(1f)
            )
        }

        // Additional information
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E38)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (viewModel.isArabic) "بيانات الحظيرة الحالية" else "Current Barn Setup Values",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = Color(0x22FFFFFF))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (viewModel.isArabic) "عمر القطيع:" else "Herd Age:", color = Color.White)
                    Text("${viewModel.age} " + (if (viewModel.isArabic) "يوم" else "days"), color = Color.Green, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (viewModel.isArabic) "عدد الطيور الكلي:" else "Broiler Count:", color = Color.White)
                    Text(String.format("%,d", viewModel.birdCount), color = Color.Green, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (viewModel.isArabic) "تبادل الهواء بالساعة:" else "Air Changes Per Hour:", color = Color.White)
                    Text(String.format("%.1f AC/h", viewModel.airChangesPerHour), color = Color.Cyan, fontWeight = FontWeight.Bold)
                }
                if (viewModel.currentMode == VentilationViewModel.VentilationMode.MINIMUM) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (viewModel.isArabic) "مؤقت التهوية الدنيا (كل 5 دقائق):" else "Cycle Timer (5m period):", color = Color.White)
                        Text("ON: ${viewModel.cycleOnSeconds}s | OFF: ${viewModel.cycleOffSeconds}s", color = Color.Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Professional Signature Footer as requested by user!
        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * Custom alert representation
 */
@Composable
fun AlertCard(alert: VentilationViewModel.ClimateAlert, isArabic: Boolean) {
    val containerColor = if (alert.type == VentilationViewModel.AlertType.DANGER) Color(0xFF4C081B) else Color(0xFF4C3008)
    val borderColor = if (alert.type == VentilationViewModel.AlertType.DANGER) Color.Red else Color.Yellow
    val icon = if (alert.type == VentilationViewModel.AlertType.DANGER) Icons.Filled.Error else Icons.Filled.Warning

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Alert",
                tint = borderColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isArabic) alert.messageAr else alert.messageEn,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 🌬️ Ventilation Calculator Screen (حاسبة التهوية)
 */
@Composable
fun CalculatorScreen(viewModel: VentilationViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (viewModel.isArabic) "إدخال المعطيات وحساب النتائج فورياً" else "Instant Ventilation Simulator & Calculator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Group 1: Flock inputs
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (viewModel.isArabic) "1. معطيات الدورة والقطيع" else "1. Flock & Biological Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )

                // Age input
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "عمر القطيع (بالأيام)" else "Herd Age (Days)",
                    value = viewModel.age.toFloat(),
                    range = 1f..56f,
                    onValueChange = { viewModel.age = it.toInt() },
                    formatString = "%.0f"
                )

                // Chicken count
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "عدد طيور الدواجن" else "Broiler Count",
                    value = viewModel.birdCount.toFloat(),
                    range = 1000f..100000f,
                    step = 1000f,
                    onValueChange = { viewModel.birdCount = it.toInt() },
                    formatString = "%,.0f"
                )
            }
        }

        // Group 2: Ambient Environment Temperatures
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (viewModel.isArabic) "2. درجات الحرارة والرطوبة" else "2. Live Climate Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )

                // Outside temp
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "الحرارة الخارجية" else "Outside Temperature",
                    value = viewModel.outsideTemp,
                    range = -10f..50f,
                    onValueChange = { viewModel.outsideTemp = it },
                    suffix = if (viewModel.isFahrenheit) "°F" else "°C"
                )

                // Inside temp
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "الحرارة الداخلية الفعلية" else "Inside Actual Temperature",
                    value = viewModel.insideTemp,
                    range = 15f..40f,
                    onValueChange = { viewModel.insideTemp = it },
                    suffix = if (viewModel.isFahrenheit) "°F" else "°C"
                )

                // Target temp
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "الحرارة المستهدفة المطلوبة" else "Target Objective Temperature",
                    value = viewModel.targetTemp,
                    range = 18f..35f,
                    onValueChange = { viewModel.targetTemp = it },
                    suffix = if (viewModel.isFahrenheit) "°F" else "°C"
                )

                // Inside Humidity
                InputFieldWithControls(
                    label = if (viewModel.isArabic) "الرطوبة النسبية الداخلية" else "Relative Air Humidity",
                    value = viewModel.insideHumidity,
                    range = 30f..95f,
                    onValueChange = { viewModel.insideHumidity = it },
                    suffix = "%"
                )
            }
        }

        // Dynamic Calculations outputs
        Text(
            text = if (viewModel.isArabic) "النتائج الحسابية الفورية" else "Real-time Computed Results",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101026)),
            border = BorderStroke(1.dp, Color(0xFF3F3F5F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Total required air volume rate
                OutputStatRow(
                    label = if (viewModel.isArabic) "إجمالي حجم الهواء المطلوب" else "Required Ventilation Rate",
                    value = String.format("%,.0f m³/h", viewModel.requiredAirVolumeM3H),
                    highlightColor = Color.Green
                )

                // Mode
                val modeStr = if (viewModel.isArabic) {
                    when (viewModel.currentMode) {
                        VentilationViewModel.VentilationMode.MINIMUM -> "التهوية الدنيا (مؤقت دوري)"
                        VentilationViewModel.VentilationMode.TRANSITIONAL -> "التهوية الانتقالية المستمرة"
                        VentilationViewModel.VentilationMode.TUNNEL -> "التهوية النفقية (سرعة هواء عالية)"
                    }
                } else {
                    viewModel.currentMode.name
                }
                OutputStatRow(
                    label = if (viewModel.isArabic) "نمط التهوية المستنتج" else "Resulting Ventilation Mode",
                    value = modeStr,
                    highlightColor = if (viewModel.currentMode == VentilationViewModel.VentilationMode.TUNNEL) Color.Cyan else Color.Green
                )

                // Active fans
                val fanPct = (viewModel.activeFansCount.toFloat() / viewModel.numFans.toFloat()) * 100f
                OutputStatRow(
                    label = if (viewModel.isArabic) "عدد المراوح المطلوب تشغيلها" else "Required Exhaust Fans",
                    value = "${viewModel.activeFansCount} / ${viewModel.numFans} مراوح (${String.format("%.0f%%", fanPct)})",
                    highlightColor = Color.Yellow
                )

                // Inlet statuses
                OutputStatRow(
                    label = if (viewModel.isArabic) "فتحة الهواء الجانبية (Side)" else "Side Inlet Openness",
                    value = String.format("%.1f %%", viewModel.sideInletOpenPercentage),
                    highlightColor = if (viewModel.sideInletOpenPercentage > 0) Color.Green else Color.Gray
                )

                OutputStatRow(
                    label = if (viewModel.isArabic) "فتحة الهواء النفقية (Tunnel)" else "Tunnel Inlet Openness",
                    value = String.format("%.1f %%", viewModel.tunnelInletOpenPercentage),
                    highlightColor = if (viewModel.tunnelInletOpenPercentage > 0) Color.Cyan else Color.Gray
                )

                // Wind speed
                OutputStatRow(
                    label = if (viewModel.isArabic) "سرعة الهواء في المقطع" else "Computed Wind Speed",
                    value = String.format("%.2f m/s", viewModel.calculatedAirSpeedMS),
                    highlightColor = Color.Cyan
                )

                // Wind chill drop
                OutputStatRow(
                    label = if (viewModel.isArabic) "تأثير تبريد الهواء (Wind Chill)" else "Perceived Cooling Drop",
                    value = "- ${String.format("%.1f°C", viewModel.windChillReduction)} (حرارة محسوسة: ${String.format("%.1f°C", viewModel.perceivedTemp)})",
                    highlightColor = Color.Green
                )

                // Cycle timer
                if (viewModel.currentMode == VentilationViewModel.VentilationMode.MINIMUM) {
                    Divider(color = Color(0x33FFFFFF))
                    Text(
                        text = if (viewModel.isArabic) "مؤقت دورة التهوية الدنيا (ON/OFF - دورة ٥ دقائق)" else "Cycle Timer Logic (ON/OFF over 5m / 300s)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (viewModel.isArabic) "زمن التشغيل (ON):" else "ON duration (sec):", color = Color.LightGray)
                        Text("${viewModel.cycleOnSeconds} ثانية", color = Color.Green, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (viewModel.isArabic) "زمن الإيقاف (OFF):" else "OFF duration (sec):", color = Color.LightGray)
                        Text("${viewModel.cycleOffSeconds} ثانية", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Climate curve visualizer loaded here dynamically
        Text(
            text = if (viewModel.isArabic) "منحنى ومعيار التهوية البيولوجي" else "Biological Standard Temperature Curve",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        ClimateCurveChart(
            currentAge = viewModel.age,
            currentValue = viewModel.insideTemp,
            targetValue = viewModel.targetTemp,
            isHumidity = false,
            isArabic = viewModel.isArabic,
            isFahrenheit = viewModel.isFahrenheit,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF161630), shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        )

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * 🏗️ House Overview Screen (نظرة عامة على البيت)
 */
@Composable
fun OverviewScreen(viewModel: VentilationViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = if (viewModel.isArabic) "المحاكاة الهندسية للعنبر والتدفق" else "Real-time 2D Barn Fluid Schematic Simulator",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (viewModel.isArabic) "يوضح اتجاه تدفق الهواء، حالة المراوح والفتحات الجانبية والنفقية" else "Displays current air paths, rotating exhausting shafts and active vents.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }

        // Live visualizer canvas component
        BarnLayoutVisualizer(
            ventilationMode = viewModel.currentMode,
            sideInletsCount = viewModel.sideInlets,
            tunnelInletsCount = viewModel.tunnelInlets,
            totalFansCount = viewModel.numFans,
            activeFansCount = viewModel.activeFansCount,
            sideInletOpeningPct = viewModel.sideInletOpenPercentage,
            tunnelInletOpeningPct = viewModel.tunnelInletOpenPercentage,
            isArabic = viewModel.isArabic,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14142B))
                .padding(12.dp)
        )

        // Status Indicators legends panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (viewModel.isArabic) "مفتاح الرموز وحالة المحاكاة" else "Simulator Legend Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(color = Color(0xFF22C55E), text = if (viewModel.isArabic) "فتحات جانبية نشطة" else "Side Inlets Opening", modifier = Modifier.weight(1f))
                    LegendItem(color = Color(0xFF00ADB5), text = if (viewModel.isArabic) "فتحات تبريد نفقية" else "Tunnel Vents Active", modifier = Modifier.weight(1f))
                    LegendItem(color = Color.Green, text = if (viewModel.isArabic) "مراوح دفع تعمل" else "Exhaust Fans Running", modifier = Modifier.weight(1f))
                }
            }
        }

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

@Composable
fun LegendItem(color: Color, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

/**
 * 🌡️ Climate Screen (المناخ)
 */
@Composable
fun ClimateScreen(viewModel: VentilationViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (viewModel.isArabic) "تحليل المناخ ومعايير الرطوبة" else "Flock Climate Standards & Humidity Rules",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Compare Gauge
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (viewModel.isArabic) "مقارنة الحرارة الآن ضد الحرارة المطلوبة" else "Inner Temp vs Target Temperature Objective",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (viewModel.isArabic) "الداخلية الفعلية" else "Actual Temp", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(viewModel.formatTemp(viewModel.insideTemp), style = MaterialTheme.typography.headlineLarge, color = Color.Green, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "vs", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (viewModel.isArabic) "المستهدفة علمياً" else "Standard Goal", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(viewModel.formatTemp(viewModel.targetTemp), style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Relative Humidity Canvas Chart below
        Text(
            text = if (viewModel.isArabic) "تغير الرطوبة المطلوبة حسب عمر القطيع" else "Biological Standard Relative Humidity Progression",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        ClimateCurveChart(
            currentAge = viewModel.age,
            currentValue = viewModel.insideHumidity,
            targetValue = viewModel.targetHumidity,
            isHumidity = true,
            isArabic = viewModel.isArabic,
            isFahrenheit = viewModel.isFahrenheit,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF161630), shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        )

        // Display Daily Temp Offset Table and Relative Humidity standard tables dynamically!
        Text(
            text = if (viewModel.isArabic) "جدول أزاحة درجة الحرارة يومياً" else "Daily Standard Temperature Offsets Table",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        DailyTempOffsetsTable(isArabic = viewModel.isArabic)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (viewModel.isArabic) "جدول تبريد الطائر وضمان الرطوبة النسبية" else "Relative Biological Humidity Guideline Matrix",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        RelativeHumidityGuidelineTable(isArabic = viewModel.isArabic)

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * 💨 Ventilation Screen (التهوية تفصيلي)
 */
@Composable
fun VentilationDetailsScreen(viewModel: VentilationViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (viewModel.isArabic) "التحليل العميق لمعالجة ميكانيكا الهواء" else "Detailed Fluid Airflow & Thermal Volume Analysis",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Detail air metrics card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (viewModel.isArabic) "العوامل والأرقام الفيزيائية النشطة" else "Current Live Mechanical Ventilation Rates",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = Color(0x33FFFFFF))

                OutputStatRow(
                    label = if (viewModel.isArabic) "الوزن المقدر للطائر المفرد:" else "Calculated Weight Per Boiler:",
                    value = String.format("%.3f kg", viewModel.birdWeight),
                    highlightColor = Color.White
                )
                OutputStatRow(
                    label = if (viewModel.isArabic) "إجمالي الوزن الحي في الحظيرة:" else "Total Biomass Live Weight:",
                    value = String.format("%,.1f kg", viewModel.totalLiveWeightKg),
                    highlightColor = Color.Green
                )
                OutputStatRow(
                    label = if (viewModel.isArabic) "حجم الهواء اللازم للتهوية الدنيا:" else "Required Baseline Volume:",
                    value = String.format("%,.0f m³/h", viewModel.minimumVentilationM3H),
                    highlightColor = Color.White
                )
                OutputStatRow(
                    label = if (viewModel.isArabic) "معدل تدفق كيلوغرام وزن حي:" else "Flow Rate Per Biomass Kg:",
                    value = String.format("%.2f m³/h/kg", viewModel.ventilationPerKg),
                    highlightColor = Color.Cyan
                )
                OutputStatRow(
                    label = if (viewModel.isArabic) "مساحة مقطع سحب الهواء:" else "Barn Tunnel Cross Section Area:",
                    value = String.format("%.2f m²", viewModel.crossSectionAreaM2),
                    highlightColor = Color.White
                )
                OutputStatRow(
                    label = if (viewModel.isArabic) "حجم الحظيرة الكلي المكعب:" else "Total Inner Cubical Volume (V):",
                    value = String.format("%,.1f m³", viewModel.volumeM3),
                    highlightColor = Color.LightGray
                )
            }
        }

        // Display Tables requested by the user:
        // 1. Min Air Speeds table
        Text(
            text = if (viewModel.isArabic) "جدول الحد الأدنى لسرعات الهواء المقبولة" else "Flock Age-Based Safe Air Speeds Matrix",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        MinAirSpeedTable(isArabic = viewModel.isArabic)

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Wind chill curve table
        Text(
            text = if (viewModel.isArabic) "جدول تبريد الرياح (Wind Chill Curve)" else "Wind Chill Thermal Drop Factor Curve",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        WindChillCurveTable(isArabic = viewModel.isArabic)

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * 📅 Programs Screen (البرامج والجدولة)
 */
@Composable
fun ProgramsScreen(viewModel: VentilationViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (viewModel.isArabic) "البرامج والجدولة القياسية المعتمدة" else "Standard Poultry Management Guides (Cobb/Ross)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Guide standard temperatures
        Text(
            text = if (viewModel.isArabic) "١. جدول درجات الحرارة والرطوبة حسب عمر الطائر" else "1. Standard Biological Climate Progression",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Green,
            fontWeight = FontWeight.Bold
        )
        TargetClimateGuideTable(isArabic = viewModel.isArabic)

        Spacer(modifier = Modifier.height(8.dp))

        // Lighting Program Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (viewModel.isArabic) "٢. خطة الإضاءة المتوازنة علمياً" else "2. Balanced Standard Lighting Management",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = Color(0x33FFFFFF))
                Text(
                    text = if (viewModel.isArabic) {
                        "• الأيام ١-٣: ٢٣ ساعة إضاءة (قوة ٣٠-٥٠ لوكس) لتشجيع كتاكت على العثور على الأكل والماء.\n" +
                        "• الأيام ٤-٧: ٢٠ ساعة إضاءة مع تخفيض شدة الإضاءة تدريجياً.\n" +
                        "• الأيام ٨-٣٥: ١٨ ساعة إضاءة (٦ ساعات إظلام كامل للنمو العظمي السليم لتقليل الوفيات والبطن المائي).\n" +
                        "• الأيام ٣٦+: ٢٠-٢٢ ساعة إضاءة لتجهيز الدواجن للتسويق."
                    } else {
                        "• Days 1-3: 23 hrs light (30-50 lux) to encourage feed finding and high survival.\n" +
                        "• Days 4-7: 20 hrs light, progressively reducing lux intensity.\n" +
                        "• Days 8-35: 18 hrs light (6 hrs solid darkness to enforce strong bone structure and prevent ascites).\n" +
                        "• Days 36+: 20-22 hrs light to prepare flock for marketing."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    lineHeight = 20.sp
                )
            }
        }

        // Feeding Program Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (viewModel.isArabic) "٣. خطة التغذية ومراحل العلف" else "3. Scientific Nutrition & Feeding Stages",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = Color(0x33FFFFFF))
                Text(
                    text = if (viewModel.isArabic) {
                        "• علف البادي (Starter): عمر ١ إلى ١٠ أيام (فتات صغر، بروتين ٢٣٪ لحجم ممتاز للصيصان).\n" +
                        "• علف النامي (Grower): عمر ١١ إلى ٢٥ يوم (حبيبات، بروتين ٢١٪ لبناء الهيكل والأعضاء).\n" +
                        "• علف الناهي (Finisher): عمر ٢٦ يوم إلى التسويق (بروتين ١٩٪ لتقليل الدهون وبناء لحم الصدر)."
                    } else {
                        "• Starter Feed: Ages 1 to 10 Days (crumbs, 23% protein to maximize chick start and development).\n" +
                        "• Grower Feed: Ages 11 to 25 Days (pelletes, 21% protein to construct frames and muscles).\n" +
                        "• Finisher Feed: Age 26 to Sale (pelletes, 19% protein to optimize meat deposition and feed conversion ratio)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    lineHeight = 20.sp
                )
            }
        }

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * ⚙️ Settings Screen (الإعدادات)
 */
@Composable
fun SettingsScreen(
    viewModel: VentilationViewModel,
    savedConfigs: List<HouseConfig>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (viewModel.isArabic) "تخصيص بنية الحظيرة وحفظ التهيئة" else "Configure Physical Equipment & Saves",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Save current preset
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E38)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (viewModel.isArabic) "حفظ إعدادات الحظيرة الحالية للرجوع لها" else "Save Current Inputs as Preset Preset",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.newPresetName,
                    onValueChange = { viewModel.newPresetName = it },
                    label = { Text(if (viewModel.isArabic) "اسم الحظيرة / العنبر" else "Barn / Custom Presest Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Green,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.saveCurrentConfiguration(viewModel.newPresetName) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (viewModel.isArabic) "احفظ في قاعدة البيانات المحلية (Room)" else "Overwrite/Insert Preset",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Saved list list
        if (savedConfigs.isNotEmpty()) {
            Text(
                text = if (viewModel.isArabic) "الحظائر المحفوظة سابقاً" else "Database Saved Presets",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            savedConfigs.forEach { saved ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(saved.name, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                if (viewModel.isArabic) {
                                    "صيصان: %,d طائر | أبعاد: %.0fx%.0fم".format(saved.birdCount, saved.length, saved.width)
                                } else {
                                    "Birds: %,d | Dims: %.0fx%.0fm".format(saved.birdCount, saved.length, saved.width)
                                },
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.loadConfiguration(saved) }) {
                            Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = "Load", tint = Color.Green)
                        }
                        IconButton(onClick = { viewModel.deleteConfiguration(saved.id) }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Equipment parameters Config
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (viewModel.isArabic) "تعديل القدرات والقياسات الهندسية" else "Configure Physical Equipment Specs",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "طول بيت الدواجن (متر)" else "Barn Length (m)",
                    value = viewModel.length,
                    range = 10f..250f,
                    onValueChange = { viewModel.length = it }
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "عرض بيت الدواجن (متر)" else "Barn Width (m)",
                    value = viewModel.width,
                    range = 5f..30f,
                    onValueChange = { viewModel.width = it }
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "ارتفاع السقف (متر)" else "Ceiling Height (m)",
                    value = viewModel.height,
                    range = 2f..6f,
                    onValueChange = { viewModel.height = it }
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "إجمالي عدد المراوح المثبتة" else "Total Exhaust Fans Installed",
                    value = viewModel.numFans.toFloat(),
                    range = 1f..50f,
                    onValueChange = { viewModel.numFans = it.toInt() },
                    formatString = "%.0f"
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "قدرة المروحة المفردة (م³/ساعة)" else "Single Fan Air Flow (m3/h)",
                    value = viewModel.fanCapacity,
                    range = 10000f..65000f,
                    step = 1000f,
                    onValueChange = { viewModel.fanCapacity = it },
                    formatString = "%,.0f"
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "عدد فتحات التهوية الجانبية (Side)" else "Total Side Inlets Mounted",
                    value = viewModel.sideInlets.toFloat(),
                    range = 0f..120f,
                    onValueChange = { viewModel.sideInlets = it.toInt() },
                    formatString = "%.0f"
                )

                InputFieldWithControls(
                    label = if (viewModel.isArabic) "عدد فتحات التهوية النفقية (Tunnel)" else "Total Tunnel Inlets Appended",
                    value = viewModel.tunnelInlets.toFloat(),
                    range = 0f..30f,
                    onValueChange = { viewModel.tunnelInlets = it.toInt() },
                    formatString = "%.0f"
                )

                Divider(color = Color(0x33FFFFFF))

                // Toggle Fahrenheit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (viewModel.isArabic) "التحويل للنظام الفهرنهايتي" else "Toggle Fahrenheit Units", color = Color.White)
                        Text(if (viewModel.isArabic) "يعرض درجات الحرارة بالفهرنهايت" else "Show temperatures in Fahrenheit scale", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = viewModel.isFahrenheit,
                        onCheckedChange = { viewModel.isFahrenheit = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Green)
                    )
                }
            }
        }

        DrDhaifallahSignature(isArabic = viewModel.isArabic)
    }
}

/**
 * -------------------------------------------
 * CUSTOM COMPONENETS & TABLES HELPER FUNCTIONS
 * -------------------------------------------
 */

@Composable
fun DashboardMetricCard(
    axisLabel: String,
    currentVal: String,
    secondaryVal: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(axisLabel, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(currentVal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(secondaryVal, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun OutputStatRow(
    label: String,
    value: String,
    highlightColor: Color = Color.Green
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = highlightColor)
    }
}

/**
 * Robust Input Control Component giving exact slider or fast tap click additions
 */
@Composable
fun InputFieldWithControls(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    step: Float = 1f,
    suffix: String = "",
    formatString: String = "%.1f"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
            val displaySuffix = if (suffix.isNotEmpty()) " $suffix" else ""
            Text(
                text = String.format(formatString, value) + displaySuffix,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus button
            FilledIconButton(
                onClick = {
                    val newVal = value - step
                    if (newVal >= range.start) onValueChange(newVal)
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2E2E4A)),
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Slider
            Slider(
                value = value,
                valueRange = range,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Green,
                    inactiveTrackColor = Color(0xFF3F3F5F),
                    thumbColor = Color.Green
                ),
                modifier = Modifier.weight(1f)
            )

            // Plus button
            FilledIconButton(
                onClick = {
                    val newVal = value + step
                    if (newVal <= range.endInclusive) onValueChange(newVal)
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2E2E4A)),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

/**
 * -------------------------------------------
 * STATIC SCIENTIFIC TABLES IN ARABIC & ENGLISH
 * -------------------------------------------
 */

@Composable
fun MinAirSpeedTable(isArabic: Boolean) {
    val headers = if (isArabic) {
        listOf("عمر الطائر (يوم)", "سرعة الهواء الدنيا المقبولة (م/ث)", "التأثير الحيوي علمياً")
    } else {
        listOf("Herd Age (Days)", "Min Wind Speed (m/s)", "Biological Impact")
    }
    val rows = if (isArabic) {
        listOf(
            listOf("أقل من 7 أيام", "لا توجد مباشرة", "تجنب توجيه الرياح وضرورة التهوية الجانبية فقط"),
            listOf("7 - 14 يوم", "0.2 - 0.5 م/ث", "سرعة منخفضة للمانع الصدمة والتحصينات"),
            listOf("15 - 21 يوم", "0.5 - 1.0 م/ث", "تبادل متوسط لسحب الرطوبة الزائدة"),
            listOf("22 - 28 يوم", "1.0 - 1.5 م/ث", "تبديد حرارة اللحم وتثبيت الفرشة"),
            listOf("29 - 35 يوم", "1.5 - 2.0 م/ث", "توفير التبريد المحسوس المتوازن"),
            listOf("أكبر من 35 يوم", "2.0 - 3.0 م/ث", "تبريد كامل مكثف لتفادي الإجهاد الحراري والتفوق")
        )
    } else {
        listOf(
            listOf("< 7 days", "No direct wind", "Side ventilation only (avoid chilling baby chicks)"),
            listOf("7 - 14 days", "0.2 - 0.5 m/s", "Low velocity preventing cold thermal shock"),
            listOf("15 - 21 days", "0.5 - 1.0 m/s", "Mild exchange helping moisture depletion"),
            listOf("22 - 28 days", "1.0 - 1.5 m/s", "Adequate speed to clean out gases & dust"),
            listOf("29 - 35 days", "1.5 - 2.0 m/s", "Balanced wind-chill cooling for broilers"),
            listOf("> 35 days", "2.0 - 3.0 m/s", "Max tunnel volume to stop heat exhaustion mortality")
        )
    }
    ScientificGridTable(headers = headers, rows = rows)
}

@Composable
fun WindChillCurveTable(isArabic: Boolean) {
    val headers = if (isArabic) {
        listOf("سرعة تدفق الهواء (م/ث)", "درجة التبريد الفعلي المحسوس (°س)", "الأعمار الأكثر تأثراً")
    } else {
        listOf("Air Speed (m/s)", "Sensory Cool Impact Drop (°C)", "Affected Flock Age Class")
    }
    val rows = if (isArabic) {
        listOf(
            listOf("0.5 م/ث", "انخفاض بمقدار 1°س - 2°س", "7-21 يوم (حساسية عالية جداً)"),
            listOf("1.0 م/ث", "انخفاض بمقدار 3°س - 4°س", "كل الأعمار (انتقال ممتاز)"),
            listOf("1.5 م/ث", "انخفاض بمقدار 5°س - 6°س", "أكبر من 21 يوم (مستوى كفء)"),
            listOf("2.0 م/ث", "انخفاض بمقدار 7°س - 8°س", "أكبر من 28 يوم (تبريد رئيسي)"),
            listOf("2.5 م/ث", "انخفاض بمقدار 9°س - 10°س", "أكبر من 35 يوم (التحمل القهرى للحر)"),
            listOf("3.0 م/ث", "انخفاض بمقدار 11°س - 12°س", "أكبر من 35 يوم (الحد الأقصى للسكوم)")
        )
    } else {
        listOf(
            listOf("0.5 m/s", "Chills temp by 1°C - 2°C", "7-21 Days (Highly sensitive)"),
            listOf("1.0 m/s", "Chills temp by 3°C - 4°C", "All age classes (standard wind)"),
            listOf("1.5 m/s", "Chills temp by 5°C - 6°C", "Over 21 Days (High efficiency)"),
            listOf("2.0 m/s", "Chills temp by 7°C - 8°C", "Over 28 Days (Primary summer mode)"),
            listOf("2.5 m/s", "Chills temp by 9°C - 10°C", "Over 35 Days (Anti-heat stress)"),
            listOf("3.0 m/s", "Chills temp by 11°C - 12°C", "Over 35 Days (Maximum SKOV limit)")
        )
    }
    ScientificGridTable(headers = headers, rows = rows)
}

@Composable
fun DailyTempOffsetsTable(isArabic: Boolean) {
    val headers = if (isArabic) {
        listOf("الفترة الزمنية", "تنزل درجة الحرارة يومياً", "خط الهبوط الإرشادي")
    } else {
        listOf("Age Phase", "Daily Temperature Drop", "Guideline Curve")
    }
    val rows = if (isArabic) {
        listOf(
            listOf("الأسبوع الأول (الأيام 1-7)", "0.4°س إلى 0.5°س كل يوم", "تنزل من 33°س إلى 30°س للنمو السريع"),
            listOf("الأسبوع الثاني (الأيام 8-14)", "0.3°س كل يوم", "تنزل من 30°س إلى 28°س لتكوين الريش"),
            listOf("الأسبوع الثالث (الأيام 15-21)", "0.3°س كل يوم", "تنزل من 28°س إلى 26°س لدعم الجهاز الهضمي"),
            listOf("الأسبوع الرابع (الايام 22-28)", "0.3°س كل يوم", "تنزل من 26°س إلى 24°س لتنشيط الأيض"),
            listOf("الأسبوع الخامس فما فوق", "0.1°س كل يوم", "الاستقرار التام عند 21°س أو 20°س للحم الخالص")
        )
    } else {
        listOf(
            listOf("Week 1 (Days 1-7)", "0.4°C to 0.5°C per day", "From 33°C to 30°C to speed feather growth"),
            listOf("Week 2 (Days 8-14)", "0.3°C per day", "From 30°C to 28°C assisting skeletal buildup"),
            listOf("Week 3 (Days 15-21)", "0.3°C per day", "From 28°C to 26°C enhancing immune resilience"),
            listOf("Week 4 (Days 22-28)", "0.3°C per day", "From 26°C to 24°C optimizing feed conversion"),
            listOf("Week 5 and beyond", "0.1°C per day", "Holds stable around 20°C for meat optimization")
        )
    }
    ScientificGridTable(headers = headers, rows = rows)
}

@Composable
fun RelativeHumidityGuidelineTable(isArabic: Boolean) {
    val headers = if (isArabic) {
        listOf("عمر الطير (يوم)", "الرطوبة النسبية الموصى بها (%)", "مخاطر الابتعاد عن النطاق الموصى به")
    } else {
        listOf("Flock Age (Days)", "Ideal Relative Humidity (%)", "Environmental Hazard Risk if out-of-range")
    }
    val rows = if (isArabic) {
        listOf(
            listOf("الأيام 1 - 3", "60 % - 70 %", "أقل من 50% تسبب الجفاف والنفوق المبكر الكثيف"),
            listOf("الأيام 4 - 7", "60 % - 65 %", "يرجى عدم زيادة الرطوبة لمقاومة العفن والميكروبات"),
            listOf("الأيام 8 - 14", "55 % - 65 %", "مستوى مثالي لجفاف الفرشة ودورة تهوية ناجحة"),
            listOf("الأيام 15 - 28", "50 % - 65 %", "وقاية من مشاكل الكوكسيديا ومخاطر البكتيريا المرضية"),
            listOf("أكبر من 28 يوم", "50 % - 70 %", "الرطوبة فوق 80٪ توقف تبخير المجرى التنفسي وتقتل الطيور")
        )
    } else {
        listOf(
            listOf("Days 1 - 3", "60 % - 70 %", "Below 50% brings acute dehydration and high mortality"),
            listOf("Days 4 - 7", "60 % - 65 %", "Keep tight control to safeguard chicks respiratory health"),
            listOf("Days 8 - 14", "55 % - 65 %", "Best scope ensuring completely dry and sanitized litter"),
            listOf("Days 15 - 28", "50 % - 65 %", "Inhibits coccidia pathogen growth and viral vectoring"),
            listOf("Over 28 Days", "50 % - 70 %", "Over 80% causes respiratory failure in high ambient heat")
        )
    }
    ScientificGridTable(headers = headers, rows = rows)
}

@Composable
fun TargetClimateGuideTable(isArabic: Boolean) {
    val headers = if (isArabic) {
        listOf("عمر القطيع باليوم", "الحرارة المستهدفة (°س)", "الرطوبة النسبية (%)")
    } else {
        listOf("Flock Age (Day)", "Standard Temp Goal (°C)", "Ideal Air Humidity (%)")
    }
    val rows = listOf(
        listOf("1", "33 - 34", "60 - 70"),
        listOf("3", "32 - 33", "60 - 70"),
        listOf("7", "30 - 31", "60 - 65"),
        listOf("14", "28 - 29", "55 - 65"),
        listOf("21", "26 - 27", "50 - 65"),
        listOf("28", "24 - 25", "50 - 65"),
        listOf("35", "22 - 23", "50 - 70"),
        listOf("42", "20 - 21", "50 - 70")
    )
    ScientificGridTable(headers = headers, rows = rows)
}

/**
 * Common Grid Table Component
 */
@Composable
fun ScientificGridTable(
    headers: List<String>,
    rows: List<List<String>>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161630)),
        border = BorderStroke(1.dp, Color(0xFF3F3F5F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E4A))
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Divider(color = Color(0xFF3F3F5F), thickness = 1.dp)

            // Content Rows
            rows.forEachIndexed { rIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rIndex % 2 == 0) Color(0xFF161630) else Color(0xFF1E1E38))
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (rIndex < rows.size - 1) {
                    Divider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Signature widget requested explicitly with signature matching
 * "المطور د.ضيف الله الحسني"
 */
@Composable
fun DrDhaifallahSignature(isArabic: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isArabic) "المطور د.ضيف الله الحسني" else "Developer Dr. Dhaifallah Al-Hasani",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isArabic) "مبني على أساس المعادلات العلمية المعتمدة علمياً لصناعة الدواجن" else "Engineered on standard scientific formulas and international guidelines",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Navigation views panels
 */
@Composable
fun SidebarNavigationPanel(
    currentScreen: AppScreen,
    isArabic: Boolean,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        color = Color(0xFF161630),
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isArabic) "لوحة التحكم الرئيسية" else "System Modules",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            NavigationSidebarItem(
                label = if (isArabic) "لوحة التحكم" else "Dashboard",
                icon = Icons.Filled.Home,
                selected = currentScreen == AppScreen.DASHBOARD,
                onClick = { onNavigate(AppScreen.DASHBOARD) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "حاسبة التهوية" else "Air Calculator",
                icon = Icons.Filled.Air,
                selected = currentScreen == AppScreen.CALCULATOR,
                onClick = { onNavigate(AppScreen.CALCULATOR) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "رسم الحظيرة" else "Barn View",
                icon = Icons.Filled.Warehouse,
                selected = currentScreen == AppScreen.OVERVIEW,
                onClick = { onNavigate(AppScreen.OVERVIEW) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "المناخ والرطوبة" else "Flock Climate",
                icon = Icons.Filled.Thermostat,
                selected = currentScreen == AppScreen.CLIMATE,
                onClick = { onNavigate(AppScreen.CLIMATE) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "تفاصيل التهوية" else "Vent Details",
                icon = Icons.Filled.Air,
                selected = currentScreen == AppScreen.VENTILATION,
                onClick = { onNavigate(AppScreen.VENTILATION) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "البرامج والجدولة" else "Flock Programs",
                icon = Icons.Filled.CalendarMonth,
                selected = currentScreen == AppScreen.PROGRAMS,
                onClick = { onNavigate(AppScreen.PROGRAMS) }
            )

            NavigationSidebarItem(
                label = if (isArabic) "الإعدادات الفنية" else "Hardware Settings",
                icon = Icons.Filled.Settings,
                selected = currentScreen == AppScreen.SETTINGS,
                onClick = { onNavigate(AppScreen.SETTINGS) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "SKOV Standard v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun NavigationSidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF2E2E4A) else Color.Transparent
    val tint = if (selected) Color.Green else Color.White

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun BottomNavigationBarPanel(
    currentScreen: AppScreen,
    isArabic: Boolean,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF161630),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onNavigate(AppScreen.DASHBOARD) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text(if (isArabic) "لوحة" else "Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.CALCULATOR,
            onClick = { onNavigate(AppScreen.CALCULATOR) },
            icon = { Icon(Icons.Filled.Air, contentDescription = "Calc") },
            label = { Text(if (isArabic) "حاسبة" else "Calc", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.OVERVIEW,
            onClick = { onNavigate(AppScreen.OVERVIEW) },
            icon = { Icon(Icons.Filled.Warehouse, contentDescription = "Overview") },
            label = { Text(if (isArabic) "منظر" else "Layout", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.CLIMATE,
            onClick = { onNavigate(AppScreen.CLIMATE) },
            icon = { Icon(Icons.Filled.Thermostat, contentDescription = "Climate") },
            label = { Text(if (isArabic) "المناخ" else "Climate", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.VENTILATION,
            onClick = { onNavigate(AppScreen.VENTILATION) },
            icon = { Icon(Icons.Filled.Speed, contentDescription = "Vent") },
            label = { Text(if (isArabic) "تهوية" else "Vent", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.PROGRAMS,
            onClick = { onNavigate(AppScreen.PROGRAMS) },
            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Programs") },
            label = { Text(if (isArabic) "برامج" else "Prog", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onNavigate(AppScreen.SETTINGS) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text(if (isArabic) "إعدادات" else "Setup", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Green, selectedTextColor = Color.Green, indicatorColor = Color(0xFF2E2E4A))
        )
    }
}
