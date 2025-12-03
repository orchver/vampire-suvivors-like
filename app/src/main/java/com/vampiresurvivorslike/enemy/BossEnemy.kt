package com.vampiresurvivorslike.enemy

import android.content.Context
import android.graphics.*
import com.vampiresurvivorslike.R
import com.vampiresurvivorslike.player.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class BossEnemy(
    context: Context, // [추가] 이미지 로딩용
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
    // ──────── [비트맵 리소스] ────────
    private val swordBitmap: Bitmap
    private val arrowBitmap: Bitmap
    private val talismanBitmap: Bitmap
    private val bodyBitmap: Bitmap // 보스 본체 (EnemyStalker 재활용하거나 별도 이미지)

    init {
        // 1. 검 이미지
        val rawSword = BitmapFactory.decodeResource(context.resources, R.drawable.sword)
        swordBitmap = Bitmap.createScaledBitmap(rawSword, 120, 120, true)

        // 2. 화살 이미지
        val rawArrow = BitmapFactory.decodeResource(context.resources, R.drawable.arrow)
        arrowBitmap = Bitmap.createScaledBitmap(rawArrow, 30, 80, true)

        // 3. 부적 이미지
        val rawTalisman = BitmapFactory.decodeResource(context.resources, R.drawable.talisman)
        talismanBitmap = Bitmap.createScaledBitmap(rawTalisman, 40, 40, true)

        // 4. 보스 본체 (일단 Stalker 이미지 3배 뻥튀기해서 사용, 전용 이미지 있으면 교체)
        // 없으면 기본 원으로 대체되도록 예외처리 가능하지만, 여기선 stalker 리소스 활용
        val rawBody = BitmapFactory.decodeResource(context.resources, R.drawable.enemy_stalker)
        // 4열 1행 중 첫번째 컷
        val fw = rawBody.width / 4
        val cropBody = Bitmap.createBitmap(rawBody, 0, 0, fw, rawBody.height)
        // 보스니까 아주 크게 (지름 120 * 2.5배)
        bodyBitmap = Bitmap.createScaledBitmap(cropBody, 300, 300, true)
    }

    // ──────── [상태 변수] ────────
    private var slowTimer = 0f
    private val baseSpeedMult = 1.2f
    private val slowedSpeedMult = baseSpeedMult * 0.6f

    // ──────── [패턴 1: 돌진 (Dash)] ────────
    private var dashCycleTimer = 0f
    private enum class DashState { IDLE, AIMING, DASHING }
    private var dashState = DashState.IDLE
    private var aimTimer = 0f
    private var aimAngle = 0f
    private var dashDuration = 0f

    // 조준선 페인트 (이건 이미지가 애매해서 점선 유지)
    private val aimLinePaint = Paint().apply {
        color = Color.parseColor("#80FF0000")
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(30f, 20f), 0f)
        style = Paint.Style.STROKE
    }

    // ──────── [무기 1: 검 (Sword)] ────────
    private val swordCount = 1
    private val swordScale = 2.0f
    private val swordDmg = 25f * 2.0f
    private val swordRadius = 100f
    private var swordAngleAccum = 0f
    private val swordAngularSpeed = (Math.PI * 2 / 1.0).toFloat()

    // ──────── [무기 2: 활 (Bow)] ────────
    private val arrowCount = 5
    private val arrowFinalDmg = 10f * 9.0f
    private var bowTimer = 0f
    private val bowInterval = 2.5f
    private val arrowSpeed = 420f
    private val arrowGapDeg = 10f

    private data class BossArrow(var x: Float, var y: Float, var vx: Float, var vy: Float, var lifeTime: Float = 0f)
    private val bossArrows = mutableListOf<BossArrow>()

    // ──────── [무기 3: 부적 (Talisman)] ────────
    private val talismanCount = 12
    private val talismanDmg = 15f
    private val explosionRadius = 100f
    private var talismanTimer = 0f
    private val talismanInterval = 4.0f
    private val talismanSpeed = 200f
    private val talismanLifeMax = 2.0f
    private val homingStrength = 0.05f

    private data class BossOrb(var x: Float, var y: Float, var vx: Float, var vy: Float, var lifeTime: Float = 0f)
    private val bossOrbs = mutableListOf<BossOrb>()

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
        val actualSpeed = player.moveSpeed * speedMult // moveSpeed -> player.moveSpeed 확인 필요 (Player 변수명)

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
            val currentRadius = swordRadius + (16f * swordScale)
            val sx = x + cos(ang) * currentRadius
            val sy = y + sin(ang) * currentRadius

            val dist = hypot(player.x - sx, player.y - sy)
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

            if (o.lifeTime >= talismanLifeMax) {
                if (hypot(player.x - o.x, player.y - o.y) <= explosionRadius + player.radius) {
                    player.takeDamage(talismanDmg)
                }
                iterator.remove()
                continue
            }

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
        // 1. 조준선
        if (dashState == DashState.AIMING) {
            val endX = x + cos(aimAngle) * 2000f
            val endY = y + sin(aimAngle) * 2000f
            canvas.drawLine(x, y, endX, endY, aimLinePaint)
        }

        // 2. 검 그리기 (이미지 회전)
        drawSwords(canvas)

        // 3. 화살 그리기
        for (a in bossArrows) {
            canvas.save()
            canvas.translate(a.x, a.y)
            // 진행 방향으로 회전
            val angle = Math.toDegrees(atan2(a.vy.toDouble(), a.vx.toDouble())).toFloat()
            canvas.rotate(angle + 90f) // 화살 이미지가 위쪽을 보고 있다고 가정
            canvas.drawBitmap(arrowBitmap, -arrowBitmap.width/2f, -arrowBitmap.height/2f, null)
            canvas.restore()
        }

        // 4. 부적 그리기 (빙글빙글 회전 효과)
        val spin = (System.currentTimeMillis() % 1000) / 1000f * 360f
        for (o in bossOrbs) {
            canvas.save()
            canvas.translate(o.x, o.y)
            canvas.rotate(spin)
            canvas.drawBitmap(talismanBitmap, -talismanBitmap.width/2f, -talismanBitmap.height/2f, null)
            canvas.restore()
        }

        // 5. 보스 본체 그리기
        // 피격 시 색상 변경 효과는 PorterDuffColorFilter 등으로 구현 가능하지만 여기선 생략
        canvas.drawBitmap(bodyBitmap, x - bodyBitmap.width/2f, y - bodyBitmap.height/2f, null)
    }

    private fun drawSwords(c: Canvas) {
        val step = (Math.PI * 2 / swordCount).toFloat()

        for (i in 0 until swordCount) {
            val ang = swordAngleAccum + step * i
            val cx = x + cos(ang) * swordRadius
            val cy = y + sin(ang) * swordRadius

            c.save()
            c.translate(cx, cy)
            c.rotate(Math.toDegrees(ang.toDouble()).toFloat())

            // 크기 스케일
            c.scale(swordScale, swordScale)

            // 검 회전 (우상향 이미지 가정 +45도)
            c.rotate(45f)

            // 이미지 그리기
            c.drawBitmap(swordBitmap, -20f, -swordBitmap.height/2f, null)
            c.restore()
        }
    }
}