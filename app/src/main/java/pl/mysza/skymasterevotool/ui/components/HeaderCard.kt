package pl.mysza.skymasterevotool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.ble.WheelsData

@Composable
fun HeaderCard(
    wheelsData: WheelsData,
    battery: Int,
    mode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛹 WHEELS 11 EVO SMART",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (wheelsData.connected) "🟢 CONNECTED" else "🔴 DISCONNECTED",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "🔋 $battery%    📶 ${wheelsData.rssi?.let { "$it dBm" } ?: "---"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MODE: $mode",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}