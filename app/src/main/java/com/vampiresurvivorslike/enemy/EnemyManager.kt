package com.vampiresurvivorslike.enemy

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class EnemyManager {

    val enemies = mutableListOf<EnemyBase>()

    // 스폰 관련 변수
    private var spawnTimer = 0f
    private val SPAWN_INTERVAL = 1.0f // 1초마다 스폰 시도
    private val SPAWN_RADIUS = 1200f  // 화면 밖 생성 거리

    // [수정 1] updateAll에서 totalTime을 받도록 함
    fun updateAll(dt: Float, targetX: Float, targetY: Float, totalTime: Float) {

        // [수정 2] updateSpawner에게 totalTime을 전달
        updateSpawner(dt, targetX, targetY, totalTime)

        // 기존 적 업데이트 로직
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.isAlive) {
                e.update(dt, targetX, targetY)
            }
        }
    }

    // [수정 3] 함수 정의에 totalTime: Float 추가 (에러 해결 핵심)
    private fun updateSpawner(dt: Float, playerX: Float, playerY: Float, totalTime: Float) {
        spawnTimer += dt

        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f

            // 난이도 배율 계산 (1분마다 20%씩 강해짐)
            val difficultyMultiplier = 1f + (totalTime / 60f) * 0.2f

            // [수정 4] 계산한 배율을 spawnRandomEnemy에게 전달
            spawnRandomEnemy(playerX, playerY, difficultyMultiplier)
        }
    }

    // [수정 5] 함수 정의에 multiplier: Float 추가 (에러 해결 핵심)
    private fun spawnRandomEnemy(playerX: Float, playerY: Float, multiplier: Float) {
        // 1. 플레이어 주변 랜덤 각도
        val angle = Random.nextFloat() * Math.PI * 2

        // 2. 화면 밖 좌표 계산
        val spawnX = (playerX + cos(angle) * SPAWN_RADIUS).toFloat()
        val spawnY = (playerY + sin(angle) * SPAWN_RADIUS).toFloat()

        // 3. 확률에 따른 몬스터 생성
        val chance = Random.nextInt(100)

        // 각 Spawner 함수에 multiplier 전달
        when {
            chance < 40 -> { // 40% Runner
                val group = EnemySpawner.spawnRunnerGroup(spawnX, spawnY, multiplier)
                enemies.addAll(group)
            }
            chance < 70 -> { // 30% Stalker
                val enemy = EnemySpawner.spawnStalker(spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            chance < 85 -> { // 15% Bruiser
                val enemy = EnemySpawner.spawnBruiser(spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            else -> { // 15% Exploder
                val enemy = EnemySpawner.spawnExploder(spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
        }
    }
}