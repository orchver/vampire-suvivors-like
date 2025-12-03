package com.vampiresurvivorslike.enemy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import com.vampiresurvivorslike.player.Player
import kotlin.math.hypot

class EnemyExploder(
    context: Context, // [추가] 이미지 로딩용
    startX: Float,
    startY: Float,
    multiplier: Float,
    val player: Player
) : EnemyBase(
    x = startX,
    y = startY,
    hp = 20,
    atk = (30 * multiplier).toInt(),
    moveSpeed = 230f,
    expReward = (15 * multiplier).toInt(),
    radius = 20f
) {
    private val bitmap: Bitmap
    private val explosionRadius = 100f
    private var hasExploded = false

    init {
        // 1. 이미지 로드 (enemy_exploder)
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.enemy_exploder)

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