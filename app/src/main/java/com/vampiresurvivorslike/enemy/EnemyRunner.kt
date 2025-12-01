package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class EnemyRunner(
    x: Float,
    y: Float,
    multiplier: Float = 1f // [추가]
) : EnemyBase(
    x = x,
    y = y,
    hp = (10 * multiplier).toInt(),  // 기본 10 -> 시간 지날수록 증가
    atk = (5 * multiplier).toInt(),  // 기본 5 -> 시간 지날수록 증가
    moveSpeed = 140f,
    expReward = (10 * multiplier).toInt(),
    radius = 20f
) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
    }

    override fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        val dx = targetX - x
        val dy = targetY - y
        val lenSq = dx * dx + dy * dy
        val len = sqrt(lenSq)

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        updateAttackTimer(dt)
    }
}