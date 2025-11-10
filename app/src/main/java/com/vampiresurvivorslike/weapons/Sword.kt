package com.vampiresurvivorslike.weapons

import android.graphics.*
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.Enemy
import kotlin.math.cos
import kotlin.math.sin

class Sword : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 25f
    override var fireIntervalMs: Long = 0L
    override var piercing: Boolean = true

    // 2초에 1바퀴
    private val angularSpeed = (Math.PI * 2 / 2.0).toFloat()

    private var swordCount = 1
    private var swordRadius = 90f
    private var swordScale = 1f

    // ─ 시각화용 페인트들
    private val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // 칼날
        color = Color.rgb(230, 230, 235)
        style = Paint.Style.FILL
    }
    private val guardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // 손잡이/가드
        color = Color.rgb(140, 120, 80)
        style = Paint.Style.FILL
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {     // 외곽선
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var angleAccum = 0f

    override fun update(player: Player, enemies: MutableList<Enemy>, nowMs: Long) {
        // 각도 적산 (60fps 가정)
        angleAccum += angularSpeed * (1f / 60f)

        // 충돌은 "칼끝 원" 근사
        val step = (Math.PI * 2 / swordCount).toFloat()
        val hitR = 18f * swordScale
        val dmg = baseDamage * swordScale

        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            // 각 검의 칼끝 근사 좌표로 거리 체크
            for (i in 0 until swordCount) {
                val ang = angleAccum + step * i
                val sx = player.x + cos(ang) * (swordRadius + 16f * swordScale) // 칼끝 쪽으로 약간 더
                val sy = player.y + sin(ang) * (swordRadius + 16f * swordScale)
                val dx = e.x - sx
                val dy = e.y - sy
                if (dx*dx + dy*dy <= hitR*hitR) {
                    val dead = e.takeDamage(dmg)
                    if (dead) { it.remove(); break }
                }
            }
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        val step = (Math.PI * 2 / swordCount).toFloat()

        // 칼 치수(스케일 반영)
        val bladeLen = 80f * swordScale   // 칼날 길이
        val bladeWid = 20f * swordScale   // 칼날 두께
        val guardWid = 30f * swordScale   // 가드 폭
        val handleLen = 20f * swordScale  // 손잡이 길이

        for (i in 0 until swordCount) {
            val ang = angleAccum + step * i
            val cx = px + cos(ang) * swordRadius
            val cy = py + sin(ang) * swordRadius

            // 캔버스 회전/이동
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(Math.toDegrees(ang.toDouble()).toFloat())

            // 손잡이(뒤쪽)  ─ x<0 방향이 플레이어쪽
            val handle = RectF(-handleLen, -bladeWid*0.5f, 0f, bladeWid*0.5f)
            canvas.drawRoundRect(handle, 4f, 4f, guardPaint)
            canvas.drawRoundRect(handle, 4f, 4f, outline)

            // 가드(십자)
            val guard = RectF(-2f, -guardWid*0.5f, 2f, guardWid*0.5f)
            canvas.drawRect(guard, guardPaint)
            canvas.drawRect(guard, outline)

            // 칼날(앞쪽)
            val blade = RectF(0f, -bladeWid*0.5f, bladeLen, bladeWid*0.5f)
            canvas.drawRoundRect(blade, 3f, 3f, bladePaint)
            canvas.drawRoundRect(blade, 3f, 3f, outline)

            // 칼끝 삼각형 느낌
            val path = Path().apply {
                moveTo(bladeLen, 0f)
                lineTo(bladeLen - 6f, -bladeWid*0.5f)
                lineTo(bladeLen - 6f, bladeWid*0.5f)
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
            else -> { swordScale = 2.0f } // 최대 강화 보정
        }
    }
}
