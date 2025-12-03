package com.vampiresurvivorslike.enemy

import android.content.Context // ★ Context import 필수
import com.vampiresurvivorslike.player.Player

object EnemySpawner {

    // 1 type (group spawn)
    // ★ context 인자 추가
    fun spawnRunnerGroup(context: Context, x: Float, y: Float, multiplier: Float = 1.0f, count: Int = 3): List<EnemyRunner> {
        val list = mutableListOf<EnemyRunner>()

        repeat(count) {
            val offsetX = (-20..20).random().toFloat()
            val offsetY = (-20..20).random().toFloat()

            // ★ 생성자에 context 전달
            list.add(EnemyRunner(context, x + offsetX, y + offsetY, multiplier))
        }

        return list
    }

    // 2 type, slow
    // ★ context 인자 추가
    fun spawnStalker(context: Context, x: Float, y: Float, multiplier: Float = 1.0f): EnemyStalker {
        // ★ 생성자에 context 전달
        return EnemyStalker(context, x, y, multiplier)
    }

    // 3 type, charge
    // ★ context 인자 추가
    fun spawnBruiser(context: Context, x: Float, y: Float, multiplier: Float = 1.0f): EnemyBruiser {
        // ★ 생성자에 context 전달
        return EnemyBruiser(context, x, y, multiplier)
    }

    // 4 type, exploder
    // ★ context 인자 추가
    fun spawnExploder(context: Context, x: Float, y: Float, multiplier: Float = 1.0f, player: Player): EnemyExploder {
        // ★ 생성자에 context 전달
        return EnemyExploder(context, x, y, multiplier, player)
    }
}