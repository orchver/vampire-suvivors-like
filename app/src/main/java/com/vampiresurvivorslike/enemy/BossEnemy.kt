package com.vampiresurvivorslike.enemy

import android.graphics.*
import com.vampiresurvivorslike.player.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class BossEnemy(
    startX: Float,
    startY: Float,
    val player: Player,
    val screenW: Int,
    val screenH: Int
) : EnemyBase(
    x = startX,
    y = startY,
    hp = 100000,
    atk = 30,
    moveSpeed = 0f,
    expReward = 50000,
    radius = 60f
) {
    // ──────── [상태 변수] ────────
    private var slowTimer = 0f

    // 속도 배율 (피격 시 40% 감속)
    private val baseSpeedMult = 1.2f
    private val slowedSpeedMult = baseSpeedMult * 0.6f

    // ──────── [패턴 1: 돌진 (Dash)] ────────
    private var dashCycleTimer = 0f
    private enum class DashState { IDLE, AIMING, DASHING }
    private var dashState = DashState.IDLE
    private var aimTimer = 0f
    private var aimAngle = 0f
    private var dashDuration = 0f

    // ──────── [무기 1: 검 (Sword)] ────────
    // 요구사항: 1개의 검이 두배 커지고 데미지도 2배
    private val swordCount = 1         // ★ [수정] 개수는 1개
    private val swordScale = 2.0f      // ★ [수정] 크기 2배 (보라색 3단계)
    private val swordDmg = 25f * 2.0f  // ★ [수정] 데미지 2배 (보라색 3단계)
    private val swordRadius = 100f
    private var swordAngleAccum = 0f
    private val swordAngularSpeed = (Math.PI * 2 / 1.0).toFloat()

    // ──────── [무기 2: 활 (Bow)] ────────
    // 요구사항: 투사체 5개, 치명타 100% + 피해량 900%
    private val arrowCount = 5         // ★ [수정] 요청하신 대로 5개 설정
    // 기본 데미지(10) * 9.0배 (치명타 배율)
    private val arrowFinalDmg = 10f * 9.0f

    private var bowTimer = 0f
    private val bowInterval = 2.5f
    private val arrowSpeed = 420f
    private val arrowGapDeg = 10f

    private data class BossArrow(var x: Float, var y: Float, var vx: Float, var vy: Float, var lifeTime: Float = 0f)
    private val bossArrows = mutableListOf<BossArrow>()

    // ──────── [무기 3: 부적 (Talisman)] ────────
    // 요구사항: 투사체 개수 12개 (보라색 3단계)
    // 폭발 범위/피해량 증가는 노란색 옵션이므로 적용 X (기본값 사용)
    private val talismanCount = 12     // ★ [수정] 개수 12개 (보라색 3단계)
    private val talismanDmg = 15f      // 기본 데미지 (증가 없음)
    private val explosionRadius = 100f // 기본 폭발 범위

    private var talismanTimer = 0f
    private val talismanInterval = 4.0f
    private val talismanSpeed = 200f
    private val talismanLifeMax = 2.0f
    private val homingStrength = 0.05f

    private data class BossOrb(var x: Float, var y: Float, var vx: Float, var vy: Float, var lifeTime: Float = 0f)
    private val bossOrbs = mutableListOf<BossOrb>()

    // ──────── [그리기 도구] ────────
    private val bodyPaint = Paint().apply { color = Color.RED }
    private val aimLinePaint = Paint().apply {
        color = Color.parseColor("#80FF0000")
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(30f, 20f), 0f)
        style = Paint.Style.STROKE
    }
    private val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(230, 230, 235); style = Paint.Style.FILL }
    private val guardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(140, 120, 80); style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW }
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }

    override fun update(dt: Float, targetX: Float, targetY: Float) {
        updateSword(dt)
        updateArrows(dt)
        updateTalisman(dt)

        when (dashState) {
            DashState.IDLE -> updateChaseLogic(dt)
            DashState.AIMING -> updateAimLogic(dt)
            DashState.DASHING -> updateDashLogic(dt)
        }

        if (slowTimer > 0f) slowTimer -= dt
    }

    private fun updateChaseLogic(dt: Float) {
        val speedMult = if (slowTimer > 0f) slowedSpeedMult else baseSpeedMult
        val actualSpeed = player.moveSpeed * speedMult

        val dx = player.x - x
        val dy = player.y - y
        val dist = hypot(dx, dy).coerceAtLeast(1f)

        if (dist > 10f) {
            x += (dx / dist) * actualSpeed * dt
            y += (dy / dist) * actualSpeed * dt
        }

        dashCycleTimer += dt
        if (dashCycleTimer >= 7f) {
            dashState = DashState.AIMING
            aimTimer = 0f
            aimAngle = atan2(player.y - y, player.x - x)
            dashCycleTimer = 0f
        }

        bowTimer += dt
        if (bowTimer >= bowInterval) {
            bowTimer = 0f
            fireFanBow()
        }

        talismanTimer += dt
        if (talismanTimer >= talismanInterval) {
            talismanTimer = 0f
            fireTalismanBurst()
        }
    }

    private fun updateSword(dt: Float) {
        swordAngleAccum += swordAngularSpeed * dt
        val step = (Math.PI * 2 / swordCount).toFloat()

        for (i in 0 until swordCount) {
            val ang = swordAngleAccum + step * i
            // swordScale이 적용된 반지름 위치 계산
            val currentRadius = swordRadius + (16f * swordScale)
            val sx = x + cos(ang) * currentRadius
            val sy = y + sin(ang) * currentRadius

            val dist = hypot(player.x - sx, player.y - sy)
            // 검 크기에 따른 충돌 범위 증가
            val hitRange = (18f * swordScale) + player.radius

            if (dist <= hitRange) {
                player.takeDamage(swordDmg)
            }
        }
    }

    private fun fireFanBow() {
        val baseAng = atan2(player.y - y, player.x - x)
        val n = (arrowCount - 1) / 2
        for (i in -n..n) {
            val ang = baseAng + Math.toRadians((i * arrowGapDeg).toDouble()).toFloat()
            val vx = cos(ang) * arrowSpeed
            val vy = sin(ang) * arrowSpeed
            bossArrows.add(BossArrow(x, y, vx, vy))
        }
    }

    private fun updateArrows(dt: Float) {
        val iterator = bossArrows.iterator()
        while (iterator.hasNext()) {
            val a = iterator.next()
            a.lifeTime += dt
            a.x += a.vx * dt
            a.y += a.vy * dt

            if (a.lifeTime > 5f) {
                iterator.remove()
                continue
            }

            if (hypot(player.x - a.x, player.y - a.y) <= 6f + player.radius) {
                player.takeDamage(arrowFinalDmg)
                iterator.remove()
            }
        }
    }

    private fun fireTalismanBurst() {
        val step = (Math.PI * 2 / talismanCount).toFloat()
        for (i in 0 until talismanCount) {
            val angle = step * i
            val vx = cos(angle) * talismanSpeed
            val vy = sin(angle) * talismanSpeed
            bossOrbs.add(BossOrb(x, y, vx, vy))
        }
    }

    private fun updateTalisman(dt: Float) {
        val iterator = bossOrbs.iterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            o.lifeTime += dt

            // 2초 뒤 폭발 (범위 데미지)
            if (o.lifeTime >= talismanLifeMax) {
                if (hypot(player.x - o.x, player.y - o.y) <= explosionRadius + player.radius) {
                    player.takeDamage(talismanDmg)
                }
                iterator.remove()
                continue
            }

            // 유도 로직
            val dx = player.x - o.x
            val dy = player.y - o.y
            val d = hypot(dx, dy).coerceAtLeast(1e-3f)
            val targetVx = (dx / d) * talismanSpeed
            val targetVy = (dy / d) * talismanSpeed

            o.vx = (1 - homingStrength) * o.vx + homingStrength * targetVx
            o.vy = (1 - homingStrength) * o.vy + homingStrength * targetVy

            val vLen = hypot(o.vx, o.vy).coerceAtLeast(1e-3f)
            o.vx = (o.vx / vLen) * talismanSpeed
            o.vy = (o.vy / vLen) * talismanSpeed

            o.x += o.vx * dt
            o.y += o.vy * dt

            // 직접 충돌 시 데미지
            if (hypot(player.x - o.x, player.y - o.y) <= 8f + player.radius) {
                player.takeDamage(talismanDmg)
                iterator.remove()
            }
        }
    }

    private fun updateAimLogic(dt: Float) {
        aimTimer += dt
        if (aimTimer >= 1.0f) {
            dashState = DashState.DASHING
            dashDuration = 0f
        }
    }

    private fun updateDashLogic(dt: Float) {
        dashDuration += dt
        val dashSpeed = 1500f
        x += cos(aimAngle) * dashSpeed * dt
        y += sin(aimAngle) * dashSpeed * dt

        if (hypot(player.x - x, player.y - y) < radius + player.radius) {
            player.takeDamage(50f)
        }
        if (dashDuration >= 0.3f) {
            dashState = DashState.IDLE
        }
    }

    override fun takeDamage(damage: Float): Boolean {
        slowTimer = 1.0f
        return super.takeDamage(damage)
    }

    override fun draw(canvas: Canvas) {
        if (dashState == DashState.AIMING) {
            val endX = x + cos(aimAngle) * 2000f
            val endY = y + sin(aimAngle) * 2000f
            canvas.drawLine(x, y, endX, endY, aimLinePaint)
        }

        drawSwords(canvas)
        for (a in bossArrows) canvas.drawCircle(a.x, a.y, 6f, arrowPaint)
        for (o in bossOrbs) canvas.drawCircle(o.x, o.y, 8f, orbPaint)
        canvas.drawCircle(x, y, radius, bodyPaint)
    }

    private fun drawSwords(c: Canvas) {
        val step = (Math.PI * 2 / swordCount).toFloat()
        val bladeLen = 80f * swordScale
        val bladeWid = 20f * swordScale
        val guardWid = 30f * swordScale
        val handleLen = 20f * swordScale

        for (i in 0 until swordCount) {
            val ang = swordAngleAccum + step * i
            val cx = x + cos(ang) * swordRadius
            val cy = y + sin(ang) * swordRadius

            c.save()
            c.translate(cx, cy)
            c.rotate(Math.toDegrees(ang.toDouble()).toFloat())

            val handle = RectF(-handleLen, -bladeWid * 0.5f, 0f, bladeWid * 0.5f)
            c.drawRoundRect(handle, 4f, 4f, guardPaint)
            c.drawRoundRect(handle, 4f, 4f, outlinePaint)
            val guard = RectF(-2f, -guardWid * 0.5f, 2f, guardWid * 0.5f)
            c.drawRect(guard, guardPaint)
            c.drawRect(guard, outlinePaint)
            val blade = RectF(0f, -bladeWid * 0.5f, bladeLen, bladeWid * 0.5f)
            c.drawRoundRect(blade, 3f, 3f, bladePaint)
            c.drawRoundRect(blade, 3f, 3f, outlinePaint)
            val path = Path().apply {
                moveTo(bladeLen, 0f)
                lineTo(bladeLen - 6f, -bladeWid * 0.5f)
                lineTo(bladeLen - 6f, bladeWid * 0.5f)
                close()
            }
            c.drawPath(path, bladePaint)
            c.drawPath(path, outlinePaint)
            c.restore()
        }
    }
}