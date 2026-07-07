package pl.mysza.skymasterevotool.ui.screens
import pl.mysza.skymasterevotool.ui.components.ParametersCard
import pl.mysza.skymasterevotool.ui.components.HeaderCard
import pl.mysza.skymasterevotool.ui.components.DataCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.ble.WheelsData
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(wheelsData: WheelsData) {
    val speed = wheelsData.speed ?: 0
    val battery = wheelsData.battery ?: 0
    val currentA = (wheelsData.current ?: 0) / 100.0
    val powerW = (currentA * 36.0).roundToInt()
    val distanceKm = (wheelsData.sessionMileage ?: 0) / 1000.0

    val mode = when {
        wheelsData.maxSpeed == 18 && wheelsData.steering == 10 && wheelsData.dynamic == 6 -> "SPORT"
        wheelsData.maxSpeed == 10 && wheelsData.steering == 6 && wheelsData.dynamic == 3 -> "STANDARD"
        else -> "MANUAL"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderCard(wheelsData, battery, mode)

        Spacer(modifier = Modifier.height(14.dp))

        SpeedDigitalCard(speed)

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DataCard(
                title = "🔋 BATTERY",
                value = "$battery%",
                subtitle = batteryBar(battery),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            DataCard(
                title = "⚡ POWER",
                value = "${powerW}W",
                subtitle = String.format(Locale.US, "%.1f A", currentA),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DataCard(
                title = "📏 DISTANCE",
                value = String.format(Locale.US, "%.2f km", distanceKm),
                subtitle = "${wheelsData.sessionMileage ?: 0} m",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            DataCard(
                title = "🌡 TEMP",
                value = wheelsData.temperature?.let { "$it°C" } ?: "---",
                subtitle = "controller",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        ParametersCard(wheelsData, mode)
    }
}

@Composable
private fun SpeedDigitalCard(speed: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SPEED", style = MaterialTheme.typography.titleMedium)

            Text(
                text = "$speed",
                style = MaterialTheme.typography.displayLarge
            )

            Text("km/h", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun batteryBar(battery: Int): String {
    val filled = (battery.coerceIn(0, 100) / 10).coerceIn(0, 10)
    val empty = 10 - filled
    return "█".repeat(filled) + "░".repeat(empty)
}