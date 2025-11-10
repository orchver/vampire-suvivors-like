package com.vampiresurvivorslike.weapons

import android.graphics.Canvas
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.Enemy

interface Weapon {
    // 현재 레벨 (업그레이드 단계 0~3)
    var level: Int

    // 기본 피해량
    var baseDamage: Float

    // 발동 간격(ms)
    var fireIntervalMs: Long

    // 관통 여부
    var piercing: Boolean

    // 업데이트 (발사/스윙/충돌판정 등)
    fun update(player: Player, enemies: MutableList<Enemy>, nowMs: Long)

    // 그리기
    fun draw(canvas: Canvas, px: Float, py: Float)

    // 레벨업 적용
    fun upgrade()
}
