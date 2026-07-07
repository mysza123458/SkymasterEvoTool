package pl.mysza.skymasterevotool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.ble.WheelsData

@Composable
fun ParametersCard(
    wheelsData: WheelsData,
    mode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "PARAMETRY JAZDY",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Tryb: $mode")
            Text("Max Speed: ${wheelsData.maxSpeed ?: "---"} km/h")
            Text("Steering: ${wheelsData.steering ?: "---"}")
            Text("Dynamic: ${wheelsData.dynamic ?: "---"}")
            Text("Autoryzacja: ${if (wheelsData.authOk) "OK" else "---"}")
        }
    }
}