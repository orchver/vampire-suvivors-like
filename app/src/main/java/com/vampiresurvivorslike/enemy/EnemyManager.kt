package com.vampiresurvivorslike.enemy

import com.vampiresurvivorslike.player.Player
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class EnemyManager {

    val enemies = mutableListOf<EnemyBase>()

    // 스폰 관련 변수
    private var spawnTimer = 0f
    private val SPAWN_INTERVAL = 1.0f
    private val SPAWN_RADIUS = 1200f

    // 보스 관련 변수
    var boss: BossEnemy? = null
    private var isBossSpawned = false
    private val BOSS_SPAWN_TIME = 480f // 8분 (테스트할 땐 10f로 줄여보세요)

    // [수정 1] updateAll에서 화면 크기(screenW, screenH)도 받아야 함 (보스 생성용)
    fun updateAll(dt: Float, targetX: Float, targetY: Float, totalTime: Float, player: Player, screenW: Int, screenH: Int) {

        // ★ 1. 보스 스폰 체크 logic
        if (!isBossSpawned && totalTime >= BOSS_SPAWN_TIME) {
            spawnBoss(player, targetX, targetY, screenW, screenH)
        }

        // 보스가 나오면 잡졸 스폰을 멈출지, 계속 할지 결정해야 함
        // (기획상 2페이즈 단독 전투라면 보스 나온 뒤엔 스폰 중지)
        if (!isBossSpawned) {
            // [수정 2] updateSpawner에게 player 객체도 전달 (Exploder 때문에 필요)
            updateSpawner(dt, targetX, targetY, totalTime, player)
        }

        // 기존 적 업데이트 로직
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.isAlive) {
                e.update(dt, targetX, targetY)
            }
        }
    }

    // [수정 3] player: Player 추가
    private fun updateSpawner(dt: Float, playerX: Float, playerY: Float, totalTime: Float, player: Player) {
        spawnTimer += dt

        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f

            val difficultyMultiplier = 1f + (totalTime / 60f) * 0.2f

            // [수정 4] player 전달
            spawnRandomEnemy(playerX, playerY, difficultyMultiplier, player)
        }
    }

    // [수정 5] player: Player 추가
    private fun spawnRandomEnemy(playerX: Float, playerY: Float, multiplier: Float, player: Player) {
        val angle = Random.nextFloat() * Math.PI * 2
        val spawnX = (playerX + cos(angle) * SPAWN_RADIUS).toFloat()
        val spawnY = (playerY + sin(angle) * SPAWN_RADIUS).toFloat()

        val chance = Random.nextInt(100)

        // ★ 주의: EnemySpawner 팩토리 클래스도 수정이 필요할 수 있습니다.
        // 만약 EnemySpawner 코드를 안 고쳤다면, 여기서 직접 생성자를 호출하는 게 안전합니다.
        when {
            chance < 40 -> { // Runner
                // 기존 팩토리 유지 (변경사항 없다고 가정)
                val group = EnemySpawner.spawnRunnerGroup(spawnX, spawnY, multiplier)
                enemies.addAll(group)
            }
            chance < 70 -> { // Stalker
                val enemy = EnemySpawner.spawnStalker(spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            chance < 85 -> { // Bruiser
                val enemy = EnemySpawner.spawnBruiser(spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            else -> { // Exploder (생성자 변경됨!)
                // EnemySpawner.spawnExploder가 아직 player를 안 받는다면 직접 생성
                // val enemy = EnemySpawner.spawnExploder(...) 대신 아래처럼:
                val enemy = EnemyExploder(spawnX, spawnY, multiplier, player)
                enemies.add(enemy)
            }
        }
    }

    // 충돌 감지 함수
    fun checkCollisions(player: Player) {
        for (enemy in enemies) {
            if (!enemy.isAlive) continue

            val dx = player.x - enemy.x
            val dy = player.y - enemy.y
            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (dist < player.radius + enemy.radius) {
                player.takeDamage(enemy.atk.toFloat())
            }
        }
    }

    // [수정 6] 화면 크기 인자 추가 및 잡졸 제거 로직
    private fun spawnBoss(player: Player, px: Float, py: Float, w: Int, h: Int) {
        isBossSpawned = true

        // ★ 기획 반영: 보스 등장 시 1:1 상황을 위해 기존 적 모두 제거
        enemies.clear()

        val bx = px
        val by = py - 600f

        // 보스 생성 (Player, Width, Height 모두 전달)
        val newBoss = BossEnemy(bx, by, player, w, h)

        boss = newBoss
        enemies.add(newBoss)
    }
}