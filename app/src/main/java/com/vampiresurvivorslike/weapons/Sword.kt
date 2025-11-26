package com.vampiresurvivorslike.weapons

import android.graphics.*
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.enemy.EnemyBase
import kotlin.math.cos
import kotlin.math.sin

class Sword : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 25f
    override var fireIntervalMs: Long = 0L
    override var piercing: Boolean = true

    // 2초에 1바퀴
    private val angularSpeed = (Math.PI * 2 / 0.5).toFloat()

    private var swordCount = 1
    private var swordRadius = 90f
    private var swordScale = 1f

    // 시각화용 페인트들
    private val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(230, 230, 235)
        style = Paint.Style.FILL
    }
    private val guardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(140, 120, 80)
        style = Paint.Style.FILL
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var angleAccum = 0f

    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        // 각도 적산
        angleAccum += angularSpeed * (1f / 60f)

        val step = (Math.PI * 2 / swordCount).toFloat()
        val hitR = 18f * swordScale              // 칼끝 공격 범위 반지름
        val dmg = baseDamage * swordScale

        for (e in enemies) {
            if (!e.isAlive) continue  // 죽은 적 패스

            // 각 검의 칼끝 좌표 계산
            for (i in 0 until swordCount) {
                val ang = angleAccum + step * i
                val sx = player.x + cos(ang) * (swordRadius + 16f * swordScale)
                val sy = player.y + sin(ang) * (swordRadius + 16f * swordScale)
                val dx = e.x - sx
                val dy = e.y - sy

                // ★ [충돌 판정] (칼 공격범위 + 적 반지름) 거리 내에 있으면 피격
                val hitRange = hitR + e.radius
                if (dx * dx + dy * dy <= hitRange * hitRange) {
                    e.takeDamage(dmg)
                    break // 한 번 맞으면 이 루프 탈출 (중복 데미지 방지)
                }
            }
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        val step = (Math.PI * 2 / swordCount).toFloat()

        val bladeLen = 80f * swordScale
        val bladeWid = 20f * swordScale
        val guardWid = 30f * swordScale
        val handleLen = 20f * swordScale

        for (i in 0 until swordCount) {
            val ang = angleAccum + step * i
            val cx = px + cos(ang) * swordRadius
            val cy = py + sin(ang) * swordRadius

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(Math.toDegrees(ang.toDouble()).toFloat())

            // 그리기 로직 (기존 동일)
            val handle = RectF(-handleLen, -bladeWid * 0.5f, 0f, bladeWid * 0.5f)
            canvas.drawRoundRect(handle, 4f, 4f, guardPaint)
            canvas.drawRoundRect(handle, 4f, 4f, outline)

            val guard = RectF(-2f, -guardWid * 0.5f, 2f, guardWid * 0.5f)
            canvas.drawRect(guard, guardPaint)
            canvas.drawRect(guard, outline)

            val blade = RectF(0f, -bladeWid * 0.5f, bladeLen, bladeWid * 0.5f)
            canvas.drawRoundRect(blade, 3f, 3f, bladePaint)
            canvas.drawRoundRect(blade, 3f, 3f, outline)

            val path = Path().apply {
                moveTo(bladeLen, 0f)
                lineTo(bladeLen - 6f, -bladeWid * 0.5f)
                lineTo(bladeLen - 6f, bladeWid * 0.5f)
                close()
            }
            canvas.drawPath(path, bladePaint)
            canvas.drawPath(path, outline)

            canvas.restore()
        }
    }

    override fun upgrade() {
        when (level) {
            0 -> { swordCount = 3; level = 1 }
            1 -> { swordCount = 5; level = 2 }
            2 -> { swordCount = 7; swordScale = 1.2f; level = 3 }
            else -> { swordScale = 2.0f }
        }
    }
}