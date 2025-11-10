package com.vampiresurvivorslike.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Player(var weapon: com.vampiresurvivorslike.weapons.Weapon) {

    var x = 0f
    var y = 0f
    val radius = 24f

    // 체력 시스템 (간단 버전)
    var maxHp = 100f       // 최대 체력
    var hp = 100f          // 현재 체력

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }

    // 조이스틱으로 이동 (기존 로직 유지)
    fun updateByJoystick(ax: Float, ay: Float, dtSec: Float, w: Int, h: Int) {
        // 플레이어 이동 로직은 생략 (기존 코드 유지)  // 플레이어 이동 로직은 생략
        val speed = 260f                                  // 이동 속도
        x = (x + ax * speed * dtSec).coerceIn(radius, w - radius)   // 경계 체크
        y = (y + ay * speed * dtSec).coerceIn(radius, h - radius)
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }

    // 생명력 흡수 회복
    fun heal(amount: Float) { // 받은 회복량만큼 체력 회복
        hp = (hp + amount).coerceAtMost(maxHp)
    }
}
