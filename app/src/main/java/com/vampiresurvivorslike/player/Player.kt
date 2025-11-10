package com.vampiresurvivorslike.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Player(var weapon: com.vampiresurvivorslike.weapons.Weapon) {

    var x = 0f
    var y = 0f
    val radius = 24f

    // ─ 체력 시스템 ─
    var maxHp = 100f
    var hp = 100f

    // ─ 이동 속도 (기본값, 필요하면 나중에 패시브로 업그레이드 가능) ─
    var moveSpeed = 260f

    // ─ 레벨 / 경험치 시스템 ─
    var level: Int = 1
    var exp: Int = 0
    var expToNext: Int = 200      // 레벨업 필요 경험치 (레벨업마다 2배)

    // GameView 에서 넣어주는 콜백: "레벨업이 발생했을 때" 호출
    var onLevelUp: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }

    // 조이스틱으로 이동
    fun updateByJoystick(ax: Float, ay: Float, dtSec: Float, w: Int, h: Int) {
        x = (x + ax * moveSpeed * dtSec).coerceIn(radius, w - radius)
        y = (y + ay * moveSpeed * dtSec).coerceIn(radius, h - radius)
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }

    // 체력 회복
    fun heal(amount: Float) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }

    /** ★ 경험치 증가 + 레벨업 처리 (여기서는 무기 업그레이드 안 함!)
     *
     *  요구사항:
     *  - 일정 경험치 이상 획득 시: 게임 일시정지 + 3가지 업그레이드 중 택1
     *  - 레벨업 시: 최대체력 25 증가, 잃은 체력의 75% 회복(정수 내림)
     *  - 무기 업그레이드는 GameView에서 "선택한 옵션"으로 처리
     */
    fun gainExp(amount: Int) {
        exp += amount
        while (exp >= expToNext) {
            exp -= expToNext
            level += 1
            expToNext *= 2      // 레벨업마다 필요 경험치 2배

            // 1) 최대 체력 25 증가
            maxHp += 25f

            // 2) 잃은 체력의 75% 회복 (정수 내림)
            val missing = maxHp - hp
            val healInt = (missing * 0.75f).toInt()
            hp += healInt
            if (hp > maxHp) hp = maxHp

            // 3) 무기 업그레이드는 여기서 하지 않고,
            //    GameView의 레벨업 선택 UI에서 어떤 무기를 올릴지 결정
            onLevelUp?.invoke()
        }
    }
}
