package com.vampiresurvivorslike.enemy

import kotlin.math.sqrt

class EnemyStalker(
    x: Float,
    y: Float
) : EnemyBase(
    x = x,
    y = y,
    hp = 20,
    atk = 15,
    moveSpeed = 85f,
    expValue = 30
) {
    // function for Stalker type
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