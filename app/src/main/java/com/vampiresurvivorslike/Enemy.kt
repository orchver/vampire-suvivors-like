package com.vampiresurvivorslike

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.hypot

class Enemy(
    var x: Float,
    var y: Float,
    var hp: Float = 5f,           // 체력
    private val speed: Float = 1.4f,
    val radius: Float = 20f,
    val expReward: Int = 10       // ★ 죽었을 때 주는 경험치(기본 10)
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }

    // ★ 바깥에서 제거하기 위한 플래그
    var isDead: Boolean = false
        private set

    fun update(targetX: Float, targetY: Float) {
        val dx = targetX - x
        val dy = targetY - y
        val d = hypot(dx, dy).coerceAtLeast(0.0001f)
        x += speed * (dx / d)
        y += speed * (dy / d)
    }

    fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    /** 피해를 받고 사망 여부 반환 + isDead 플래그 설정 */
    fun takeDamage(damage: Float): Boolean {
        if (isDead) return true  // 이미 죽은 애는 무시
        hp -= damage
        if (hp <= 0f) {
            isDead = true
        }
        return isDead
    }

    fun knockback(dirX: Float, dirY: Float, power: Float) {
        val len = hypot(dirX, dirY).coerceAtLeast(0.0001f)
        val nx = dirX / len
        val ny = dirY / len
        x += nx * power
        y += ny * power
    }

    fun hitCircle(cx: Float, cy: Float, r: Float, damage: Float): Boolean {
        if (hypot(cx - x, cy - y) <= r + radius) {
            return takeDamage(damage)
        }
        return false
    }
}
