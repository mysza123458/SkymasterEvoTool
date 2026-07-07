package pl.mysza.skymasterevotool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.ble.WheelsData

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
fun InfoCard(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}