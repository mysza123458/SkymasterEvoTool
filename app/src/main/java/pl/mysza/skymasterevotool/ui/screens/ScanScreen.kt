package pl.mysza.skymasterevotool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.model.BleDeviceItem

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
        Button(onClick = onStartScan, modifier = Modifier.fillMaxWidth()) {
            Text("SKANUJ WHEELS")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onStopScan, modifier = Modifier.fillMaxWidth()) {
            Text("STOP SKAN")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
            Text("ROZŁĄCZ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Znalezione urządzenia:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        if (devices.isEmpty()) {
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