package fi.leif.android.scrollerpedal.config

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BLEHandler(private val context: Context) {

    private var gatt: BluetoothGatt? = null
    private var reconnect: ScheduledFuture<*>? = null

    private var onConnectListener: Runnable? = null
    fun setOnConnectListener(impl: Runnable) { onConnectListener = impl }

    private var onDisconnectListener: Runnable? = null
    fun setOnDisconnectListener(impl: Runnable) { onDisconnectListener = impl }

    private var onBatchStartListener: Runnable? = null
    fun setOnBatchStartListener(impl: Runnable) { onBatchStartListener = impl }

    private var onBatchFinishListener: Runnable? = null
    fun setOnBatchFinishListener(impl: Runnable) { onBatchFinishListener = impl }

    fun interface ConfigEntryConsumer { fun with(entry: ConfigEntry?) }
    private var onCharacteristicReadListener: ConfigEntryConsumer? = null
    fun setOnCharacteristicReadListener(impl: ConfigEntryConsumer) { onCharacteristicReadListener = impl}

    private var onCharacteristicWriteListener: ConfigEntryConsumer? = null
    fun setOnCharacteristicWriteListener(impl: ConfigEntryConsumer) { onCharacteristicWriteListener = impl}

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
        onBatchStartListener?.run()
        gatt?.discoverServices()
    }

    @Suppress("DEPRECATION")
    fun save(characteristic: BluetoothGattCharacteristic? ) {
        if (!checkPermission()) return
        onBatchStartListener?.run()
        gatt?.writeCharacteristic(characteristic)
    }

    private fun checkPermission(): Boolean {
        val permission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.BLUETOOTH
        return ( ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED)
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
                    it.value = charac.getStringValue(0).toFloat()
                    onCharacteristicReadListener?.with(it)
                    // Read next
                    val index = Config.entries.indexOf(it)
                    if (index < Config.entries.size - 1) {
                        // Read next
                        gatt.readCharacteristic(Config.entries[index + 1].characteristic)
                    } else {
                        onBatchFinishListener?.run()
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
                entry?.unsaved = false
                onCharacteristicWriteListener?.with(entry)

                // Save next
                val next: BluetoothGattCharacteristic? =
                    Config.entries.filter { it.unsaved }.getOrNull(0)?.characteristic
                next?.let { gatt.writeCharacteristic(next) } ?: onBatchFinishListener?.run()
            }
        }
    }
}