package com.vampiresurvivorslike.weapons

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.Enemy
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class Talisman : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 15f                     // 기본 피해 15
    override var fireIntervalMs: Long = 500L                 // 발사 간격 0.5초
    override var piercing: Boolean = false                   // 관통 X

    private var lastFire = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }

    // ─ 유도 투사체
    private data class Orb(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var target: Enemy?,           // 현재 락온 대상
        var bornAt: Long,             // 생성 시간(ms)
        var alive: Boolean = true
    )

    private val orbs = mutableListOf<Orb>()

    // ─ 이동/유도 파라미터
    private val speed = 380f                             // 비행 속도(px/s)
    private val homingStrength = 0.22f                   // 유도 강도(0~1) : 클수록 급선회
    private val maxLifeMs = 7000L                        // 안전용 최대 생존 시간

    // ─ 레벨에 따른 동시 발사 개수(요구사항: 1/3/7/12)
    private fun shotsPerFire(): Int = when (level) {
        1 -> 3
        2 -> 7
        3 -> 12
        else -> 1
    }

    override fun update(player: Player, enemies: MutableList<Enemy>, nowMs: Long) {
        // 발사 타이밍
        if (nowMs - lastFire >= fireIntervalMs) {
            lastFire = nowMs
            val n = shotsPerFire()
            repeat(n) {
                // 가장 가까운 적 기준으로 초기 각도 부여
                val tgt = findNearest(player.x, player.y, enemies)
                val ang = if (tgt != null) atan2(tgt.y - player.y, tgt.x - player.x) else 0f
                val vx = cos(ang) * speed
                val vy = sin(ang) * speed
                orbs += Orb(player.x, player.y, vx, vy, tgt, nowMs)
            }
        }

        // 투사체 업데이트
        val dt = 1f / 60f                                 // 간단히 60fps 가정
        val itO = orbs.iterator()
        while (itO.hasNext()) {
            val o = itO.next()

            // 수명 초과 시 제거 (무한 루프 방지용)
            if (nowMs - o.bornAt > maxLifeMs) {
                itO.remove()
                continue
            }

            // 대상 갱신: null 이거나 이미 목록에서 제거되었으면 최근접으로 재락온
            if (o.target == null || !enemies.contains(o.target)) {
                o.target = findNearest(o.x, o.y, enemies)
            }

            // 강력 락온: 현재 속도를 "목표 방향 속도" 쪽으로 블렌딩 → 빙 돌아서라도 맞춤
            o.target?.let { t ->
                val dx = t.x - o.x
                val dy = t.y - o.y
                val d = hypot(dx, dy).coerceAtLeast(1e-3f)

                // 원하는 속도(목표 방향) 벡터
                val desiredVx = dx / d * speed
                val desiredVy = dy / d * speed

                // 현재 속도를 desired 쪽으로 보간 (homingStrength 만큼 당김)
                o.vx = (1 - homingStrength) * o.vx + homingStrength * desiredVx
                o.vy = (1 - homingStrength) * o.vy + homingStrength * desiredVy

                // 속도 크기를 speed 로 정규화 (너무 느려지는 것 방지)
                val vLen = hypot(o.vx, o.vy).coerceAtLeast(1e-3f)
                o.vx = o.vx / vLen * speed
                o.vy = o.vy / vLen * speed
            }

            // 위치 갱신
            o.x += o.vx * dt
            o.y += o.vy * dt

            // 충돌 판정 (원-원 근사)
            var hit = false
            val itE = enemies.iterator()
            while (itE.hasNext()) {
                val e = itE.next()
                val dx = e.x - o.x
                val dy = e.y - o.y
                if (dx*dx + dy*dy <= 12f * 12f) {
                    // 피해량: 레벨별 폭발/피해 배율 반영(요구 스펙에 맞춰 단순 배율)
                    val dmg = baseDamage * when (level) {
                        1 -> 1.2f
                        2 -> 1.6f
                        3 -> 2.0f
                        else -> 1f
                    }
                    val dead = e.takeDamage(dmg)
                    if (dead) itE.remove()
                    hit = true
                    break
                }
            }
            if (hit && !piercing) {
                itO.remove()                               // 관통X 이므로 즉시 제거
            }
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        // 유도구체(부적) 시각화
        for (o in orbs) {
            // 본체
            canvas.drawCircle(o.x, o.y, 8f, paint)        // 본체
            // (선택) 꼬리 효과: 속도 반대방향으로 짧은 선
            // val tail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN; strokeWidth = 3f }
            // canvas.drawLine(o.x, o.y, o.x - o.vx * 0.05f, o.y - o.vy * 0.05f, tail)
        }
    }

    override fun upgrade() {
        // 레벨당 "폭발/피해 배율"은 update에서 dmg 계산에 반영
        // 여기서는 동시 발사 개수만 스펙대로 증가
        when (level) {
            0 -> level = 1    // 1단계: 3발
            1 -> level = 2    // 2단계: 7발
            else -> level = 3 // 3단계: 12발
        }
    }

    // ─ 최근접 적 찾기
    private fun findNearest(px: Float, py: Float, enemies: List<Enemy>): Enemy? {
        var best: Enemy? = null
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
