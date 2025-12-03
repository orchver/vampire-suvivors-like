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
import kotlin.math.hypot
import kotlin.math.sin

class Talisman(context: Context) : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 15f
    override var fireIntervalMs: Long = 500L
    override var piercing: Boolean = false

    private var lastFire = 0L

    // 유도 투사체
    private data class Orb(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var target: EnemyBase?, var bornAt: Long, var alive: Boolean = true
    )

    private val orbs = mutableListOf<Orb>()
    private val speed = 380f
    private val homingStrength = 0.22f
    private val maxLifeMs = 7000L

    private val bitmap: Bitmap

    init {
        // papertalisman.png 로드
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.talisman)
        // 적당한 크기 (30x30 정도)
        val size = 32
        bitmap = Bitmap.createScaledBitmap(raw, size, size, true)
    }

    private fun shotsPerFire(): Int = when (level) {
        1 -> 3
        2 -> 7
        3 -> 12
        else -> 1
    }

    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        if (nowMs - lastFire >= fireIntervalMs) {
            lastFire = nowMs
            val n = shotsPerFire()
            repeat(n) {
                val tgt = findNearest(player.x, player.y, enemies)
                val ang = if (tgt != null) atan2(tgt.y - player.y, tgt.x - player.x) else 0f
                val vx = cos(ang) * speed
                val vy = sin(ang) * speed
                orbs += Orb(player.x, player.y, vx, vy, tgt, nowMs)
            }
        }

        val dt = 1f / 60f
        val itO = orbs.iterator()
        while (itO.hasNext()) {
            val o = itO.next()
            if (nowMs - o.bornAt > maxLifeMs) { itO.remove(); continue }

            if (o.target == null || !enemies.contains(o.target)) {
                o.target = findNearest(o.x, o.y, enemies)
            }

            o.target?.let { t ->
                val dx = t.x - o.x
                val dy = t.y - o.y
                val d = hypot(dx, dy).coerceAtLeast(1e-3f)
                val desiredVx = dx / d * speed
                val desiredVy = dy / d * speed
                o.vx = (1 - homingStrength) * o.vx + homingStrength * desiredVx
                o.vy = (1 - homingStrength) * o.vy + homingStrength * desiredVy
                val vLen = hypot(o.vx, o.vy).coerceAtLeast(1e-3f)
                o.vx = o.vx / vLen * speed
                o.vy = o.vy / vLen * speed
            }

            o.x += o.vx * dt
            o.y += o.vy * dt

            var hit = false
            val itE = enemies.iterator()
            while (itE.hasNext()) {
                val e = itE.next()
                val dx = e.x - o.x
                val dy = e.y - o.y
                if (dx*dx + dy*dy <= 15f * 15f) { // 충돌 범위 (이미지 크기 절반)
                    val dmg = baseDamage * when (level) {
                        1 -> 1.2f
                        2 -> 1.6f
                        3 -> 2.0f
                        else -> 1f
                    }
                    e.takeDamage(dmg)
                    hit = true
                    break
                }
            }
            if (hit && !piercing) {
                itO.remove()
            }
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        for (o in orbs) {
            canvas.save()
            canvas.translate(o.x, o.y)

            // 이동 방향으로 회전
            val deg = Math.toDegrees(atan2(o.vy, o.vx).toDouble()).toFloat()
            canvas.rotate(deg + 90f)

            canvas.drawBitmap(bitmap, -bitmap.width/2f, -bitmap.height/2f, null)
            canvas.restore()
        }
    }

    override fun upgrade() {
        if (level < 3) level++
    }

    private fun findNearest(px: Float, py: Float, enemies: List<EnemyBase>): EnemyBase? {
        var best: EnemyBase? = null
        var bestD2 = Float.MAX_VALUE
        for (e in enemies) {
            val dx = e.x - px
            val dy = e.y - py
            val d2 = dx*dx + dy*dy
            if (d2 < bestD2) { bestD2 = d2; best = e }
        }
        return best
    }
}