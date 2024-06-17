package fi.leif.android.scrollerpedal.config

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat

class Point(var x: Float, var y: Float)

class ScrollTrackView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val radius = 150f

    private lateinit var p1: Point
    private lateinit var p2: Point
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val paintP1 = getCirclePaint()
    private val paintP2 = getCirclePaint()
    private val paintArrow = getArrowPaint()

    private enum class Move {P1, P2, ALL, NONE}
    private var mover: Move = Move.NONE
    private var dx1 = 0f
    private var dy1 = 0f
    private var dx2 = 0f
    private var dy2 = 0f

    private val infoTextSize = 50f
    private val infoLineHeight = 80f
    private val infoTextPaint = Paint().apply {
        color = Color.GRAY
        typeface = ResourcesCompat.getFont(context, R.font.bad_script_regular)
        textSize = infoTextSize
        isAntiAlias = true

    }
    private val infoText = context.resources.getString(R.string.scroll_track_info)

    constructor(context: Context, point1: Point, point2: Point, screenWidth: Int, screenHeight: Int) : this(context, null) {
        this.p1 = point1
        this.p2 = point2
        setRadialGradient(paintP1, p1)
        setRadialGradient(paintP2, p2)

        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
    }

    private fun getCirclePaint(): Paint {
        return Paint()
    }

    private fun setRadialGradient(paint: Paint, p: Point) {
        paint.apply { shader = RadialGradient(p.x, p.y, radius, Color.DKGRAY, Color.TRANSPARENT, Shader.TileMode.MIRROR) }
    }

    private fun getArrowPaint(): Paint {
        val color = Color.RED
        val paint = Paint()
        paint.color = color
        paint.isAntiAlias = true
        paint.strokeWidth = 10f
        paint.setShadowLayer(20f,0f,0f,color)
        return paint
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        infoText.split("\n").forEachIndexed { index, line ->
            canvas.drawText(line, infoTextSize, infoTextSize + infoLineHeight * (index + 1), infoTextPaint)
        }

        canvas.drawCircle(p1.x, p1.y, radius, paintP1)
        canvas.drawCircle(p2.x, p2.y, radius, paintP2)

        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintArrow)
        val angle: Float = angleBetweenPoints(p1, p2)
        drawTriangle(canvas, paintArrow, p2.x, p2.y, angle)
    }

    private fun angleBetweenPoints(a: Point, b: Point): Float {
        val deltaY = b.y - a.y
        val deltaX = b.x - a.x
        return Math.toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
    }

    private fun drawTriangle(canvas: Canvas, paint: Paint?, x: Float, y: Float, angle: Float, thinWidth: Int=10, wideWidth: Int=50) {
        val halfThinWidth = thinWidth / 2
        val halfWideWidth = wideWidth / 2

        val path = Path()
        path.moveTo(x-halfThinWidth, y) // Top left
        path.lineTo((x - halfWideWidth), (y + wideWidth)) // Bottom left
        path.lineTo((x + halfWideWidth), (y + wideWidth)) // Bottom right
        path.lineTo(x+halfThinWidth, y) // Top right
        path.lineTo(x-halfThinWidth, y) // Back to Top left
        path.close()

        // Rotate
        canvas.save()
        canvas.rotate(angle+90f, x, y)
        canvas.drawPath(path, paint!!)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dx1 = x - p1.x
                dy1 = y - p1.y
                dx2 = x - p2.x
                dy2 = y - p2.y

                // Move first circle
                if(withinBounds(x,y,p1.x-radius, p1.x+radius, p1.y-radius, p1.y+radius)) {
                    mover = Move.P1
                }
                // Move second circle
                else if(withinBounds(x,y,p2.x-radius, p2.x+radius, p2.y-radius, p2.y+radius)) {
                    mover = Move.P2
                }
                else {
                    val lx = p1.x.coerceAtMost(p2.x)-radius
                    val rx = p1.x.coerceAtLeast(p2.x)+radius
                    val ty = p1.y.coerceAtMost(p2.y)-radius
                    val by = p1.y.coerceAtLeast(p2.y)+radius
                    // Touched somewhere within => move all elements
                    if(withinBounds(x,y,lx,rx,ty,by)) {
                        mover = Move.ALL
                    }
                    // Touched outside => finish activity and return values
                    else {
                        val act: ScrollTrackActivity = context as ScrollTrackActivity

                        act.intent.putExtra("x1", p1.x)
                        act.intent.putExtra("y1", p1.y)
                        act.intent.putExtra("x2", p2.x)
                        act.intent.putExtra("y2", p2.y)

                        act.setResult(RESULT_OK, act.intent)
                        act.finish()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(mover == Move.P1 || mover == Move.ALL) {
                    setXY(p1, x - dx1, y - dy1)
                    setRadialGradient(paintP1, p1)
                }
                if(mover == Move.P2 || mover == Move.ALL) {
                    setXY(p2,x - dx2,y - dy2)
                    setRadialGradient(paintP2, p2)
                }
                if(mover != Move.NONE) {
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                mover = Move.NONE
            }
        }
        return true
    }

    private fun withinBounds(x: Float, y: Float, lx: Float, rx: Float, ty: Float, by: Float): Boolean {
        return (x in lx..rx && y in ty..by)
    }

    private fun setXY(el: Point?, x: Float, y: Float) {
        el?.x = if(x<0) 0f else if(x>screenWidth) screenWidth.toFloat() else x
        el?.y = if(y<0) 0f else if(y>screenHeight) screenHeight.toFloat() else y
    }
}