package com.vampiresurvivorslike.enemy

import android.content.Context
import com.vampiresurvivorslike.player.Player
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// [수정] 생성자에 context 추가
class EnemyManager(private val context: Context) {

    val enemies = mutableListOf<EnemyBase>()

    private var spawnTimer = 0f
    private val SPAWN_INTERVAL = 1.0f
    private val SPAWN_RADIUS = 1200f

    var boss: BossEnemy? = null
    private var isBossSpawned = false
    private val BOSS_SPAWN_TIME = 480f

    fun updateAll(dt: Float, targetX: Float, targetY: Float, totalTime: Float, player: Player, screenW: Int, screenH: Int) {
        if (!isBossSpawned && totalTime >= BOSS_SPAWN_TIME) {
            spawnBoss(player, targetX, targetY, screenW, screenH)
        }

        if (!isBossSpawned) {
            updateSpawner(dt, targetX, targetY, totalTime, player)
        }

        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.isAlive) {
                e.update(dt, targetX, targetY)
            }
        }
    }

    private fun updateSpawner(dt: Float, playerX: Float, playerY: Float, totalTime: Float, player: Player) {
        spawnTimer += dt
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f
            val difficultyMultiplier = 1f + (totalTime / 60f) * 0.2f
            spawnRandomEnemy(playerX, playerY, difficultyMultiplier, player)
        }
    }

    private fun spawnRandomEnemy(playerX: Float, playerY: Float, multiplier: Float, player: Player) {
        val angle = Random.nextFloat() * Math.PI * 2
        val spawnX = (playerX + cos(angle) * SPAWN_RADIUS).toFloat()
        val spawnY = (playerY + sin(angle) * SPAWN_RADIUS).toFloat()

        val chance = Random.nextInt(100)

        // [수정] EnemySpawner 함수들에 context 전달
        when {
            chance < 40 -> { // Runner
                val group = EnemySpawner.spawnRunnerGroup(context, spawnX, spawnY, multiplier)
                enemies.addAll(group)
            }
            chance < 70 -> { // Stalker
                val enemy = EnemySpawner.spawnStalker(context, spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            chance < 85 -> { // Bruiser
                val enemy = EnemySpawner.spawnBruiser(context, spawnX, spawnY, multiplier)
                enemies.add(enemy)
            }
            else -> { // Exploder
                // Exploder는 Player 추적이 필요하므로 player도 전달
                val enemy = EnemyExploder(context, spawnX, spawnY, multiplier, player)
                enemies.add(enemy)
            }
        }
    }

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

    private fun spawnBoss(player: Player, px: Float, py: Float, w: Int, h: Int) {
        isBossSpawned = true
        enemies.clear()

        val bx = px
        val by = py - 600f

        // [수정] 첫 번째 인자로 context를 넘겨줍니다.
        // EnemyManager는 이미 생성자에서 context를 받고 있으므로 그대로 전달하면 됩니다.
        val newBoss = BossEnemy(context, bx, by, player, w, h)

        boss = newBoss
        enemies.add(newBoss)
    }
}