package com.vampiresurvivorslike.enemy

import kotlin.math.sqrt

class EnemyBruiser(
    x: Float,
    y: Float
) : EnemyBase(
    x = x,
    y = y,
    hp = 30,
    atk = 25,
    moveSpeed = 50f,
    expValue = 45
) {
    private var isCharging = false
    private var chargeDirX = 0f
    private var chargeDirY = 0f
    private var chargeCooldown = 2.0f
    private val chargeSpeed = 180f
    private val chargeRange = 120f

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        // function for Bruiser(charge) type
        if (!isAlive) return

        if (isCharging) {
            // 돌진 중
            x += chargeDirX * chargeSpeed * dt
            y += chargeDirY * chargeSpeed * dt

            chargeCooldown -= dt
            if (chargeCooldown <= 0f) {
                isCharging = false
                chargeCooldown = 2.0f
            }

            updateAttackTimer(dt)
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

        // 돌진 범위 도달 → 돌진 준비
        if (distanceTo(targetX, targetY) < chargeRange && chargeCooldown <= 0f) {
            isCharging = true
            chargeDirX = dx / len
            chargeDirY = dy / len
        }

        updateAttackTimer(dt)
    }
}