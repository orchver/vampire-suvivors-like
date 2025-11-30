package com.vampiresurvivorslike.enemy

import com.vampiresurvivorslike.player.Player // ★ Player import 필수

object EnemySpawner {

    // 1 type (group spawn)
    fun spawnRunnerGroup(x: Float, y: Float, multiplier: Float = 1.0f, count: Int = 3): List<EnemyRunner> {
        val list = mutableListOf<EnemyRunner>()

        repeat(count) {
            val offsetX = (-20..20).random().toFloat()
            val offsetY = (-20..20).random().toFloat()
            // Runner 생성자가 (x, y, multiplier) 순서인지 확인 필요
            list.add(EnemyRunner(x + offsetX, y + offsetY, multiplier))
        }

        return list
    }

    // 2 type, slow
    fun spawnStalker(x: Float, y: Float, multiplier: Float = 1.0f): EnemyStalker {
        return EnemyStalker(x, y, multiplier)
    }

    // 3 type, charge
    fun spawnBruiser(x: Float, y: Float, multiplier: Float = 1.0f): EnemyBruiser {
        return EnemyBruiser(x, y, multiplier)
    }

    // ★ [수정됨] Player 객체를 받아서 생성자에 넘겨줌
    fun spawnExploder(x: Float, y: Float, multiplier: Float = 1.0f, player: Player): EnemyExploder {
        return EnemyExploder(x, y, multiplier, player)
    }
}