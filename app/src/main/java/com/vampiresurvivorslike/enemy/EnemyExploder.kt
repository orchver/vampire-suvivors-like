package com.vampiresurvivorslike.enemy

import kotlin.math.sqrt

class EnemyExploder(
    x: Float,
    y: Float
) : EnemyBase(
    x = x,
    y = y,
    hp = 10,
    atk = 35,
    moveSpeed = 95f,
    expValue = 25
) {
    private val explodeRange = 45f //explode range, can be adjusted

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        // function for Explode type
        if (!isAlive) return

        val dist = distanceTo(targetX, targetY)

        // 폭발 조건
        if (dist <= explodeRange) {
            explode()
            return
        }

        // 일반 추적
        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        if (len != 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        updateAttackTimer(dt)
    }

    private fun explode() {
        // 나중에 Player.takeDamage(atk) 연결
        isAlive = false
    }
}
