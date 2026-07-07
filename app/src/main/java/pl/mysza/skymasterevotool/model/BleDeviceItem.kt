package pl.mysza.skymasterevotool.model

import android.bluetooth.BluetoothDevice

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice
)