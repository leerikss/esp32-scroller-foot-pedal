package fi.leif.android.scrollerpedal.config

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import fi.leif.android.scrollerpedal.config.Point as Point1


class ScrollTrackActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        val x1 = intent.getFloatExtra("x1", 0f)
        val y1 = intent.getFloatExtra("y1", 0f)
        val x2 = intent.getFloatExtra("x2", 0f)
        val y2 = intent.getFloatExtra("y2", 0f)
        val screenWidth = intent.getIntExtra("screenWidth", 0)
        val screenHeight = intent.getIntExtra("screenHeight", 0)

        val view = ScrollTrackView(this, Point1(x1,y1), Point1(x2,y2), screenWidth, screenHeight)
        setContentView(view)
    }


}