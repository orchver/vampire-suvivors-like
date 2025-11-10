package com.vampiresurvivorslike

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.hypot

class Enemy(
    var x: Float,
    var y: Float,
    var hp: Float = 5f,           // 체력: 무기 피해(float)와 맞추기 위해 Float 사용
    private val speed: Float = 1.4f,
    val radius: Float = 20f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }

    /** 플레이어 위치를 향해 단순 추적 */
    fun update(targetX: Float, targetY: Float) {
        val dx = targetX - x
        val dy = targetY - y
        val d = hypot(dx, dy).coerceAtLeast(0.0001f)    // 0으로 나누기 방지
        x += speed * (dx / d)
        y += speed * (dy / d)
    }

    /** 그리기 */
    fun draw(c: Canvas) {
        c.drawCircle(x, y, radius, paint)
    }

    /** 피해를 받고 사망 여부 반환 */
    fun takeDamage(damage: Float): Boolean {            // 무기에서 호출
        hp -= damage
        return hp <= 0f
    }

    /** 넉백: 방향 벡터(dirX, dirY)를 power 만큼 밀어냄 */
    fun knockback(dirX: Float, dirY: Float, power: Float) {
        val len = hypot(dirX, dirY).coerceAtLeast(0.0001f)
        val nx = dirX / len
        val ny = dirY / len
        x += nx * power
        y += ny * power
    }

    /** 원형 히트 판정이 필요한 경우를 대비한 보조 함수(선택 사용) */
    fun hitCircle(cx: Float, cy: Float, r: Float, damage: Float): Boolean { // 선택적
        if (hypot(cx - x, cy - y) <= r + radius) {
            return takeDamage(damage)
        }
        return false
    }
}
