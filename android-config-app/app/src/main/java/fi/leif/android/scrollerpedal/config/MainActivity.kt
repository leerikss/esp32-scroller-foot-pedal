package fi.leif.android.scrollerpedal.config

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup.LayoutParams.*
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class MainActivity : AppCompatActivity() {

    private var loadButton: Button? = null
    private var saveButton: Button? = null
    private val ble = BLEHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addUIInputTexts()

        saveButton = findViewById(R.id.save_button)
        saveButton?.setOnClickListener { runOnUiThread { saveValidValues() } }

        loadButton = findViewById(R.id.load_button)
        loadButton?.setOnClickListener { ble.fetch() }

        initBLEHandler()
    }

    @SuppressLint("DiscouragedApi")
    private fun addUIInputTexts() {
        runOnUiThread {

            val view: LinearLayout = findViewById(R.id.layout_input_texts)

            Config.entries.forEach {
                val txtLay = TextInputLayout(this)
                txtLay.hint = getString(resources.getIdentifier(it.name, "string", packageName))
                txtLay.isErrorEnabled = true

                val txtEl = TextInputEditText(this)
                txtEl.isEnabled = false
                txtEl.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                txtLay.addView(txtEl)
                view.addView(txtLay)

                txtEl.layoutParams.height = 110

                it.uiInput = txtEl
            }
        }
    }

    private fun initBLEHandler() {
        ble.setOnCheckPermissionListener {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH),
                1
            )
        }
        ble.setOnConnectListener { runOnUiThread { enableAll(true) } }
        ble.setOnDisconnectListener { runOnUiThread { enableAll(false) } }
        ble.setOnCharacteristicFoundListener { runOnUiThread { it?.uiInput?.isEnabled = false } }
        ble.setOnCharacteristicReadListener { it, value ->
            run {
                runOnUiThread {
                    it?.uiInput?.isEnabled = true
                    it?.uiInput?.setText(value)
                }
            }
        }
        ble.setOnCharacteristicWrittenListener {
            runOnUiThread {
                it?.unsaved = false
                it?.uiInput?.isEnabled = true
            }
        }
        ble.start()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("DiscouragedApi")
    private fun saveValidValues() {
        Config.entries.forEach {
            val ok: Boolean = it.validate()
            if (!ok) {
                it.uiInput?.error =
                    getString(resources.getIdentifier(it.name, "string", packageName))
            } else {
                it.uiInput?.isEnabled = false
                it.characteristic?.setValue(it.uiInput?.text.toString())
                it.uiInput?.error = null
                it.unsaved = true
            }
        }
        // Start saving one by one
        val firstToSave: BluetoothGattCharacteristic? =
            Config.entries.filter { it.unsaved }.getOrNull(0)?.characteristic
        firstToSave?.let { ble.save(firstToSave) }
    }


    private fun enableAll(enabled: Boolean) {
        Config.entries.forEach {
            it.uiInput?.isEnabled = enabled
        }
        loadButton?.isEnabled = enabled
        saveButton?.isEnabled = enabled
    }
}

