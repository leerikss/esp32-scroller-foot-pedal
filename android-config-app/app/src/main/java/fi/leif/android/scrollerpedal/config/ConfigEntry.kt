package fi.leif.android.scrollerpedal.config

import android.bluetooth.BluetoothGattCharacteristic
import com.google.android.material.textfield.TextInputEditText

enum class Validate {BETWEEN, LTE1_GTE2, GTE1_LTE2, GTE1_LTEDIFF}

class ConfigEntry(val uuid: String, val name: String, private val validationType: Validate) {
    var uiInput: TextInputEditText? = null
    var characteristic: BluetoothGattCharacteristic? = null
    var unsaved: Boolean = false
    private var val1: Int? = null
    private var val2: Int? = null
    private var valId1: String? = null
    private var valId2: String? = null

    // Constructor for Validate.BETWEEN
    constructor(uuid: String, id: String, validationType: Validate, minValue: Int, maxValue: Int): this(uuid, id, validationType) {
        val1 = minValue
        val2 = maxValue
    }

    // Constructor for Validate.LTE1_GTE2 & Validate.GTE2_LTE2
    constructor(uuid: String, id: String, validationType: Validate, value1: Int, compareToId1: String): this(uuid, id, validationType) {
        val1 = value1
        valId1 = compareToId1
    }

    // Constructor for GTE1_AND_LTEDIFF
    constructor(uuid: String, id: String, validationType: Validate, minValue: Int, compareToId1: String, compareToId2: String): this(uuid, id, validationType) {
        val1 = minValue
        valId1 = compareToId1
        valId2 = compareToId2
    }

    fun validate(): Boolean {
        val value: Int = uiInput?.text.toString().toIntOrNull() ?: return false
        return when(validationType) {
            Validate.BETWEEN -> (value in (val1!! until val2!!+1))
            Validate.LTE1_GTE2 -> (value <= val1!! && value >= (getIntValById(valId1)))
            Validate.GTE1_LTE2 -> (value >= val1!! && value <= (getIntValById(valId1)))
            Validate.GTE1_LTEDIFF -> {
                (value >= val1!! && value <= (getIntValById(valId1)-getIntValById(valId2)))
            }
        }
    }

    private fun getIntValById(value: String?): Int {
        return Config.entries.filter { it.name == value }[0].uiInput?.text.toString().toInt()
    }
}

