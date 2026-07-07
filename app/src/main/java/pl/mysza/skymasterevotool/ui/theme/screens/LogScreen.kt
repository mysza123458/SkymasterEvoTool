package pl.mysza.skymasterevotool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogScreen(
    logText: String,
    onClear: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClear
        ) {
            Text("WYCZYŚĆ LOG")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = 2.dp
        ) {

            Text(
                text = if (logText.isBlank()) "Brak logów..." else logText,
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            )

        }
    }

}