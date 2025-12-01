package com.vampiresurvivorslike.weapons

import android.graphics.*
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.enemy.EnemyBase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Axe : Weapon {

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

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 90, 60)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 180, 190)
        style = Paint.Style.FILL
    }
    private val headOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
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

            // ★ [충돌 판정] 스윕 반지름 + 적 반지름 안에 적이 있으면 피격
            val hitRange = sweepRadius + e.radius

            if (dx*dx + dy*dy <= hitRange * hitRange) {
                val dead = e.takeDamage(dmg)
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
        val dirX = cos(sweepAngle)
        val dirY = sin(sweepAngle)
        val headX = px + dirX * sweepRadius
        val headY = py + dirY * sweepRadius

        canvas.drawLine(px, py, headX, headY, handlePaint)

        canvas.save()
        canvas.translate(headX, headY)
        canvas.rotate(Math.toDegrees(sweepAngle.toDouble()).toFloat())

        val w = 36f; val h = 26f
        val rect = RectF(-w*0.2f, -h, w, h)
        canvas.drawOval(rect, headPaint)
        canvas.drawOval(rect, headOutline)

        val path = Path().apply {
            moveTo(w, 0f)
            lineTo(w - 10f, -h*0.6f)
            lineTo(w - 10f, h*0.6f)
            close()
        }
        canvas.drawPath(path, headPaint)
        canvas.drawPath(path, headOutline)

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