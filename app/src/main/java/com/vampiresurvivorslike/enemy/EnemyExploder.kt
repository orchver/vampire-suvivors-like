package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class EnemyExploder(
    x: Float,
    y: Float,
    multiplier: Float = 1f // [추가]
) : EnemyBase(
    x = x,
    y = y,
    hp = (5 * multiplier).toInt(),
    atk = (30 * multiplier).toInt(), // 자폭 데미지도 시간 지날수록 강력해짐
    moveSpeed = 160f,
    expReward = (15 * multiplier).toInt(),
    radius = 20f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    override fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        // 자폭 로직 (거리가 가까우면 멈추거나 터질 준비 등)
        // 현재는 이동만 구현됨

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }
        updateAttackTimer(dt)
    }
}