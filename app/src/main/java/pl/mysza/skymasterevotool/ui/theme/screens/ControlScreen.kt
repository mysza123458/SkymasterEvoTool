package pl.mysza.skymasterevotool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mysza.skymasterevotool.ble.WheelsData
import kotlin.math.roundToInt

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

    var maxSpeed by remember(wheelsData.maxSpeed) {
        mutableFloatStateOf((wheelsData.maxSpeed ?: 10).toFloat())
    }

    var steering by remember(wheelsData.steering) {
        mutableFloatStateOf((wheelsData.steering ?: 6).toFloat())
    }

    var dynamic by remember(wheelsData.dynamic) {
        mutableFloatStateOf((wheelsData.dynamic ?: 3).toFloat())
    }

    val maxSpeedInt = maxSpeed.roundToInt().coerceIn(6, 18)
    val steeringInt = steering.roundToInt().coerceIn(1, 10)
    val dynamicInt = dynamic.roundToInt().coerceIn(0, 6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "Sterowanie",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "Manualne ustawienia",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Max Speed : $maxSpeedInt km/h")

                Slider(
                    value = maxSpeed,
                    onValueChange = { maxSpeed = it },
                    valueRange = 6f..18f,
                    steps = 11
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Steering : $steeringInt")

                Slider(
                    value = steering,
                    onValueChange = { steering = it },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Dynamic : $dynamicInt")

                Slider(
                    value = dynamic,
                    onValueChange = { dynamic = it },
                    valueRange = 0f..6f,
                    steps = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onApplySettings(
                            maxSpeedInt,
                            steeringInt,
                            dynamicInt
                        )
                    }
                ) {
                    Text("WYŚLIJ USTAWIENIA")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "Powiadomienie baterii",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Alarm przy: $batteryWarningLevel%")

                Slider(
                    value = batteryWarningLevel.toFloat(),
                    onValueChange = {
                        onBatteryWarningLevelChange(
                            it.roundToInt().coerceIn(5, 50)
                        )
                    },
                    valueRange = 5f..50f,
                    steps = 44
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    maxSpeed = 10f
                    steering = 6f
                    dynamic = 3f
                    onStandard()
                }
            ) {
                Text("STANDARD")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    maxSpeed = 18f
                    steering = 10f
                    dynamic = 6f
                    onSport()
                }
            ) {
                Text("SPORT")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {

            Button(
                modifier = Modifier.weight(1f),
                onClick = onStandbyOn
            ) {
                Text("STANDBY ON")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = onStandbyOff
            ) {
                Text("STANDBY OFF")
            }
        }
    }
}