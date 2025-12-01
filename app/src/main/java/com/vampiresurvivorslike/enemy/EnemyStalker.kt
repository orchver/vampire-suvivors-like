package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class EnemyStalker(
    x: Float,
    y: Float,
    multiplier: Float = 1f // [추가]
) : EnemyBase(
    x = x,
    y = y,
    hp = (20 * multiplier).toInt(),
    atk = (8 * multiplier).toInt(),
    moveSpeed = 80f,
    expReward = (20 * multiplier).toInt(),
    radius = 25f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
    }

    override fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        updateAttackTimer(dt)
    }
}