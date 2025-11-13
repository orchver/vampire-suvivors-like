package com.vampiresurvivorslike.enemy

class EnemyManager {

    val enemies = mutableListOf<EnemyBase>()

    fun updateAll(dt: Float, targetX: Float, targetY: Float) {
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (!e.isAlive) {
                iter.remove()
                continue
            }
            e.update(dt, targetX, targetY)
        }
    }

    fun add(enemy: EnemyBase) {
        enemies.add(enemy)
    }
}