package pl.mysza.skymasterevotool
import pl.mysza.skymasterevotool.model.BleDeviceItem
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.mysza.skymasterevotool.ble.BleManager
import pl.mysza.skymasterevotool.ble.WheelsData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var scanCallback: ScanCallback? = null
    private var bleManager: BleManager? = null
    private val channelId = "battery_warning"

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            var wheelsData by remember { mutableStateOf(WheelsData()) }
            var batteryWarningLevel by remember { mutableIntStateOf(20) }
            var warningAlreadyShown by remember { mutableStateOf(false) }

            LaunchedEffect(wheelsData.battery, batteryWarningLevel) {
                val battery = wheelsData.battery
                if (battery != null && battery <= batteryWarningLevel && !warningAlreadyShown) {
                    showBatteryNotification(battery, batteryWarningLevel)
                    warningAlreadyShown = true
                }
                if (battery != null && battery > batteryWarningLevel) warningAlreadyShown = false
            }

            SkymasterEvoToolApp(
                wheelsData = wheelsData,
                batteryWarningLevel = batteryWarningLevel,
                onBatteryWarningLevelChange = { batteryWarningLevel = it },
                onStartScan = { addDevice, addLog -> startScan(addDevice, addLog) },
                onStopScan = { addLog -> stopScan(addLog) },
                onConnect = { device, addLog ->
                    stopScan(addLog)
                    bleManager?.disconnect()
                    bleManager = BleManager(
                        context = this,
                        log = addLog,
                        onData = { newData -> runOnUiThread { wheelsData = newData } }
                    )
                    bleManager?.connect(device)
                },
                onDisconnect = { addLog ->
                    bleManager?.disconnect()
                    bleManager = null
                    wheelsData = WheelsData()
                    addLog("Ręcznie rozłączono")
                },
                onSport = { bleManager?.setSportMode() },
                onStandard = { bleManager?.setStandardMode() },
                onStandbyOn = { bleManager?.standbyOn() },
                onStandbyOff = { bleManager?.standbyOff() },
                onApplySettings = { maxSpeed, steering, dynamic ->
                    bleManager?.setMaxSpeed(maxSpeed)
                    Handler(Looper.getMainLooper()).postDelayed({
                        bleManager?.setSteering(steering)
                    }, 300)
                    Handler(Looper.getMainLooper()).postDelayed({
                        bleManager?.setDynamic(dynamic)
                    }, 600)
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan(addDevice: (BleDeviceItem) -> Unit, addLog: (String) -> Unit) {
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            addLog("BŁĄD: BluetoothLeScanner = null")
            return
        }

        stopScan(addLog)
        addLog("Start skanowania BLE...")

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName ?: "Unknown"
                val address = device.address
                val rssi = result.rssi

                if (name.contains("Wheels", ignoreCase = true)) {
                    addLog("Znaleziono: $name / $address / RSSI $rssi")
                    addDevice(BleDeviceItem(name, address, rssi, device))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                addLog("BŁĄD SKANOWANIA: $errorCode")
            }
        }

        scanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(addLog: (String) -> Unit) {
        val callback = scanCallback
        if (callback != null) {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(callback)
            scanCallback = null
            addLog("Skanowanie zatrzymane")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ostrzeżenia baterii",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Powiadomienia o niskiej baterii hoverboarda"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBatteryNotification(battery: Int, threshold: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Niska bateria hoverboarda")
            .setContentText("Bateria: $battery%. Próg ostrzeżenia: $threshold%.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(2001, notification)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkymasterEvoToolApp(
    wheelsData: WheelsData,
    batteryWarningLevel: Int,
    onBatteryWarningLevelChange: (Int) -> Unit,
    onStartScan: ((BleDeviceItem) -> Unit, (String) -> Unit) -> Unit,
    onStopScan: ((String) -> Unit) -> Unit,
    onConnect: (BluetoothDevice, (String) -> Unit) -> Unit,
    onDisconnect: ((String) -> Unit) -> Unit,
    onSport: () -> Unit,
    onStandard: () -> Unit,
    onStandbyOn: () -> Unit,
    onStandbyOff: () -> Unit,
    onApplySettings: (Int, Int, Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var logText by remember { mutableStateOf("") }
    var devices by remember { mutableStateOf(listOf<BleDeviceItem>()) }

    fun addLog(text: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logText += "$time  $text\n"
    }

    fun addDevice(device: BleDeviceItem) {
        devices = devices.filterNot { it.address == device.address }.plus(device)
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Skymaster EVO Tool v1.0") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("🏠") },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("🔍") },
                        label = { Text("Scan") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("⚙️") },
                        label = { Text("Control") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Text("📋") },
                        label = { Text("Log") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(wheelsData)
                    1 -> ScanScreen(
                        devices = devices,
                        onStartScan = {
                            devices = emptyList()
                            onStartScan(::addDevice, ::addLog)
                        },
                        onStopScan = { onStopScan(::addLog) },
                        onConnect = { item ->
                            addLog("Kliknięto: ${item.name} ${item.address}")
                            onConnect(item.device, ::addLog)
                        },
                        onDisconnect = { onDisconnect(::addLog) }
                    )
                    2 -> ControlScreen(
                        wheelsData = wheelsData,
                        batteryWarningLevel = batteryWarningLevel,
                        onBatteryWarningLevelChange = onBatteryWarningLevelChange,
                        onStandard = onStandard,
                        onSport = onSport,
                        onStandbyOn = onStandbyOn,
                        onStandbyOff = onStandbyOff,
                        onApplySettings = onApplySettings
                    )
                    3 -> LogScreen(logText = logText, onClear = { logText = "" })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(wheelsData: WheelsData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (wheelsData.connected) "🟢 Wheels 11 EVO Smart" else "🔴 Brak połączenia",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🔋 Bateria", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = wheelsData.battery?.let { "$it%" } ?: "---",
                    style = MaterialTheme.typography.displayLarge
                )
                LinearProgressIndicator(
                    progress = { (wheelsData.battery ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard("🚀", "Prędkość", wheelsData.speed?.let { "$it km/h" } ?: "---", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            InfoCard("📏", "Dystans", wheelsData.sessionMileage?.let { "$it m" } ?: "---", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard("🌡", "Temperatura", wheelsData.temperature?.let { "$it°C" } ?: "---", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            InfoCard("⚡", "Prąd", wheelsData.current?.let { "$it" } ?: "---", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Parametry jazdy", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Max Speed: ${wheelsData.maxSpeed ?: "---"} km/h")
                Text("Steering: ${wheelsData.steering ?: "---"}")
                Text("Dynamic: ${wheelsData.dynamic ?: "---"}")
                Text("Autoryzacja: ${if (wheelsData.authOk) "OK" else "---"}")
            }
        }
    }
}

@Composable
fun InfoCard(icon: String, title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun ScanScreen(
    devices: List<BleDeviceItem>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BleDeviceItem) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Button(onClick = onStartScan, modifier = Modifier.fillMaxWidth()) { Text("SKANUJ WHEELS") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onStopScan, modifier = Modifier.fillMaxWidth()) { Text("STOP SKAN") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("ROZŁĄCZ") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Znalezione urządzenia:", style = MaterialTheme.typography.titleMedium)

        if (devices.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Brak urządzeń Wheels")
        } else {
            devices.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onConnect(item) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.address)
                        Text("RSSI: ${item.rssi}")
                        Text("Kliknij, aby połączyć")
                    }
                }
            }
        }
    }
}

@Composable
fun ControlScreen(
    wheelsData: WheelsData,
    batteryWarningLevel: Int,
    onBatteryWarningLevelChange: (Int) -> Unit,
    onStandard: () -> Unit,
    onSport: () -> Unit,
    onStandbyOn: () -> Unit,
    onStandbyOff: () -> Unit,
    onApplySettings: (Int, Int, Int) -> Unit
) {
    var maxSpeed by remember(wheelsData.maxSpeed) { mutableFloatStateOf((wheelsData.maxSpeed ?: 10).toFloat()) }
    var steering by remember(wheelsData.steering) { mutableFloatStateOf((wheelsData.steering ?: 6).toFloat()) }
    var dynamic by remember(wheelsData.dynamic) { mutableFloatStateOf((wheelsData.dynamic ?: 3).toFloat()) }

    val maxSpeedInt = maxSpeed.roundToInt().coerceIn(6, 18)
    val steeringInt = steering.roundToInt().coerceIn(1, 10)
    val dynamicInt = dynamic.roundToInt().coerceIn(0, 6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Sterowanie", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manualne ustawienia", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(12.dp))
                Text("Max Speed: $maxSpeedInt km/h")
                Slider(value = maxSpeed, onValueChange = { maxSpeed = it }, valueRange = 6f..18f, steps = 11)

                Text("Steering: $steeringInt")
                Slider(value = steering, onValueChange = { steering = it }, valueRange = 1f..10f, steps = 8)

                Text("Dynamic: $dynamicInt")
                Slider(value = dynamic, onValueChange = { dynamic = it }, valueRange = 0f..6f, steps = 5)

                Button(
                    onClick = { onApplySettings(maxSpeedInt, steeringInt, dynamicInt) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("WYŚLIJ USTAWIENIA")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ostrzeżenie baterii", style = MaterialTheme.typography.titleMedium)
                Text("Próg powiadomienia: $batteryWarningLevel%")
                Slider(
                    value = batteryWarningLevel.toFloat(),
                    onValueChange = { onBatteryWarningLevelChange(it.roundToInt().coerceIn(5, 50)) },
                    valueRange = 5f..50f,
                    steps = 44
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    maxSpeed = 10f
                    steering = 6f
                    dynamic = 3f
                    onStandard()
                },
                modifier = Modifier.weight(1f)
            ) { Text("STANDARD") }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    maxSpeed = 18f
                    steering = 10f
                    dynamic = 6f
                    onSport()
                },
                modifier = Modifier.weight(1f)
            ) { Text("SPORT") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onStandbyOn, modifier = Modifier.weight(1f)) { Text("STANDBY ON") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onStandbyOff, modifier = Modifier.weight(1f)) { Text("STANDBY OFF") }
        }
    }
}

@Composable
fun LogScreen(logText: String, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("WYCZYŚĆ LOG") }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxSize(), tonalElevation = 2.dp) {
            Text(
                text = if (logText.isBlank()) "Brak logów..." else logText,
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}