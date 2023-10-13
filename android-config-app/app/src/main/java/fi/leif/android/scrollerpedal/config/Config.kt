package fi.leif.android.scrollerpedal.config

import java.util.*

object Config {

    const val serviceUuid: String = "a89d0000-543c-4855-be30-f2270a00a83b"
    const val deviceName: String = "Scroller Pedal"

    var entries: LinkedList<ConfigEntry> = LinkedList()

    init {
        entries.add(ConfigEntry("a89d0001-543c-4855-be30-f2270a00a83b", "action_time", Validate.BETWEEN, 100, 5000))
        entries.add(ConfigEntry("a89d0002-543c-4855-be30-f2270a00a83b", "x", Validate.BETWEEN,0, 10000))
        entries.add(ConfigEntry("a89d0003-543c-4855-be30-f2270a00a83b", "y_top", Validate.GTE1_LTE2, 0, "y_bottom"))
        entries.add(ConfigEntry("a89d0004-543c-4855-be30-f2270a00a83b", "y_bottom", Validate.LTE1_GTE2, 10000,"y_top"))
        entries.add(ConfigEntry("a89d0005-543c-4855-be30-f2270a00a83b", "scroll_amount", Validate.GTE1_LTEDIFF, 1, "y_bottom", "y_top"))
        entries.add(ConfigEntry("a89d0006-543c-4855-be30-f2270a00a83b", "scroll_delay", Validate.BETWEEN, 10, 10000))
        entries.add(ConfigEntry("a89d0007-543c-4855-be30-f2270a00a83b", "page_scroll_amount", Validate.GTE1_LTEDIFF, 1, "y_bottom", "y_top"))
        entries.add(ConfigEntry("a89d0008-543c-4855-be30-f2270a00a83b", "page_scroll_steps", Validate.BETWEEN, 1, 100))
    }
}