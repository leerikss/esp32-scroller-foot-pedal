package fi.leif.android.scrollerpedal.config

import android.bluetooth.BluetoothGattCharacteristic
import com.google.android.material.textfield.TextInputEditText
import java.util.LinkedList

class ConfigEntry(val uuid: String, val id: String, private val validationType: Validate) {
    var uiEl: TextInputEditText? = null
    var gattCharacter: BluetoothGattCharacteristic? = null
    var save: Boolean = false
    private var minVal: Int? = null
    private var maxVal: Int? = null
    private var valId1: String? = null
    private var valId2: String? = null

    constructor(uuid: String, id: String, validationType: Validate, minValue: Int, maxValue: Int): this(uuid, id, validationType) {
        minVal = minValue
        maxVal = maxValue
    }

    constructor(uuid: String, id: String, validationType: Validate, compareToId1: String): this(uuid, id, validationType) {
        valId1 = compareToId1
    }

    constructor(uuid: String, id: String, validationType: Validate, minValue: Int, compareToId1: String, compareToId2: String): this(uuid, id, validationType) {
        minVal = minValue
        valId1 = compareToId1
        valId2 = compareToId2
    }

    fun validate(): Boolean {
        val value: Int = uiEl?.text.toString().toIntOrNull() ?: return false
        return when(validationType) {
            Validate.IN -> Validator.valueIn(value, minVal!!, maxVal!!)
            Validate.LTE -> Validator.valueLTE(value, getIntValById(valId1))
            Validate.GTE -> Validator.valueGTE(value, getIntValById(valId1))
            Validate.GTE_AND_LTE_DIFF -> Validator.valueIn(value, minVal!!, (getIntValById(valId1)-getIntValById(valId2)))
        }
    }

    private fun getIntValById(value: String?): Int {
        return Config.entries.filter { it.id == value }[0].uiEl?.text.toString().toInt()
    }
}

enum class Validate {IN, LTE, GTE, GTE_AND_LTE_DIFF}

object Validator {
    fun valueIn(value: Int, minValue: Int, maxValue: Int): Boolean {
        return (value in (minValue until maxValue+1))
    }
    fun valueLTE(value: Int, maxValue: Int): Boolean {
        return (value <= maxValue)
    }
    fun valueGTE(value: Int, minValue: Int): Boolean {
        return (value >= minValue)
    }
}

object Config {

    const val serviceUuid: String = "a89d0000-543c-4855-be30-f2270a00a83b"
    const val deviceName: String = "Scroller Pedal"

    var entries: LinkedList<ConfigEntry> = LinkedList()

    init {
        entries.add(ConfigEntry("a89d0001-543c-4855-be30-f2270a00a83b", "action_time", Validate.IN, 100, 5000))
        entries.add(ConfigEntry("a89d0002-543c-4855-be30-f2270a00a83b", "x", Validate.IN,0, 10000))
        entries.add(ConfigEntry("a89d0003-543c-4855-be30-f2270a00a83b", "y_top", Validate.LTE, "y_bottom"))
        entries.add(ConfigEntry("a89d0004-543c-4855-be30-f2270a00a83b", "y_bottom", Validate.GTE, "y_top"))
        entries.add(ConfigEntry("a89d0005-543c-4855-be30-f2270a00a83b", "scroll_amount", Validate.GTE_AND_LTE_DIFF, 0, "y_bottom", "y_top"))
        entries.add(ConfigEntry("a89d0006-543c-4855-be30-f2270a00a83b", "scroll_delay", Validate.IN, 10, 10000))
        entries.add(ConfigEntry("a89d0007-543c-4855-be30-f2270a00a83b", "page_scroll_amount", Validate.GTE_AND_LTE_DIFF, 0, "y_bottom", "y_top"))
        entries.add(ConfigEntry("a89d0008-543c-4855-be30-f2270a00a83b", "page_scroll_steps", Validate.IN, 1, 100))
    }
}
