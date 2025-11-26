package com.vampiresurvivorslike

import kotlin.math.sqrt
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.vampiresurvivorslike.enemy.EnemyBase
import com.vampiresurvivorslike.enemy.EnemyManager
import com.vampiresurvivorslike.input.Joystick
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.weapons.*
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // 🔹 전체 게임 상태
    private enum class GameState { SELECT_WEAPON, PLAYING, LEVEL_UP }
    private var gameState = GameState.SELECT_WEAPON

    // 🔹 레벨업 카드 타입
    private enum class OptionType { ADD_WEAPON, UPGRADE_WEAPON }
    // [추가] 1. 총 게임 진행 시간 (초 단위)
    private var totalGameTime = 0f
    // 🔹 레벨업 카드 데이터
    private data class LevelUpOption(
        val type: OptionType,
        val weaponType: String,
        val description: String
    )

    private var currentLevelUpOptions: List<LevelUpOption> = emptyList()

    private lateinit var thread: Thread
    @Volatile private var running = false

    private val bg = Paint().apply { color = Color.BLACK }
    private val hud = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
    }

    // 🚩 [변경] 기존 List<Enemy> 삭제하고 EnemyManager만 사용
    private val enemyManager = EnemyManager()

    private var player: Player? = null
    // private val enemies = mutableListOf<Enemy>()  <-- 삭제됨
    private val weapons = mutableListOf<Weapon>()

    private val joystick = Joystick()
    private var lastFrameNs = 0L
    // private var lastSpawnMs = 0L <-- 삭제됨 (EnemyManager 내부 타이머 사용)

    // 🔹 경험치 구슬
    private val expOrbs = mutableListOf<ExpOrb>()

    // 🔹 초기 무기 선택 관련
    private val availableTypes = listOf("sword", "axe", "bow", "talisman")
    private var option1 = ""
    private var option2 = ""

    // 🔹 한 번에 여러 레벨업이 발생할 수 있으므로 큐로 관리
    private var levelUpQueue = 0

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this).also { it.start() }
        lastFrameNs = System.nanoTime()

        val shuffled = availableTypes.shuffled()
        option1 = shuffled[0]
        option2 = shuffled[1]
        gameState = GameState.SELECT_WEAPON

        joystick.ensureBase(width, height)
    }

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            val dtSec = ((now - lastFrameNs).coerceAtMost(100_000_000L)) / 1_000_000_000f
            lastFrameNs = now
            update(dtSec)
            drawFrame()
        }
    }

    private fun update(dtSec: Float) {
        when (gameState) {
            GameState.SELECT_WEAPON -> { }

            GameState.PLAYING -> {
                val p = player ?: return
                // [추가] 2. 시간 누적
                totalGameTime += dtSec
                // 1. 플레이어 이동
                p.updateByJoystick(joystick.axisX, joystick.axisY, dtSec, width, height)

                // 2. 🚩 [변경] 적 이동 및 스폰 (EnemyManager에게 위임)
                // EnemyManager 내부에서 spawnTimer를 돌려 적을 추가하고, 살아있는 적을 이동시킴
                // [수정] 3. 적 업데이트 함수에 'totalGameTime' 전달
                enemyManager.updateAll(dtSec, p.x, p.y, totalGameTime)

                val nowMs = System.currentTimeMillis()

                // 3. 🚩 [변경] 무기 업데이트 (enemyManager.enemies 리스트 전달)
                // 주의: Weapon 클래스의 update 함수가 List<EnemyBase>를 받도록 수정되어 있어야 함
                for (w in weapons) {
                    w.update(p, enemyManager.enemies, nowMs)
                }

                // 4. 🚩 [변경] 죽은 적 처리 (경험치 생성 + 삭제)
                // EnemyManager 리스트를 직접 순회하며 죽은 적을 찾아냄
                val itE = enemyManager.enemies.iterator()
                while (itE.hasNext()) {
                    val e = itE.next()
                    if (!e.isAlive) { // EnemyBase의 isAlive 혹은 hp <= 0 체크
                        // EnemyBase에 expReward 속성이 있다고 가정 (없으면 e.expReward 대신 숫자 하드코딩)
                        expOrbs += ExpOrb(e.x, e.y, 10) // 임시로 경험치 10
                        itE.remove() // 리스트에서 제거
                    }
                }

                // 5. 경험치 구슬 자석 처리
                val magnetRadius = 300f // 3000f는 너무 커서 300f로 줄임 (필요시 수정)
                val magnetSpeed = 500f

                for (orb in expOrbs) {
                    val dx = p.x - orb.x
                    val dy = p.y - orb.y
                    val dist2 = dx * dx + dy * dy
                    if (dist2 <= magnetRadius * magnetRadius) {
                        val dist = sqrt(dist2.toDouble()).toFloat().coerceAtLeast(1e-3f)
                        val vx = dx / dist * magnetSpeed
                        val vy = dy / dist * magnetSpeed
                        orb.x += vx * dtSec
                        orb.y += vy * dtSec
                    }
                }

                // 6. 플레이어가 경험치 구슬 습득
                val itO = expOrbs.iterator()
                while (itO.hasNext()) {
                    val orb = itO.next()
                    if (orb.isCollected(p.x, p.y, p.radius)) {
                        p.gainExp(orb.value)
                        itO.remove()
                    }
                }

                // 적 스폰 로직은 이제 EnemyManager가 알아서 하므로 여기선 삭제함
            }

            GameState.LEVEL_UP -> { }
        }
    }

    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            when (gameState) {
                GameState.SELECT_WEAPON -> drawWeaponSelectScreen(c)
                GameState.PLAYING      -> drawGamePlay(c)
                GameState.LEVEL_UP     -> drawLevelUpScreen(c)
            }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    private fun drawWeaponSelectScreen(c: Canvas) {
        // ... (기존 코드 동일) ...
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("무기를 선택하세요", width / 2f, height / 4f, paint)

        val rectPaint = Paint().apply { color = Color.DKGRAY }
        val optionW = width / 3f
        val optionH = 180f
        val leftRect = RectF(width / 6f, height / 2f, width / 6f + optionW, height / 2f + optionH)
        val rightRect = RectF(width / 2f + width / 12f, height / 2f, width / 2f + width / 12f + optionW, height / 2f + optionH)

        c.drawRoundRect(leftRect, 40f, 40f, rectPaint)
        c.drawRoundRect(rightRect, 40f, 40f, rectPaint)

        paint.textSize = 50f
        c.drawText(option1.uppercase(), leftRect.centerX(), leftRect.centerY() + 20f, paint)
        c.drawText(option2.uppercase(), rightRect.centerX(), rightRect.centerY() + 20f, paint)
    }

    private fun drawGamePlay(c: Canvas) {
        val p = player

        // 🚩 [변경] EnemyManager의 리스트를 그리기
        enemyManager.enemies.forEach { it.draw(c) }

        expOrbs.forEach { it.draw(c) }

        if (p != null) {
            p.draw(c)
            // 🚩 [변경] 무기 그리기 (enemies 전달)
            weapons.forEach { it.draw(c, p.x, p.y) }

            // HUD
            c.drawText("ENEMY: ${enemyManager.enemies.size}", 24f, 48f, hud)
            c.drawText("LV ${p.level}  EXP ${p.exp}/${p.expToNext}", 24f, 96f, hud)
            c.drawText("HP ${p.hp.toInt()} / ${p.maxHp.toInt()}", 24f, 144f, hud)
        }

        joystick.draw(c)
    }

    private fun drawLevelUpScreen(c: Canvas) {
        // ... (기존 코드 동일) ...
        drawGamePlay(c)
        val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 64f; textAlign = Paint.Align.CENTER }
        c.drawText("LEVEL UP!", width / 2f, height / 4f, titlePaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; style = Paint.Style.FILL }
        val cardText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER }

        val cardWidth = width / 4f
        val cardHeight = 220f
        val top = height / 2f - cardHeight / 2f
        val spacing = width / 12f
        val totalWidth = cardWidth * currentLevelUpOptions.size + spacing * (currentLevelUpOptions.size - 1)
        val leftStart = (width - totalWidth) / 2f

        for (i in currentLevelUpOptions.indices) {
            val left = leftStart + i * (cardWidth + spacing)
            val rect = RectF(left, top, left + cardWidth, top + cardHeight)
            c.drawRoundRect(rect, 30f, 30f, cardPaint)
            c.drawText(currentLevelUpOptions[i].description, rect.centerX(), rect.centerY(), cardText)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // ... (기존 코드 동일) ...
        when (gameState) {
            GameState.SELECT_WEAPON -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.x
                    val y = event.y
                    val leftRange = width / 6f..(width / 6f + width / 3f)
                    val rightRange = (width / 2f + width / 12f)..(width / 2f + width / 12f + width / 3f)
                    if (y in (height / 2f)..(height / 2f + 180f)) {
                        if (x in leftRange) chooseWeapon(option1)
                        else if (x in rightRange) chooseWeapon(option2)
                    }
                }
                return true
            }
            GameState.PLAYING -> {
                val handled = joystick.onTouchEvent(event)
                if (handled) performClick()
                return handled || super.onTouchEvent(event)
            }
            GameState.LEVEL_UP -> {
                if (event.action == MotionEvent.ACTION_DOWN) handleLevelUpTouch(event.x, event.y)
                return true
            }
        }
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun handleLevelUpTouch(x: Float, y: Float) {
        // ... (기존 코드 동일) ...
        if (currentLevelUpOptions.isEmpty()) return
        val cardWidth = width / 4f
        val cardHeight = 220f
        val top = height / 2f - cardHeight / 2f
        val spacing = width / 12f
        val totalWidth = cardWidth * currentLevelUpOptions.size + spacing * (currentLevelUpOptions.size - 1)
        val leftStart = (width - totalWidth) / 2f

        for (i in currentLevelUpOptions.indices) {
            val left = leftStart + i * (cardWidth + spacing)
            val rect = RectF(left, top, left + cardWidth, top + cardHeight)
            if (x in rect.left..rect.right && y in rect.top..rect.bottom) {
                applyLevelUpChoice(i)
                break
            }
        }
    }

    private fun applyLevelUpChoice(index: Int) {
        // ... (기존 코드 동일) ...
        if (index !in currentLevelUpOptions.indices) return
        val option = currentLevelUpOptions[index]
        when (option.type) {
            OptionType.ADD_WEAPON -> {
                val newWeapon = WeaponFactory.createWeapon(option.weaponType)
                weapons.add(newWeapon)
            }
            OptionType.UPGRADE_WEAPON -> {
                val w = weapons.firstOrNull { weaponTypeOf(it) == option.weaponType }
                w?.upgrade()
            }
        }
        levelUpQueue--
        if (levelUpQueue > 0) {
            prepareLevelUpOptions()
            gameState = GameState.LEVEL_UP
        } else {
            levelUpQueue = 0
            gameState = GameState.PLAYING
        }
    }

    private fun chooseWeapon(type: String) {
        val w = WeaponFactory.createWeapon(type)
        val p = Player(w)
        p.x = width / 2f
        p.y = height / 2f

        p.onLevelUp = {
            levelUpQueue++
            if (gameState != GameState.LEVEL_UP) {
                prepareLevelUpOptions()
                gameState = GameState.LEVEL_UP
            }
        }

        player = p
        weapons.clear()
        weapons.add(w)

        // 🚩 [변경] 적 리스트 초기화 (EnemyManager 이용)
        enemyManager.enemies.clear()
        // enemyManager.spawnEnemies(5) <-- 초기 스폰 필요하면 EnemyManager에 함수 추가 필요.
        // 지금은 update 루프 돌면서 자동으로 스폰될 것이므로 놔둬도 됨.

        expOrbs.clear()
        gameState = GameState.PLAYING
    }

    private fun prepareLevelUpOptions() {
        // ... (기존 코드 동일) ...
        val p = player ?: return
        val ownedTypes = weapons.map { weaponTypeOf(it) }.distinct()
        val addableTypes = availableTypes.filter { it !in ownedTypes }
        val upgradableWeapons = weapons.filter { it.level < 3 }

        val pool = mutableListOf<LevelUpOption>()
        for (t in addableTypes) pool += LevelUpOption(OptionType.ADD_WEAPON, t, "새 무기 획득: ${typeDisplayName(t)}")
        for (w in upgradableWeapons) {
            val t = weaponTypeOf(w)
            val nextLv = w.level + 1
            pool += LevelUpOption(OptionType.UPGRADE_WEAPON, t, "무기 강화 Lv.$nextLv: ${typeDisplayName(t)}")
        }

        if (pool.isEmpty()) {
            currentLevelUpOptions = emptyList()
            gameState = GameState.PLAYING
            levelUpQueue = 0
            return
        }
        pool.shuffle()
        currentLevelUpOptions = pool.take(3)
    }

    private fun weaponTypeOf(w: Weapon): String = when (w) {
        is Sword    -> "sword"
        is Axe      -> "axe"
        is Bow      -> "bow"
        is Talisman -> "talisman"
        else        -> "sword"
    }

    private fun typeDisplayName(type: String): String = when (type.lowercase()) {
        "sword"    -> "검"
        "axe"      -> "도끼"
        "bow"      -> "활"
        "talisman" -> "부적"
        else       -> type
    }

    // 🚩 [삭제] spawnEnemies 함수는 이제 EnemyManager가 담당하므로 제거함

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hgt: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false }
}