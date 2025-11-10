package com.vampiresurvivorslike

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.hypot

class ExpOrb(
    var x: Float,
    var y: Float,
    val value: Int,
    val radius: Float = 12f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
    }

    fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    /** 플레이어와의 충돌(먹기) 판정 */
    fun isCollected(px: Float, py: Float, pr: Float): Boolean {
        val dx = px - x
        val dy = py - y
        val rr = radius + pr
        return dx * dx + dy * dy <= rr * rr
    }
}
