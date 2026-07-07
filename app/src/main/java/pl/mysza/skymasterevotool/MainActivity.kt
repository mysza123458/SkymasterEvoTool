package pl.mysza.skymasterevotool
import pl.mysza.skymasterevotool.ui.screens.ControlScreen
import pl.mysza.skymasterevotool.ui.screens.ScanScreen
import pl.mysza.skymasterevotool.ui.screens.HomeScreen
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