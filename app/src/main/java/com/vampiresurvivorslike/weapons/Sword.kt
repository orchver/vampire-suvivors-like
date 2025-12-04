package com.vampiresurvivorslike.weapons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.vampiresurvivorslike.R
import com.vampiresurvivorslike.enemy.EnemyBase
import com.vampiresurvivorslike.player.Player
import kotlin.math.cos
import kotlin.math.sin

class Sword(context: Context) : Weapon {

    override var level: Int = 0
    override var baseDamage: Float = 20f
    override var fireIntervalMs: Long = 0L
    override var piercing: Boolean = true

    // 2초에 1바퀴
    private val angularSpeed = (Math.PI * 2 / 0.5).toFloat()

    private var swordCount = 1
    private var swordRadius = 90f
    private var swordScale = 1f
    private var angleAccum = 0f

    // 비트맵
    private val bitmap: Bitmap

    init {
        // sword.png 로드
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.sword)

        // 크기 조정 (기존 코드의 bladeLen 등 고려해서 적절히 설정)
        // 약 100x100 픽셀 정도로 조정 (비율 유지)
        val scaledW = 120
        val scaledH = (raw.height * (120f / raw.width)).toInt()
        bitmap = Bitmap.createScaledBitmap(raw, scaledW, scaledH, true)
    }

    override fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long) {
        // 각도 적산
        angleAccum += angularSpeed * (1f / 60f)

        val step = (Math.PI * 2 / swordCount).toFloat()
        val hitR = 25f * swordScale // 공격 범위 약간 보정
        val dmg = baseDamage * swordScale

        for (e in enemies) {
            if (!e.isAlive) continue

            // 각 검의 칼끝 좌표 계산
            for (i in 0 until swordCount) {
                val ang = angleAccum + step * i
                // 검의 중심부 + 칼날 길이만큼 나간 곳이 타격점
                val sx = player.x + cos(ang) * (swordRadius + 40f * swordScale)
                val sy = player.y + sin(ang) * (swordRadius + 40f * swordScale)
                val dx = e.x - sx
                val dy = e.y - sy

                // 충돌 판정
                val hitRange = hitR + e.radius
                if (dx * dx + dy * dy <= hitRange * hitRange) {
                    e.takeDamage(dmg)
                    break
                }
            }
        }
    }

    override fun draw(canvas: Canvas, px: Float, py: Float) {
        val step = (Math.PI * 2 / swordCount).toFloat()

        for (i in 0 until swordCount) {
            val ang = angleAccum + step * i
            val cx = px + cos(ang) * swordRadius
            val cy = py + sin(ang) * swordRadius

            canvas.save()
            canvas.translate(cx, cy)

            // 검 회전 (기본 각도 + 이미지 보정)
            // 이미지가 대각선(45도)으로 되어 있다면 -45도 보정이 필요할 수 있습니다.
            // 여기서는 일단 기본 회전만 적용하고, 필요시 오프셋을 더하세요.
            canvas.rotate(Math.toDegrees(ang.toDouble()).toFloat())

            // 크기 스케일 적용
            canvas.scale(swordScale, swordScale)

            // 이미지를 중심점이 아닌 손잡이 부분이 (0,0)에 오도록 조정해서 그림
            // 이미지가 "우상향" 대각선 검이라고 가정하고, 45도 돌려서 수평으로 맞춤
            canvas.rotate(45f)

            // 중심 보정 (이미지의 손잡이가 왼쪽 아래라고 가정)
            canvas.drawBitmap(bitmap, -20f, -bitmap.height / 2f, null)

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