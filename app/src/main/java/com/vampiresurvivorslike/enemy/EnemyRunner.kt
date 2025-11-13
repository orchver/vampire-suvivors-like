package com.vampiresurvivorslike.enemy

import kotlin.math.sqrt

class EnemyRunner(
    x: Float,
    y: Float
) : EnemyBase(
    x = x,
    y = y,
    hp = 10,
    atk = 5,
    moveSpeed = 140f,   // high speed
    expValue = 10
) {
    // function for runner type
    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        if (len != 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        updateAttackTimer(dt)
    }
}