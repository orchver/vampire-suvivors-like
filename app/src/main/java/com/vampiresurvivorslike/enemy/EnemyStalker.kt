package com.vampiresurvivorslike.enemy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import kotlin.math.sqrt

class EnemyStalker(
    context: Context,
    x: Float,
    y: Float,
    multiplier: Float = 1f
) : EnemyBase(
    x = x,
    y = y,
    hp = (20 * multiplier).toInt(),
    atk = (8 * multiplier).toInt(),
    moveSpeed = 80f,
    expReward = (20 * multiplier).toInt(),
    radius = 25f
) {
    private val bitmap: Bitmap

    init {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.enemy_stalker)

        // ⭐ [수정] 이미지가 가로 6칸, 세로 4칸입니다.
        val frameWidth = raw.width / 6
        val frameHeight = raw.height / 4

        // 맨 왼쪽 위(0, 0)에 있는 첫 번째 프레임만 잘라냅니다.
        val crop = Bitmap.createBitmap(raw, 0, 0, frameWidth, frameHeight)

        // 3배 크기로 확대
        val size = (radius * 2 * 3).toInt()
        bitmap = Bitmap.createScaledBitmap(crop, size, size, true)
    }

    override fun draw(c: Canvas) {
        // 중심점 보정하여 그리기
        c.drawBitmap(bitmap, x - bitmap.width / 2f, y - bitmap.height / 2f, null)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return
        val dx = targetX - x
        val dy = targetY - y
        val len = sqrt(dx * dx + dy * dy)
        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }
        updateAttackTimer(dt)
    }
}