package com.vampiresurvivorslike.enemy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import kotlin.math.sqrt

class EnemyRunner(
    context: Context, // [추가] 이미지 로딩용
    x: Float,
    y: Float,
    multiplier: Float = 1f
) : EnemyBase(
    x = x,
    y = y,
    hp = (10 * multiplier).toInt(),
    atk = (5 * multiplier).toInt(),
    moveSpeed = 140f,
    expReward = (10 * multiplier).toInt(),
    radius = 20f
) {
    private val bitmap: Bitmap

    init {
        // 1. 이미지 로드 (enemy_runner)
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.enemy_runner)

        // 2. 스프라이트 시트 첫 프레임 자르기 (4등분)
        val fw = raw.width / 4
        val fh = raw.height
        val crop = Bitmap.createBitmap(raw, 0, 0, fw, fh)

        // 3. 크기 조절
        val size = (radius * 2*6).toInt()
        bitmap = Bitmap.createScaledBitmap(crop, size, size, true)
    }

    override fun draw(c: Canvas) {
        // 비트맵 그리기
        c.drawBitmap(bitmap, x - radius, y - radius, null)
    }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        if (!isAlive) return

        val dx = targetX - x
        val dy = targetY - y
        val lenSq = dx * dx + dy * dy
        val len = sqrt(lenSq)

        if (len > 0f) {
            x += (dx / len) * moveSpeed * dt
            y += (dy / len) * moveSpeed * dt
        }

        updateAttackTimer(dt)
    }
}