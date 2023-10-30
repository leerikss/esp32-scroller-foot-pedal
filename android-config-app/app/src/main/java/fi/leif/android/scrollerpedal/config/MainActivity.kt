package fi.leif.android.scrollerpedal.config

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.*
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.torrydo.screenez.ScreenEz
import java.util.*

class MainActivity : AppCompatActivity() {

    private var loadButton: Button? = null
    private var saveButton: Button? = null
    private val ble = BLEHandler(this)
    private val scrollPointsAct: ScrollTrackActivity = ScrollTrackActivity()
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Couldn't get correct height without this dependency on my device..
        ScreenEz.with(this)
        screenWidth = ScreenEz.fullWidth
        screenHeight = ScreenEz.fullHeight

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Config.getPointEntryByType(PointEntryType.X1).value = result.data!!.getFloatExtra("x1", 0f)
                Config.getPointEntryByType(PointEntryType.Y1).value = result.data!!.getFloatExtra("y1", 0f)
                Config.getPointEntryByType(PointEntryType.X2).value = result.data!!.getFloatExtra("x2", 0f)
                Config.getPointEntryByType(PointEntryType.Y2).value = result.data!!.getFloatExtra("y2", 0f)
            }
        }
        val scrollTrackButton: Button = findViewById(R.id.scroll_track)
        scrollTrackButton.setOnClickListener {
            val intent = Intent(this, scrollPointsAct.javaClass)
            intent.putExtra("x1", Config.getPointEntryByType(PointEntryType.X1).value)
            intent.putExtra("y1", Config.getPointEntryByType(PointEntryType.Y1).value)
            intent.putExtra("x2", Config.getPointEntryByType(PointEntryType.X2).value)
            intent.putExtra("y2", Config.getPointEntryByType(PointEntryType.Y2).value)
            intent.putExtra("screenWidth", screenWidth)
            intent.putExtra("screenHeight", screenHeight)
            resultLauncher.launch(intent)
        }

        saveButton = findViewById(R.id.save_button)
        saveButton?.setOnClickListener { runOnUiThread { saveValues() } }

        loadButton = findViewById(R.id.load_button)
        loadButton?.setOnClickListener { ble.fetch() }

        initBLEHandler()
    }

    private fun initBLEHandler() {
        ble.setOnConnectListener { runOnUiThread { enableAll(true) } }
        ble.setOnDisconnectListener { runOnUiThread { enableAll(false) } }
        ble.setOnBatchStartListener{ runOnUiThread{ enableAll(false)} }
        ble.setOnBatchFinishListener{ runOnUiThread{ enableAll(true)} }
        ble.setOnCharacteristicReadListener { runOnUiThread { valueReceived(it) } }
        ble.setOnCharacteristicWriteListener { runOnUiThread { enable(it, true) } }
        ble.start()
    }

    private fun valueReceived(cfg: ConfigEntry?) {
        when(cfg) {
            is SliderEntry -> {
                val slider:Slider = findViewById(cfg.viewId)
                slider.value = cfg.value!!
                slider.isEnabled = true
            }
            is PointEntry -> {
                cfg.convertServerValue(screenWidth, screenHeight)
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("DiscouragedApi")
    private fun saveValues() {
        // Prepare values
        Config.entries.forEach {
            when(it) {
                is SliderEntry -> {
                    val slider:Slider = findViewById(it.viewId)
                    it.characteristic?.setValue(slider.value.toString())
                }
                is PointEntry -> {
                    it.characteristic?.setValue(it.getServerValue(screenWidth, screenHeight))
                }
            }
            it.unsaved = true
        }
        // Start saving one by one
        val firstToSave: BluetoothGattCharacteristic? =
            Config.entries.filter { it.unsaved }.getOrNull(0)?.characteristic
        firstToSave?.let { ble.save(firstToSave) }
    }


    private fun enable(cfg: ConfigEntry?, enabled: Boolean) {
        if(cfg is SliderEntry) {
            val slider: Slider = findViewById(cfg.viewId)
            slider.isEnabled = enabled
        }
    }

    private fun enableAll(enabled: Boolean) {
        val setTrackBtn: Button = findViewById(R.id.scroll_track)
        setTrackBtn.isEnabled = enabled
        Config.entries.forEach { enable(it, enabled) }
        val saveBtn: Button = findViewById(R.id.save_button)
        saveBtn.isEnabled = enabled
    }
}
