package pl.mysza.skymasterevotool.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.util.UUID

data class WheelsData(
    val connected: Boolean = false,
    val authOk: Boolean = false,
    val battery: Int? = null,
    val speed: Int? = null,
    val temperature: Int? = null,
    val current: Int? = null,
    val sessionMileage: Int? = null,
    val maxSpeed: Int? = null,
    val steering: Int? = null,
    val dynamic: Int? = null
)

class BleManager(
    private val context: Context,
    private val log: (String) -> Unit,
    private val onData: (WheelsData) -> Unit
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val WRITE_UUID: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        val NOTIFY_UUID: UUID = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var data = WheelsData()

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        log("Connecting...")
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        log("Disconnect requested")
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        data = WheelsData()
        onData(data)
    }

    @SuppressLint("MissingPermission")
    private fun sendFrame(frame: ByteArray, label: String) {
        val currentGatt = gatt
        val characteristic = writeCharacteristic

        if (currentGatt == null || characteristic == null) {
            log("TX $label ERROR: brak połączenia")
            return
        }

        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = frame

        val ok = currentGatt.writeCharacteristic(characteristic)
        log("TX $label -> ${frame.toHex()} result=$ok")
    }

    fun setStandardMode() {
        log("Ustawiam tryb STANDARD")
        setMaxSpeed(10)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ setSteering(6) }, 300)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ setDynamic(3) }, 600)
    }

    fun setSportMode() {
        log("Ustawiam tryb SPORT")
        setMaxSpeed(18)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ setSteering(10) }, 300)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ setDynamic(6) }, 600)
    }

    fun setMaxSpeed(value: Int) {
        val safe = value.coerceIn(6, 18)
        sendFrame(byteArrayOf(0x03, 0x01, 0x01, safe.toByte()), "MAX SPEED $safe")
    }

    fun setSteering(value: Int) {
        val safe = value.coerceIn(1, 10)
        sendFrame(byteArrayOf(0x05, 0x01, 0x01, safe.toByte()), "STEERING $safe")
    }

    fun setDynamic(value: Int) {
        val safe = value.coerceIn(0, 6)
        sendFrame(byteArrayOf(0x04, 0x01, 0x01, safe.toByte()), "DYNAMIC $safe")
    }

    fun standbyOn() {
        sendFrame(byteArrayOf(0x02, 0x01, 0x01, 0x00), "STANDBY ON")
    }

    fun standbyOff() {
        sendFrame(byteArrayOf(0x02, 0x01, 0x01, 0x01), "STANDBY OFF")
    }

    private fun sendAuth() {
        sendFrame(byteArrayOf(0x01, 0x00, 0x03, 0x00, 0x00, 0x00), "AUTH")
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                data = data.copy(connected = true)
                onData(data)
                log("CONNECTED status=$status")
                gatt.discoverServices()
            } else {
                data = data.copy(connected = false, authOk = false)
                onData(data)
                log("DISCONNECTED status=$status newState=$newState")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            log("Services discovered status=$status")

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                log("FFF0 NOT FOUND")
                return
            }

            writeCharacteristic = service.getCharacteristic(WRITE_UUID)
            if (writeCharacteristic == null) {
                log("FFF1 NOT FOUND")
                return
            }

            val notify = service.getCharacteristic(NOTIFY_UUID)
            if (notify == null) {
                log("FFF4 NOT FOUND")
                return
            }

            log("FFF0 OK / FFF1 OK / FFF4 OK")

            gatt.setCharacteristicNotification(notify, true)

            val descriptor = notify.getDescriptor(CCCD_UUID)
            if (descriptor == null) {
                log("CCCD descriptor NOT FOUND")
                return
            }

            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val ok = gatt.writeDescriptor(descriptor)
            log("Enable notification writeDescriptor=$ok")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            log("Descriptor write status=$status")

            if (descriptor.uuid == CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                log("Notification enabled OK")
                sendAuth()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            log("Characteristic write status=$status")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value
            log("RX -> ${value.toHex()}")
            parseFrame(value)
        }
    }

    private fun parseFrame(frame: ByteArray) {
        if (frame.size < 3) return

        when (frame[0].toInt() and 0xFF) {
            0x01 -> {
                if (frame.size >= 4) {
                    val ok = (frame[3].toInt() and 0xFF) == 0
                    data = data.copy(authOk = ok)
                    onData(data)
                    log(if (ok) "AUTH OK" else "AUTH FAIL")
                }
            }

            0x00 -> {
                if (frame.size >= 11) {
                    val mileage = ((frame[4].toInt() and 0xFF) shl 8) or
                            (frame[5].toInt() and 0xFF)

                    val speed = frame[6].toInt() and 0xFF
                    val temp = frame[7].toInt() and 0xFF

                    val current = ((frame[8].toInt() and 0xFF) shl 8) or
                            (frame[9].toInt() and 0xFF)

                    val battery = frame[10].toInt() and 0xFF

                    data = data.copy(
                        battery = battery,
                        speed = speed,
                        temperature = temp,
                        current = current,
                        sessionMileage = mileage
                    )
                    onData(data)

                    log("STATUS: battery=$battery%, speed=$speed, temp=$temp°C, current=$current, mileage=$mileage")
                }
            }

            0x03 -> if (frame.size >= 4) {
                val v = frame[3].toInt() and 0xFF
                data = data.copy(maxSpeed = v)
                onData(data)
                log("MAX SPEED: $v")
            }

            0x04 -> if (frame.size >= 4) {
                val v = frame[3].toInt() and 0xFF
                data = data.copy(dynamic = v)
                onData(data)
                log("DYNAMIC: $v")
            }

            0x05 -> if (frame.size >= 4) {
                val v = frame[3].toInt() and 0xFF
                data = data.copy(steering = v)
                onData(data)
                log("STEERING: $v")
            }
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}