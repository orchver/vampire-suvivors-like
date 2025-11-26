package com.vampiresurvivorslike.weapons

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.enemy.EnemyBase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Bow : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 10f
    override var fireIntervalMs: Long = 1000L
    override var piercing: Boolean = true

    private var lastFire = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW }

    private var projectileCount = 3
    private var angleGapDeg = 10f
    private var critChance = 0.20f
    private var critMultiplier = 2.0f

    private data class Arrow(var x: Float, var y: Float, var vx: Float, var vy: Float, var alive: Boolean = true)
    private val arrows = mutableListOf<Arrow>()
    private val speed = 420f

    // 화살의 충돌 크기 (반지름)
    private val arrowRadius = 6f

    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        if (nowMs - lastFire >= fireIntervalMs) {
            lastFire = nowMs
            shootFan(player, enemies)
        }

        val itA = arrows.iterator()
        while (itA.hasNext()) {
            val a = itA.next()
            a.x += a.vx * (1f/60f)
            a.y += a.vy * (1f/60f)

            val itE = enemies.iterator()
            while (itE.hasNext()) {
                val e = itE.next()
                if (!e.isAlive) continue

                val dx = e.x - a.x
                val dy = e.y - a.y

                // ★ [충돌 판정] 화살 반지름 + 적 반지름
                val hitDist = arrowRadius + e.radius

                if (dx*dx + dy*dy <= hitDist * hitDist) {
                    val isCrit = Random.nextFloat() < critChance
                    val dmg = baseDamage * if (isCrit) critMultiplier else 1f

                    e.takeDamage(dmg)

                    if (!piercing) { a.alive = false; break }
                }
            }
            if (!a.alive) itA.remove()
        }
    }

    private fun shootFan(player: Player, enemies: List<EnemyBase>) {
        val target = findNearest(player.x, player.y, enemies)
        val baseAng = if (target != null) {
            atan2(target.y - player.y, target.x - player.x)
        } else 0f

        val n = (projectileCount - 1) / 2
        for (i in -n..n) {
            val ang = baseAng + Math.toRadians((i * angleGapDeg).toDouble()).toFloat()
            val vx = cos(ang) * speed
            val vy = sin(ang) * speed
            arrows += Arrow(player.x, player.y, vx, vy)
        }
    }

    private fun findNearest(px: Float, py: Float, enemies: List<EnemyBase>): EnemyBase? {
        var best: EnemyBase? = null
        var bestD2 = Float.MAX_VALUE
        for (e in enemies) {
            if (!e.isAlive) continue
            val dx = e.x - px
            val dy = e.y - py
            val d2 = dx*dx + dy*dy
            if (d2 < bestD2) { bestD2 = d2; best = e }
        }
        return best
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        for (a in arrows) {
            canvas.drawCircle(a.x, a.y, arrowRadius, paint)
        }
    }

    override fun upgrade() {
        when (level) {
            0 -> { level = 1; projectileCount = 5; fireIntervalMs = 750L; critChance = 0.35f; critMultiplier = 4.5f }
            1 -> { level = 2; projectileCount = 9; fireIntervalMs = 500L; critChance = 0.60f; critMultiplier = 7.0f }
            else -> { level = 3; projectileCount = 15; fireIntervalMs = 250L; critChance = 1.0f; critMultiplier = 9.0f }
        }
    }
}