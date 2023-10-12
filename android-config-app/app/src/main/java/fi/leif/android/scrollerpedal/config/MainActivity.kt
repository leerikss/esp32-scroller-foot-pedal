package fi.leif.android.scrollerpedal.config

import android.annotation.SuppressLint
import android.bluetooth.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.ViewGroup.LayoutParams.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private var gatt: BluetoothGatt? = null

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addUIInputTexts()

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            saveValidValues()
        }

        val loadButton: Button = findViewById(R.id.load_button)
        loadButton.setOnClickListener{
            gatt?.discoverServices()
        }

        connectToPairedDevice()
    }

    @SuppressLint("DiscouragedApi")
    private fun addUIInputTexts() {
        this@MainActivity.runOnUiThread {

            val view: LinearLayout = findViewById(R.id.layout_input_texts)

            Config.entries.forEach {
                val txtLay = TextInputLayout(this)
                txtLay.hint = getString(resources.getIdentifier(it.id, "string", packageName))
                txtLay.isErrorEnabled = true

                val txtEl = TextInputEditText(this)
                txtEl.isEnabled = false
                txtEl.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                txtLay.addView(txtEl)
                view.addView(txtLay)

                txtEl.layoutParams.height = 110

                it.uiEl = txtEl
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun saveValidValues() {
        // Avoid the "Animators may only be run on Looper threads" by running a thread
        this@MainActivity.runOnUiThread {
            Config.entries.forEach {
                val ok:Boolean = it.validate()
                if(!ok) {
                    it.uiEl?.error = getString(resources.getIdentifier(it.id, "string", packageName))
                } else {
                    it.uiEl?.isEnabled = false
                    it.gattCharacter?.setValue(it.uiEl?.text.toString())
                    it.save = true
                }
            }
            // Start saving one by one
            val firstToSave: BluetoothGattCharacteristic? = Config.entries.filter { it.save }.getOrNull(0)?.gattCharacter
            firstToSave?.let { gatt?.writeCharacteristic(firstToSave) }
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connectToPairedDevice() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter?.bondedDevices?.filter { device -> device.name == Config.deviceName }?.getOrNull(0)
        device?.let { it.connectGatt(this, false, bluetoothGattCallback) } ?: {
            Handler().postDelayed({
                connectToPairedDevice()
            }, 2000)
        }
    }

    private fun enableAll(enabled: Boolean) {
        this@MainActivity.runOnUiThread {
            Config.entries.forEach{
                it.uiEl?.isEnabled = enabled
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onConnectionStateChange(btGatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Start discovering services
                gatt = btGatt
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                enableAll(false)
                connectToPairedDevice()
            }
        }

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Fetch correct service
                val service =
                    gatt?.services?.filter { s -> s.uuid.toString() == Config.serviceUuid }?.getOrNull(0)
                service.let { s ->
                    // Read characteristics value
                    s?.characteristics?.forEach {
                        val entry: ConfigEntry? = Config.entries.filter { cfg -> cfg.uuid == it?.uuid.toString()}.getOrNull(0)
                        entry?.gattCharacter = it
                        // Update UI
                        this@MainActivity.runOnUiThread {
                            entry?.uiEl?.isEnabled = false
                        }
                    }
                    // Start fetching characteristics values one by one
                    gatt?.readCharacteristic(Config.entries.first.gattCharacter)
                }
            }
        }

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            charac: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val entry: ConfigEntry? = Config.entries.filter { cfg -> cfg.uuid == charac.uuid.toString()}.getOrNull(0)
                entry?.let {
                    // Update text value
                    this@MainActivity.runOnUiThread {
                        it.uiEl?.isEnabled = true
                        it.uiEl?.setText(charac.getStringValue(0))
                    }
                    // Read next
                    val index = Config.entries.indexOf(it)
                    if (index < Config.entries.size - 1) {
                        gatt.readCharacteristic(Config.entries[index + 1].gattCharacter)
                    }
                }
            }
        }

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                val entry: ConfigEntry? = Config.entries.filter { it.gattCharacter == characteristic }.getOrNull(0)

                // Update input text
                this@MainActivity.runOnUiThread {
                    entry?.save = false
                    entry?.uiEl?.isEnabled = true
                }

                // Save next
                val next: BluetoothGattCharacteristic? = Config.entries.filter { it.save }.getOrNull(0)?.gattCharacter
                next?.let { gatt.writeCharacteristic(next) }
            }
        }
    }
}

