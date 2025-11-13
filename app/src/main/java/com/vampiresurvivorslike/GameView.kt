package com.vampiresurvivorslike

import kotlin.math.sqrt
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
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

    // 🔹 레벨업 카드 데이터
    private data class LevelUpOption(
        val type: OptionType,
        val weaponType: String,   // "sword" / "axe" / "bow" / "talisman"
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
    private val enemyManager = EnemyManager()

    private var player: Player? = null
    private val enemies = mutableListOf<Enemy>()
    private val weapons = mutableListOf<Weapon>()

    private val joystick = Joystick()
    private var lastFrameNs = 0L
    private var lastSpawnMs = 0L

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

        // 무기 두 가지를 랜덤으로 뽑아서 선택 화면에 표시
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
            GameState.SELECT_WEAPON -> {
                // 무기 선택 화면에서는 게임 로직 없음
            }

            GameState.PLAYING -> {
                val p = player ?: return

                // 플레이어 이동
                p.updateByJoystick(joystick.axisX, joystick.axisY, dtSec, width, height)

                // 적 이동
                for (e in enemies) {
                    e.update(p.x, p.y)
                }
                //EnemyManager에 위치 넘김. 위의 코드 등은 충돌 우려로 일단 냅둠
                enemyManager.updateAll(dtSec, p.x, p.y)

                val nowMs = System.currentTimeMillis()

                // 무기 업데이트 (데미지만 넣고, 적 제거/경험치는 여기서 하지 않음)
                for (w in weapons) {
                    w.update(p, enemies, nowMs)
                }

                // 1) 죽은 적 → 경험치 구슬 생성 + 적 제거
                val itE = enemies.iterator()
                while (itE.hasNext()) {
                    val e = itE.next()
                    if (e.isDead) {
                        expOrbs += ExpOrb(e.x, e.y, e.expReward)
                        itE.remove()
                    }
                }
                // 경험치 구술 자석
                val magnetRadius = 3000f
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

                // 2) 플레이어가 경험치 구슬을 먹었는지 체크
                val itO = expOrbs.iterator()
                while (itO.hasNext()) {
                    val orb = itO.next()
                    if (orb.isCollected(p.x, p.y, p.radius)) {
                        p.gainExp(orb.value)   // 여기서 레벨업 발생 가능 (onLevelUp 콜백 호출)
                        itO.remove()
                    }
                }

                // 3) 적 스폰
                if (nowMs - lastSpawnMs >= 2000L && enemies.size < 25) {
                    spawnEnemies(3)
                    lastSpawnMs = nowMs
                }
            }

            GameState.LEVEL_UP -> {
                // 레벨업 선택 화면에서는 게임이 일시정지된 상태
            }
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

    /** 🔹 초기 무기 선택 화면 */
    private fun drawWeaponSelectScreen(c: Canvas) {
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
        val rightRect = RectF(
            width / 2f + width / 12f,
            height / 2f,
            width / 2f + width / 12f + optionW,
            height / 2f + optionH
        )

        c.drawRoundRect(leftRect, 40f, 40f, rectPaint)
        c.drawRoundRect(rightRect, 40f, 40f, rectPaint)

        paint.textSize = 50f
        c.drawText(option1.uppercase(), leftRect.centerX(), leftRect.centerY() + 20f, paint)
        c.drawText(option2.uppercase(), rightRect.centerX(), rightRect.centerY() + 20f, paint)
    }

    /** 🔹 실제 플레이 화면 그리기 */
    private fun drawGamePlay(c: Canvas) {
        val p = player

        enemies.forEach { it.draw(c) }
        expOrbs.forEach { it.draw(c) }

        if (p != null) {
            p.draw(c)
            weapons.forEach { it.draw(c, p.x, p.y) }

            // HUD
            c.drawText("ENEMY: ${enemies.size}", 24f, 48f, hud)
            c.drawText("LV ${p.level}  EXP ${p.exp}/${p.expToNext}", 24f, 96f, hud)
            c.drawText("HP ${p.hp.toInt()} / ${p.maxHp.toInt()}", 24f, 144f, hud)
        }

        joystick.draw(c)
    }

    /** 🔹 레벨업 카드 화면 (무기 추가 / 무기 강화 3개 중 택1) */
    private fun drawLevelUpScreen(c: Canvas) {
        // 현재 게임 화면 위에 반투명 오버레이
        drawGamePlay(c)

        val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("LEVEL UP!", width / 2f, height / 4f, titlePaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.FILL
        }
        val cardText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

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
                if (event.action == MotionEvent.ACTION_DOWN) {
                    handleLevelUpTouch(event.x, event.y)
                }
                return true
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 🔹 레벨업 카드 터치 처리 */
    private fun handleLevelUpTouch(x: Float, y: Float) {
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

    /** 🔹 선택한 업그레이드 옵션 적용 (새 무기 추가 또는 기존 무기 강화) */
    private fun applyLevelUpChoice(index: Int) {
        if (index !in currentLevelUpOptions.indices) return
        val option = currentLevelUpOptions[index]

        when (option.type) {
            OptionType.ADD_WEAPON -> {
                // 새 무기 생성 후 리스트에 추가
                val newWeapon = WeaponFactory.createWeapon(option.weaponType)
                weapons.add(newWeapon)
            }
            OptionType.UPGRADE_WEAPON -> {
                // 해당 타입의 무기를 찾아서 upgrade()
                val w = weapons.firstOrNull { weaponTypeOf(it) == option.weaponType }
                w?.upgrade()
            }
        }

        // 큐 처리
        levelUpQueue--
        if (levelUpQueue > 0) {
            // 아직 처리해야 할 레벨업이 남았으면 새로운 옵션 세트 생성
            prepareLevelUpOptions()
            gameState = GameState.LEVEL_UP
        } else {
            levelUpQueue = 0
            gameState = GameState.PLAYING
        }
    }

    /** 🔹 무기 선택 후 플레이어 생성 및 게임 시작 */
    private fun chooseWeapon(type: String) {
        val w = WeaponFactory.createWeapon(type)
        val p = Player(w)
        p.x = width / 2f
        p.y = height / 2f

        // 레벨업 발생 시 콜백 연결
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
        enemies.clear()
        expOrbs.clear()
        spawnEnemies(5)
        gameState = GameState.PLAYING
        lastSpawnMs = System.currentTimeMillis()
    }

    /** 🔹 현재 상태(보유 무기)에 따라 레벨업 옵션 3개 생성
     *  - 아직 없는 무기 → ADD_WEAPON
     *  - 가진 무기 중 레벨 < 3 → UPGRADE_WEAPON
     *  최대 3개 랜덤
     */
    private fun prepareLevelUpOptions() {
        val p = player ?: return

        val ownedTypes = weapons.map { weaponTypeOf(it) }.distinct()
        val addableTypes = availableTypes.filter { it !in ownedTypes }
        val upgradableWeapons = weapons.filter { it.level < 3 }

        val pool = mutableListOf<LevelUpOption>()

        // 새 무기 추가 후보
        for (t in addableTypes) {
            pool += LevelUpOption(
                type = OptionType.ADD_WEAPON,
                weaponType = t,
                description = "새 무기 획득: ${typeDisplayName(t)}"
            )
        }

        // 기존 무기 강화 후보
        for (w in upgradableWeapons) {
            val t = weaponTypeOf(w)
            val nextLv = w.level + 1
            pool += LevelUpOption(
                type = OptionType.UPGRADE_WEAPON,
                weaponType = t,
                description = "무기 강화 Lv.$nextLv: ${typeDisplayName(t)}"
            )
        }

        if (pool.isEmpty()) {
            // 더 이상 줄 업그레이드가 없으면 그냥 플레이 계속
            currentLevelUpOptions = emptyList()
            gameState = GameState.PLAYING
            levelUpQueue = 0
            return
        }

        pool.shuffle()
        currentLevelUpOptions = pool.take(3)
    }

    /** 🔹 무기 인스턴스 → 타입 문자열 */
    private fun weaponTypeOf(w: Weapon): String = when (w) {
        is Sword    -> "sword"
        is Axe      -> "axe"
        is Bow      -> "bow"
        is Talisman -> "talisman"
        else        -> "sword"
    }

    /** 🔹 UI용 무기 이름 (한글) */
    private fun typeDisplayName(type: String): String = when (type.lowercase()) {
        "sword"    -> "검"
        "axe"      -> "도끼"
        "bow"      -> "활"
        "talisman" -> "부적"
        else       -> type
    }

    /** 🔹 적 스폰 (나중에 여기서 expReward / hp / speed 다르게 해서 몬스터 종류 늘리면 됨) */
    private fun spawnEnemies(count: Int) {
        if (width == 0 || height == 0) return
        repeat(count) {
            when (Random.nextInt(4)) {
                0 -> enemies += Enemy(-30f, Random.nextFloat() * height)
                1 -> enemies += Enemy(width + 30f, Random.nextFloat() * height)
                2 -> enemies += Enemy(Random.nextFloat() * width, -30f)
                else -> enemies += Enemy(Random.nextFloat() * width, height + 30f)
            }
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hgt: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false }
}
