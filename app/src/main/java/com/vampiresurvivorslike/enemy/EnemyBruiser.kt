package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class EnemyBruiser(
    x: Float,
    y: Float,
    multiplier: Float = 1f // [추가] 기본값 1f
) : EnemyBase(
    x = x,
    y = y,
    hp = (50 * multiplier).toInt(),  // [수정] 배율 적용
    atk = (15 * multiplier).toInt(), // [수정] 배율 적용
    moveSpeed = 110f,
    expReward = (50 * multiplier).toInt(), // [선택] 경험치도 배율 적용
    radius = 35f
) {
    // --- [1] 외형 관련 ---
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 0, 0) // 진한 빨강
    }

    // --- [2] 돌진 관련 변수 ---
    private var isCharging = false
    private var chargeDirX = 0f
    private var chargeDirY = 0f

    private var chargeCooldownTimer = 0f
    private val chargeCooldownMax = 2.0f

    private var currentChargeDuration = 0f
    private val maxChargeDuration = 0.8f

    private val chargeSpeed = 300f
    private val chargeRange = 250f

    override fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        // 1. 돌진 상태일 때 로직
        if (isCharging) {
            x += chargeDirX * chargeSpeed * dt
            y += chargeDirY * chargeSpeed * dt

            currentChargeDuration -= dt
            if (currentChargeDuration <= 0f) {
                isCharging = false
                chargeCooldownTimer = chargeCooldownMax
            }
            updateAttackTimer(dt)
            return
        }

        // 2. 일반 상태 (추적 + 쿨타임 관리)
        if (chargeCooldownTimer > 0f) {
            chargeCooldownTimer -= dt
        }

        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        // 3. 돌진 조건 체크
        if (len < chargeRange && chargeCooldownTimer <= 0f && len > 0f) {
            isCharging = true
            currentChargeDuration = maxChargeDuration
            chargeDirX = dx / len
            chargeDirY = dy / len
        }

        updateAttackTimer(dt)
    }
}