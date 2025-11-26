package com.vampiresurvivorslike.weapons

import android.graphics.Canvas
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.enemy.EnemyBase // ★ 변경됨: Enemy -> enemy.EnemyBase

interface Weapon {
    // 현재 레벨 (업그레이드 단계 0~3)
    var level: Int

    // 기본 피해량
    var baseDamage: Float

    // 발동 간격(ms)
    var fireIntervalMs: Long

    // 관통 여부
    var piercing: Boolean

    // ★ 변경됨: enemies의 타입이 MutableList<Enemy> -> List<EnemyBase>
    // (MutableList여도 상관없지만, 무기가 리스트를 수정(삭제)할 일은 없으므로 List가 더 안전합니다.
    //  하지만 기존 코드 호환성을 위해 MutableList<EnemyBase>로 하셔도 됩니다.
    //  여기선 GameView가 넘겨주는 타입에 맞춰 MutableList로 유지하겠습니다.)
    fun update(player: Player, enemies: MutableList<EnemyBase>, nowMs: Long)

    // 그리기
    fun draw(canvas: Canvas, px: Float, py: Float)

    // 레벨업 적용
    fun upgrade()
}