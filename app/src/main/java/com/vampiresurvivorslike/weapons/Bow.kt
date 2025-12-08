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
import kotlin.random.Random

class Bow(context: Context) : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 1f
    override var fireIntervalMs: Long = 1000L
    override var piercing: Boolean = true

    private var lastFire = 0L

    private var projectileCount = 3
    private var angleGapDeg = 10f
    private var critChance = 0.20f
    private var critMultiplier = 2.0f

    private data class Arrow(var x: Float, var y: Float, var vx: Float, var vy: Float, var alive: Boolean = true)
    private val arrows = mutableListOf<Arrow>()
    private val speed = 420f
    private val arrowRadius = 10f // 충돌 범위

    private val bitmap: Bitmap

    init {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.arrow)
        // 화살 크기 (가로세로 비율 유지하며 높이 40px 정도로)
        val h = 40
        val w = (raw.width * (40f / raw.height)).toInt()
        bitmap = Bitmap.createScaledBitmap(raw, w, h, true)
    }

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
            canvas.save()
            canvas.translate(a.x, a.y)

            // 이동 방향으로 회전
            val deg = Math.toDegrees(atan2(a.vy, a.vx).toDouble()).toFloat()
            canvas.rotate(deg + 90f) // 화살 이미지가 위를 보고 있다면 +90도 해서 진행방향 맞춤

            canvas.drawBitmap(bitmap, -bitmap.width/2f, -bitmap.height/2f, null)
            canvas.restore()
        }
    }

    override fun upgrade() {
        when (level) {
            0 -> { level = 1; projectileCount = 5; fireIntervalMs = 750L; critChance = 0.1f; critMultiplier = 3f }
            1 -> { level = 2; projectileCount = 9; fireIntervalMs = 500L; critChance = 0.2f; critMultiplier = 7.0f }
            else -> { level = 3; projectileCount = 15; fireIntervalMs = 250L; critChance = 0.3f; critMultiplier = 15.0f }
        }
    }
}