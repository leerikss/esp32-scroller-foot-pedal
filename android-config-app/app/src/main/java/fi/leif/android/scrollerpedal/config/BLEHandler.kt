package fi.leif.android.scrollerpedal.config

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BLEHandler(private val context: Context) {

    private var gatt: BluetoothGatt? = null
    private var reconnect: ScheduledFuture<*>? = null

    private var onCheckPermissionListener: Runnable? = null
    fun setOnCheckPermissionListener(impl: Runnable) { onCheckPermissionListener = impl }

    private var onConnectListener: Runnable? = null
    fun setOnConnectListener(impl: Runnable) { onConnectListener = impl }

    private var onDisconnectListener: Runnable? = null
    fun setOnDisconnectListener(impl: Runnable) { onDisconnectListener = impl }

    fun interface ConfigEntryConsumer { fun with(entry: ConfigEntry?) }
    private var onCharacteristicFoundListener: ConfigEntryConsumer? = null
    fun setOnCharacteristicFoundListener(impl: ConfigEntryConsumer) { onCharacteristicFoundListener = impl}

    fun interface CharacteristicReadConsumer { fun with(entry: ConfigEntry?, value: String) }
    private var onCharacteristicReadListener: CharacteristicReadConsumer? = null
    fun setOnCharacteristicReadListener(impl: CharacteristicReadConsumer) { onCharacteristicReadListener = impl}

    private var onCharacteristicWrittenListener: ConfigEntryConsumer? = null
    fun setOnCharacteristicWrittenListener(impl: ConfigEntryConsumer) { onCharacteristicWrittenListener = impl}

    @Suppress("DEPRECATION")
    fun start() {
        // BLE reconnect interval
        reconnect = reconnect ?: Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate({ gatt ?: start() }, 0, 2, TimeUnit.SECONDS)

        if(!checkPermission()) return
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val device =
            bluetoothAdapter?.bondedDevices?.filter { device -> device.name ==
                    Config.deviceName }?.getOrNull(0)
        gatt = device?.connectGatt(context, false, bluetoothGattCallback)
    }

    fun fetch() {
        if (!checkPermission()) return
        gatt?.discoverServices()
    }

    @Suppress("DEPRECATION")
    fun save(characteristic: BluetoothGattCharacteristic? ) {
        if (!checkPermission()) return
        gatt?.writeCharacteristic(characteristic)
    }

    private fun checkPermission(): Boolean {
        return if( ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            onCheckPermissionListener?.run()
            false
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(btGatt: BluetoothGatt?, status: Int, newState: Int) {
            if(!checkPermission()) return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = btGatt
                onConnectListener?.run()
                gatt?.discoverServices() // Start discovering services
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt = null
                onDisconnectListener?.run()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if(!checkPermission()) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Fetch correct service
                val service =
                    gatt?.services?.filter { s -> s.uuid.toString() ==
                            Config.serviceUuid }?.getOrNull(0)
                service.let { s ->
                    // Read characteristics value
                    s?.characteristics?.forEach {
                        val entry: ConfigEntry? =
                            Config.entries.filter {
                                    cfg -> cfg.uuid == it?.uuid.toString()}
                                .getOrNull(0)
                        entry?.characteristic = it
                        onCharacteristicFoundListener?.with(entry)
                    }
                    // Start fetching characteristics values one by one
                    gatt?.readCharacteristic(Config.entries.first.characteristic)
                }
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            charac: BluetoothGattCharacteristic,
            status: Int
        ) {
            if(!checkPermission()) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val entry: ConfigEntry? =
                    Config.entries.filter { cfg -> cfg.uuid == charac.uuid.toString()}.getOrNull(0)
                entry?.let {
                    onCharacteristicReadListener?.with(it, charac.getStringValue(0))
                    // Read next
                    val index = Config.entries.indexOf(it)
                    if (index < Config.entries.size - 1) {
                        // Read next
                        gatt.readCharacteristic(Config.entries[index + 1].characteristic)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if(!checkPermission()) return
            if(status == BluetoothGatt.GATT_SUCCESS) {
                val entry: ConfigEntry? =
                    Config.entries.filter { it.characteristic == characteristic }.getOrNull(0)
                onCharacteristicWrittenListener?.with(entry)
                // Save next
                val next: BluetoothGattCharacteristic? =
                    Config.entries.filter { it.unsaved }.getOrNull(0)?.characteristic
                next?.let { gatt.writeCharacteristic(next) }
            }
        }
    }
}