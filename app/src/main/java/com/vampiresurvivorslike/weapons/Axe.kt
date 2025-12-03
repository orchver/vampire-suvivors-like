package com.vampiresurvivorslike.weapons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import com.vampiresurvivorslike.enemy.EnemyBase
import com.vampiresurvivorslike.player.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Axe(context: Context) : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 20f
    override var fireIntervalMs: Long = 1500L
    override var piercing: Boolean = true

    private var lastFire = 0L
    private var sweepAngle = 0f
    private var sweeping = false

    private var sweepRadius = 120f
    private var sweepPower = 13f
    private var lifestealRate = 0.10f

    private val bitmap: Bitmap

    init {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.axe)
        // 도끼 크기 조절 (좀 크게)
        val size = 100
        bitmap = Bitmap.createScaledBitmap(raw, size, size, true)
    }

    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        if (!sweeping) {
            if (nowMs - lastFire >= fireIntervalMs) {
                sweeping = true
                sweepAngle = -Math.PI.toFloat()
                lastFire = nowMs
            }
            return
        }

        val delta = (Math.PI * 2 / 30).toFloat()
        sweepAngle += delta

        val dmg = baseDamage * when (level) {
            1 -> 1.3f
            2 -> 1.6f
            3 -> 1.8f
            else -> 1f
        }

        val centerX = player.x
        val centerY = player.y
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (!e.isAlive) continue

            val dx = e.x - centerX
            val dy = e.y - centerY
            val hitRange = sweepRadius + e.radius

            if (dx*dx + dy*dy <= hitRange * hitRange) {
                e.takeDamage(dmg)
                val ang = atan2(dy, dx)
                e.knockback(cos(ang).toFloat(), sin(ang).toFloat(), sweepPower)
                player.heal(dmg * lifestealRate)
            }
        }

        if (sweepAngle >= Math.PI.toFloat()) {
            sweeping = false
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        // 도끼 머리 위치 계산
        val dirX = cos(sweepAngle)
        val dirY = sin(sweepAngle)
        val headX = px + dirX * sweepRadius
        val headY = py + dirY * sweepRadius

        canvas.save()

        // 1. 도끼 위치로 이동
        canvas.translate(headX, headY)

        // 2. 회전 (스윙 각도 + 90도 정도 더해서 날이 밖을 보게)
        val deg = Math.toDegrees(sweepAngle.toDouble()).toFloat() + 45f
        canvas.rotate(deg)

        // 3. 그리기 (중심점 잡기)
        canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, null)

        canvas.restore()
    }

    override fun upgrade() {
        when (level) {
            0 -> { level = 1 }
            1 -> { level = 2 }
            2 -> { level = 3; fireIntervalMs = 1200L }
            else -> { fireIntervalMs = 900L }
        }
        sweepRadius = when (level) {
            1 -> 140f
            2 -> 170f
            else -> 200f
        }
    }
}