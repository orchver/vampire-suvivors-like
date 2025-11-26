package com.vampiresurvivorslike.enemy

object EnemySpawner {

    // 1 type(group spawn)
    // [수정] multiplier 추가 (기본값 1.0f)
    fun spawnRunnerGroup(x: Float, y: Float, multiplier: Float = 1.0f, count: Int = 3): List<EnemyRunner> {
        val list = mutableListOf<EnemyRunner>()

        repeat(count) {
            val offsetX = (-20..20).random().toFloat()
            val offsetY = (-20..20).random().toFloat()
            // EnemyRunner 생성자도 multiplier를 받도록 수정 필요
            list.add(EnemyRunner(x + offsetX, y + offsetY, multiplier))
        }

        return list
    }
    //2 type, slow
    fun spawnStalker(x: Float, y: Float, multiplier: Float = 1.0f): EnemyStalker {
        return EnemyStalker(x, y, multiplier)
    }
    //3 type, charge
    fun spawnBruiser(x: Float, y: Float, multiplier: Float = 1.0f): EnemyBruiser {
        // 이미 수정된 EnemyBruiser 사용
        return EnemyBruiser(x, y, multiplier)
    }

    fun spawnExploder(x: Float, y: Float, multiplier: Float = 1.0f): EnemyExploder {
        // EnemyExploder 생성자도 multiplier를 받도록 수정 필요
        return EnemyExploder(x, y, multiplier)
    }
}