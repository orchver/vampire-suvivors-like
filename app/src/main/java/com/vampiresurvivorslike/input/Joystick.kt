package com.vampiresurvivorslike.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

class Joystick(
    private val baseRadius: Float = 140f,   // 조이스틱 바닥 반지름 (기존보다 큼)
    private val knobRadius: Float = 60f     // 손잡이 반지름 (기존보다 큼)
) {
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 255, 255) }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 0, 255, 255) }

    private var baseX = 0f
    private var baseY = 0f
    private var knobX = 0f
    private var knobY = 0f

    var axisX = 0f
        private set
    var axisY = 0f
        private set

    private var pointerId: Int? = null
    var isActive = false
        private set

    /** 왼쪽 하단에 고정 위치 설정 */
    fun ensureBase(defaultW: Int, defaultH: Int) {
        baseX = baseRadius + 60f                    // 여백 넉넉히
        baseY = defaultH - (baseRadius + 60f)
        knobX = baseX
        knobY = baseY
        axisX = 0f
        axisY = 0f
    }

    /** 터치 입력 처리 (고정형) */
    fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = e.actionIndex
                if (pointerId == null) {
                    pointerId = e.getPointerId(idx)
                    // ✅ 베이스 위치는 고정, 눌러도 이동하지 않음
                    isActive = true
                    axisX = 0f; axisY = 0f
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                pointerId?.let { pid ->
                    val idx = e.findPointerIndex(pid)
                    if (idx != -1) {
                        val tx = e.getX(idx)
                        val ty = e.getY(idx)
                        val dx = tx - baseX
                        val dy = ty - baseY
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val r = min(dist, baseRadius)
                        val ang = atan2(dy, dx)
                        knobX = baseX + r * kotlin.math.cos(ang)
                        knobY = baseY + r * kotlin.math.sin(ang)
                        axisX = if (baseRadius > 0f) (dx / baseRadius).coerceIn(-1f, 1f) else 0f
                        axisY = if (baseRadius > 0f) (dy / baseRadius).coerceIn(-1f, 1f) else 0f
                        isActive = true
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
                return true
            }
        }
        return false
    }

    /** 손잡이를 원래 위치로 복귀 */
    private fun reset() {
        pointerId = null
        isActive = false
        axisX = 0f
        axisY = 0f
        knobX = baseX
        knobY = baseY
    }

    /** 조이스틱 그리기 (항상 표시) */
    fun draw(canvas: Canvas) {
        // 항상 표시 (고정형)
        canvas.drawCircle(baseX, baseY, baseRadius, basePaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }
}
