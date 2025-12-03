package com.vampiresurvivorslike.enemy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import kotlin.math.sqrt

class EnemyBruiser(
    context: Context,
    x: Float,
    y: Float,
    multiplier: Float = 1f
) : EnemyBase(
    x = x,
    y = y,
    hp = (50 * multiplier).toInt(),
    atk = (15 * multiplier).toInt(),
    moveSpeed = 110f,
    expReward = (50 * multiplier).toInt(),
    radius = 35f
) {
    private val bitmap: Bitmap

    // 돌진 변수들 (기존 로직 유지)
    private var isCharging = false
    private var chargeDirX = 0f
    private var chargeDirY = 0f
    private var chargeCooldownTimer = 0f
    private val chargeCooldownMax = 2.0f
    private var currentChargeDuration = 0f
    private val maxChargeDuration = 0.8f
    private val chargeSpeed = 300f
    private val chargeRange = 250f

    init {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.enemy_bruiser)

        // ⭐ [수정] Bruiser 이미지는 6열 4행입니다.
        val frameWidth = raw.width / 6
        val frameHeight = raw.height / 4

        // 맨 왼쪽 위 첫 번째 프레임만 잘라냅니다.
        val crop = Bitmap.createBitmap(raw, 0, 0, frameWidth, frameHeight)

        // 3배로 키우기
        val size = (radius * 2 * 3).toInt()
        bitmap = Bitmap.createScaledBitmap(crop, size, size, true)
    }

    override fun draw(c: Canvas) {
        // 중심점 보정
        c.drawBitmap(bitmap, x - bitmap.width / 2f, y - bitmap.height / 2f, null)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        // 돌진 로직 (그대로 유지)
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

        if (chargeCooldownTimer > 0f) chargeCooldownTimer -= dt

        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        if (len < chargeRange && chargeCooldownTimer <= 0f && len > 0f) {
            isCharging = true
            currentChargeDuration = maxChargeDuration
            chargeDirX = dx / len
            chargeDirY = dy / len
        }
        updateAttackTimer(dt)
    }
}