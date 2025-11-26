package com.vampiresurvivorslike.enemy

import android.graphics.Canvas
import kotlin.math.hypot
import kotlin.math.sqrt

abstract class EnemyBase(
    var x: Float,
    var y: Float,
    var hp: Int,
    var atk: Int,
    var moveSpeed: Float,
    var expReward: Int,
    val radius: Float = 20f
) {
    var isAlive: Boolean = true

    // 공격 쿨타임(기본 0.75초)
    private var attackCooldown = 0.75f
    private var attackTimer = 0f

    abstract fun update(dt: Float, targetX: Float, targetY: Float)

    // GameView에서 호출하므로 꼭 필요합니다.
    abstract fun draw(c: Canvas)

    // Weapon에서 이 이름(takeDamage)으로 호출합니다.
    // 리턴값: true면 사망, false면 생존
    open fun takeDamage(damage: Float): Boolean {
        if (!isAlive) return true

        hp -= damage.toInt()

        if (hp <= 0) {
            die()
            return true // 죽었음
        }
        return false // 아직 살았음
    }

    open fun die() {
        isAlive = false
        hp = 0
    }

    // ★ [추가됨] 기존 Enemy에 있던 넉백 기능 (Axe 무기에서 사용)
    fun knockback(dirX: Float, dirY: Float, power: Float) {
        val len = hypot(dirX, dirY).coerceAtLeast(0.0001f)
        val nx = dirX / len
        val ny = dirY / len
        x += nx * power
        y += ny * power
    }

    // ★ [추가됨] 기존 Enemy에 있던 원형 충돌 판정 (호환성 위해 추가)
    fun hitCircle(cx: Float, cy: Float, r: Float, damage: Float): Boolean {
        if (!isAlive) return false
        if (hypot(cx - x, cy - y) <= r + radius) {
            return takeDamage(damage)
        }
        return false
    }

    fun canAttack(): Boolean {
        return attackTimer >= attackCooldown
    }

    fun resetAttackTimer() {
        attackTimer = 0f
    }

    fun updateAttackTimer(dt: Float) {
        attackTimer += dt
    }

    // 거리 계산
    fun distanceTo(targetX: Float, targetY: Float): Float {
        val dx = targetX - x
        val dy = targetY - y
        return sqrt(dx * dx + dy * dy)
    }
}