package com.vampiresurvivorslike.weapons

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.vampiresurvivorslike.enemy.EnemyBase // ★ Enemy 대신 EnemyBase import
import com.vampiresurvivorslike.player.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Talisman : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 15f
    override var fireIntervalMs: Long = 500L
    override var piercing: Boolean = false

    private var lastFire = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }

    // ─ 유도 투사체
    private data class Orb(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var target: EnemyBase?,       // ★ 변경: Enemy -> EnemyBase
        var bornAt: Long,
        var alive: Boolean = true
    )

    private val orbs = mutableListOf<Orb>()

    // ─ 이동/유도 파라미터
    private val speed = 380f
    private val homingStrength = 0.22f
    private val maxLifeMs = 7000L

    private fun shotsPerFire(): Int = when (level) {
        1 -> 3
        2 -> 7
        3 -> 12
        else -> 1
    }

    // ★ 변경: enemies: MutableList<EnemyBase>
    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        // 발사 타이밍
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

        // 투사체 업데이트
        val dt = 1f / 60f
        val itO = orbs.iterator()
        while (itO.hasNext()) {
            val o = itO.next()

            if (nowMs - o.bornAt > maxLifeMs) {
                itO.remove()
                continue
            }

            // 대상 갱신
            if (o.target == null || !enemies.contains(o.target)) {
                o.target = findNearest(o.x, o.y, enemies)
            }

            // 유도 로직
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

            // 충돌 판정 (기존 코드 유지)
            var hit = false
            val itE = enemies.iterator()
            while (itE.hasNext()) {
                val e = itE.next()
                val dx = e.x - o.x
                val dy = e.y - o.y

                // ★ 기존 계산법 유지
                if (dx*dx + dy*dy <= 12f * 12f) {
                    val dmg = baseDamage * when (level) {
                        1 -> 1.2f
                        2 -> 1.6f
                        3 -> 2.0f
                        else -> 1f
                    }
                    // EnemyBase에 있는 takeDamage 호출
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
            canvas.drawCircle(o.x, o.y, 8f, paint)
        }
    }

    override fun upgrade() {
        if (level < 3) level++
    }

    // ★ 변경: List<EnemyBase>, 반환형 EnemyBase?
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