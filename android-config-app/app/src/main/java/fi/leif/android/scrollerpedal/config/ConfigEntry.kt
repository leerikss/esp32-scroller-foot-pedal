package fi.leif.android.scrollerpedal.config

import android.bluetooth.BluetoothGattCharacteristic
import android.content.res.Resources

abstract class ConfigEntry(val uuid: String) {
    var characteristic: BluetoothGattCharacteristic? = null
    var unsaved: Boolean = false
    var value: Float? = 0f
}

class SliderEntry(uuid: String, val viewId: Int): ConfigEntry(uuid)

enum class PointEntryType { X1, Y1, X2, Y2 }

class PointEntry(uuid: String, val type: PointEntryType): ConfigEntry(uuid) {

    val serverResolution: Int = 10000

    fun convertServerValue(screenWidth: Int, screenHeight: Int) {
        val len: Int = if(type == PointEntryType.X1 || type == PointEntryType.X2) screenWidth else screenHeight
        value = (value?.div(serverResolution))?.times(len)
    }

    fun getServerValue(screenWidth: Int, screenHeight: Int): String {
        val len: Int = if(type == PointEntryType.X1 || type == PointEntryType.X2) screenWidth else screenHeight
        val v = this.value!!.div(len)
        return (v * serverResolution).toInt().toString()
    }

}

