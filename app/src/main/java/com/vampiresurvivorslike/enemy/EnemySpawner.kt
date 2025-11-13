package com.vampiresurvivorslike.enemy

object EnemySpawner {

    // 1 type(group spawn)
    fun spawnRunnerGroup(x: Float, y: Float, count: Int = 3): List<EnemyRunner> {
        val list = mutableListOf<EnemyRunner>()

        repeat(count) {
            val offsetX = (-20..20).random().toFloat()
            val offsetY = (-20..20).random().toFloat()
            list.add(EnemyRunner(x + offsetX, y + offsetY))
        }

        return list
    }
    //2 type, slow
    fun spawnStalker(x: Float, y: Float): EnemyStalker {
        return EnemyStalker(x, y)
    }
    //3 type, charge
    fun spawnBruiser(x: Float, y: Float): EnemyBruiser {
        return EnemyBruiser(x, y)
    }
    //4 type, explode
    fun spawnExploder(x: Float, y: Float): EnemyExploder {
        return EnemyExploder(x, y)
    }
}