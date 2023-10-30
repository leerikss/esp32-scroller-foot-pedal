package fi.leif.android.scrollerpedal.config

import java.util.*

object Config {

    const val serviceUuid: String = "a89d0000-543c-4855-be30-f2270a00a83b"
    const val deviceName: String = "Scroller Pedal"

    var entries: LinkedList<ConfigEntry> = LinkedList()

    fun getPointEntryByType(type: PointEntryType): ConfigEntry {
        return entries.filter { (it is PointEntry) && it.type == type }[0]
    }

    init {
        entries.add(PointEntry("a89d0001-543c-4855-be30-f2270a00a83b", PointEntryType.X1))
        entries.add(PointEntry("a89d0002-543c-4855-be30-f2270a00a83b", PointEntryType.Y1))
        entries.add(PointEntry("a89d0003-543c-4855-be30-f2270a00a83b", PointEntryType.X2))
        entries.add(PointEntry("a89d0004-543c-4855-be30-f2270a00a83b", PointEntryType.Y2))
        entries.add(SliderEntry("a89d0005-543c-4855-be30-f2270a00a83b", R.id.action_time))
        entries.add(SliderEntry("a89d0006-543c-4855-be30-f2270a00a83b", R.id.scroll_amount))
        entries.add(SliderEntry("a89d0007-543c-4855-be30-f2270a00a83b", R.id.scroll_delay))
        entries.add(SliderEntry("a89d0008-543c-4855-be30-f2270a00a83b", R.id.page_scroll_amount))
        entries.add(SliderEntry("a89d0009-543c-4855-be30-f2270a00a83b", R.id.page_scroll_steps))
    }
}