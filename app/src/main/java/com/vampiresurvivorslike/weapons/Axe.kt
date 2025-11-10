package com.vampiresurvivorslike.weapons

import android.graphics.*
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.Enemy
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

    // ─ 시각화용 페인트
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // 손잡이(자루)
        color = Color.rgb(120, 90, 60)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {   // 도끼날
        color = Color.rgb(180, 180, 190)
        style = Paint.Style.FILL
    }
    private val headOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    override fun update(player: Player, enemies: MutableList<Enemy>, nowMs: Long) {
        if (!sweeping) {
            if (nowMs - lastFire >= fireIntervalMs) {
                sweeping = true
                sweepAngle = -Math.PI.toFloat()
                lastFire = nowMs
            }
            return
        }

        // 쓸기 진행(30프레임 가정)
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
            val dx = e.x - centerX
            val dy = e.y - centerY
            if (dx*dx + dy*dy <= sweepRadius * sweepRadius) {
                val dead = e.takeDamage(dmg)
                val ang = atan2(dy, dx)
                e.knockback(cos(ang).toFloat(), sin(ang).toFloat(), sweepPower)
                player.heal(dmg * lifestealRate)
                if (dead) it.remove()
            }
        }

        if (sweepAngle >= Math.PI.toFloat()) {
            sweeping = false
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        // 쓸기 진행 중이든 아니든 항상 도끼를 보이게 그림
        // 현재 스윙 각도 위치에 "도끼 머리"를 배치
        val dirX = cos(sweepAngle)
        val dirY = sin(sweepAngle)
        val headX = px + dirX * sweepRadius
        val headY = py + dirY * sweepRadius

        // 자루(플레이어 → 도끼 머리)
        canvas.drawLine(px, py, headX, headY, handlePaint)   // 굵은 라인으로 자루 표현

        // 도끼 머리 모양 (타원형 + 베벨)
        canvas.save()
        canvas.translate(headX, headY)
        canvas.rotate(Math.toDegrees(sweepAngle.toDouble()).toFloat())

        // 머리 타원
        val w = 36f; val h = 26f
        val rect = RectF(-w*0.2f, -h, w, h)                  // 오른쪽이 베는 날
        canvas.drawOval(rect, headPaint)
        canvas.drawOval(rect, headOutline)

        // 베는 날을 강조하는 베벨(삼각형)
        val path = Path().apply {
            moveTo(w, 0f)
            lineTo(w - 10f, -h*0.6f)
            lineTo(w - 10f, h*0.6f)
            close()
        }
        canvas.drawPath(path, headPaint)
        canvas.drawPath(path, headOutline)

        canvas.restore()

        // (선택) 범위 가이드가 필요하면 얇게 표시
        // val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 200,200,200); style = Paint.Style.STROKE; strokeWidth = 3f }
        // canvas.drawCircle(px, py, sweepRadius, guide)
    }

    override fun upgrade() {
        when (level) {
            0 -> { level = 1 }                     // 피해/범위 1.3배는 update에서 반영
            1 -> { level = 2 }                     // 1.6배
            2 -> { level = 3; fireIntervalMs = 1200L } // 1.8배 + 간격 1.2초
            else -> { fireIntervalMs = 900L }      // 추가로 빠르게
        }
        sweepRadius = when (level) {
            1 -> 140f
            2 -> 170f
            else -> 200f
        }
    }
}
