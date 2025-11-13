package com.vampiresurvivorslike.enemy

import kotlin.math.sqrt

abstract class EnemyBase(
    var x: Float,
    var y: Float,
    var hp: Int,
    var atk: Int,
    var moveSpeed: Float,
    var expValue: Int
) {
    var isAlive: Boolean = true

    // 공격 쿨타임(기본 0.75초)
    private var attackCooldown = 0.75f
    private var attackTimer = 0f

    abstract fun update(dt: Float, targetX: Float, targetY: Float)

    open fun onDamage(dmg: Int) {
        if (!isAlive) return
        hp -= dmg
        if (hp <= 0) die()
    }

    open fun die() {
        isAlive = false
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