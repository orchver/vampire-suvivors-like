package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.vampiresurvivorslike.player.Player
import kotlin.math.hypot

class EnemyExploder(
    startX: Float,
    startY: Float,
    multiplier: Float, // ★ 시간 비례 배율 다시 추가
    val player: Player
) : EnemyBase(
    x = startX,
    y = startY,
    hp = 20,                         // ★ [고정] 체력은 언제나 20 (스치면 사망)
    atk = (30 * multiplier).toInt(), // ★ [변동] 공격력은 시간 비례 증가
    moveSpeed = 230f,
    expReward = (15 * multiplier).toInt(), // ★ [변동] 경험치도 시간 비례 증가
    radius = 20f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private val explosionRadius = 100f
    private var hasExploded = false

    override fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive || hasExploded) return

        // 1. 이동
        val dx = targetX - x
        val dy = targetY - y
        val dist = hypot(dx, dy).coerceAtLeast(1f)

        if (dist > 1f) {
            x += (dx / dist) * moveSpeed * dt
            y += (dy / dist) * moveSpeed * dt
        }

        updateAttackTimer(dt)

        // 2. [충돌 체크] 플레이어와 닿으면 즉시 자폭
        val contactDist = radius + player.radius
        if (dist <= contactDist) {
            explode()
        }
    }

    // 3. [사망 체크] 맞아 죽어도 자폭
    override fun takeDamage(damage: Float): Boolean {
        if (!isAlive) return true

        hp -= damage.toInt()

        if (hp <= 0) {
            explode()
            return true
        }
        return false
    }

    private fun explode() {
        if (hasExploded) return
        hasExploded = true

        // 폭발 범위 판정
        val dist = hypot(player.x - x, player.y - y)
        if (dist <= explosionRadius + player.radius) {
            player.takeDamage(atk.toFloat())
        }

        die()
    }
}