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

    // 🔹 타이머 & 경험치 바
    private var gameStartMs: Long = 0L          // 게임 시작시간
    private var elapsedMs: Long = 0L           // 게임 지난시간
    private val maxTimeMs = 8 * 60 * 1000L     // 8분

    // (GameView 자체에서 쓰는 exp 변수는 안 써도 됨. Player 안의 exp 사용)
    private var exp: Int = 0                   // 경험치(여유분)
    private var expToLevel: Int = 200          // 업그레이드에 필요한 경험치(여유분)

    // 🔹 HUD용 페인트들
    private val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }
    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }
    private val hpBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val expBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private val weaponTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
    }
    private val circleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val circleEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

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

    // 🔹 ExpOrb 정의
    private data class ExpOrb(
        var x: Float,
        var y: Float,
        val value: Int,
        val radius: Float = 10f
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GREEN
        }

        fun draw(c: Canvas) {
            c.drawCircle(x, y, radius, paint)
        }

        fun isCollected(px: Float, py: Float, pr: Float): Boolean {
            val dx = px - x
            val dy = py - y
            val r = radius + pr
            return dx * dx + dy * dy <= r * r
        }
    }

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

    // -------------------------------------------------------------
    // SurfaceView 생명주기 & 메인 루프
    // -------------------------------------------------------------
    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this).also { it.start() }
        lastFrameNs = System.nanoTime()

        // 무기 두 가지를 랜덤으로 선택
        val shuffled = availableTypes.shuffled()
        option1 = shuffled[0]
        option2 = shuffled[1]
        gameState = GameState.SELECT_WEAPON

        // 상태 초기화
        levelUpQueue = 0
        player = null
        enemies.clear()
        weapons.clear()
        expOrbs.clear()
        gameStartMs = 0L
        elapsedMs = 0L

        joystick.ensureBase(width, height)
    }

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            val dtSec =
                ((now - lastFrameNs).coerceAtMost(100_000_000L)) / 1_000_000_000f
            lastFrameNs = now
            update(dtSec)
            drawFrame()
        }
    }

    // -------------------------------------------------------------
    // 업데이트 로직
    // -------------------------------------------------------------
    private fun update(dtSec: Float) {
        when (gameState) {
            GameState.SELECT_WEAPON -> { }

            GameState.PLAYING -> {
                val p = player ?: return
                  // [추가] 2. 시간 누적
                totalGameTime += dtSec
                // 1. 플레이어 이동
                p.updateByJoystick(joystick.axisX, joystick.axisY, dtSec, width, height)
                //무적 푸는 타머
                p.updateTimer(dtSec)

                // 2. 🚩 [변경] 적 이동 및 스폰 (EnemyManager에게 위임)
                // EnemyManager 내부에서 spawnTimer를 돌려 적을 추가하고, 살아있는 적을 이동시킴
                // [수정] 3. 적 업데이트 함수에 'totalGameTime' 전달
                enemyManager.updateAll(dtSec, p.x, p.y, totalGameTime, p, width, height)

                //충돌 검사
                enemyManager.checkCollisions(p)

                val nowMs = System.currentTimeMillis()

                // 🔸 타이머 갱신 (최대 8분)
                if (gameStartMs != 0L) {
                    val diff = nowMs - gameStartMs
                    elapsedMs = diff.coerceAtMost(maxTimeMs)
                }

                // 무기 업데이트
                for (w in weapons) {
                    w.update(p, enemyManager.enemies, nowMs)
                }

                // 4. 🚩 [변경] 죽은 적 처리 (경험치 생성 + 삭제)
                // EnemyManager 리스트를 직접 순회하며 죽은 적을 찾아냄
                val itE = enemyManager.enemies.iterator()
                while (itE.hasNext()) {
                    val e = itE.next()
                    if (e.isDead) {          // Enemy 쪽에 isDead, expReward 있다고 가정
                        expOrbs += ExpOrb(e.x, e.y, e.expReward)
                        itE.remove()
                    }
                }

                // 2) 경험치 구슬 자석 효과
                val magnetRadius = 3000f
                val magnetSpeed = 500f

                for (orb in expOrbs) {
                    val dx = p.x - orb.x
                    val dy = p.y - orb.y
                    val dist2 = dx * dx + dy * dy
                    if (dist2 <= magnetRadius * magnetRadius) {
                        val dist =
                            sqrt(dist2.toDouble()).toFloat().coerceAtLeast(1e-3f)
                        val vx = dx / dist * magnetSpeed
                        val vy = dy / dist * magnetSpeed
                        orb.x += vx * dtSec
                        orb.y += vy * dtSec
                    }
                }

                // 3) 플레이어가 경험치 구슬을 먹었는지 체크
                val itO = expOrbs.iterator()
                while (itO.hasNext()) {
                    val orb = itO.next()
                    if (orb.isCollected(p.x, p.y, p.radius)) {
                        p.gainExp(orb.value)   // Player 쪽에 gainExp / onLevelUp 있음
                        itO.remove()
                    }
                }

                // 4) 적 스폰
                if (nowMs - lastSpawnMs >= 2000L && enemies.size < 25) {
                    spawnEnemies(3)
                    lastSpawnMs = nowMs
                }
            }

            GameState.LEVEL_UP -> {
                // 레벨업 선택 화면에서는 게임 일시정지
            }
        }
    }

    // -------------------------------------------------------------
    // 그리기
    // -------------------------------------------------------------
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

        val leftRect =
            RectF(width / 6f, height / 2f, width / 6f + optionW, height / 2f + optionH)
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

    private fun drawGamePlay(c: Canvas) {
        val p = player

        // 🚩 [변경] EnemyManager의 리스트를 그리기
        enemyManager.enemies.forEach { it.draw(c) }

        expOrbs.forEach { it.draw(c) }

        if (p != null) {
            p.draw(c)
            // 🚩 [변경] 무기 그리기 (enemies 전달)
            weapons.forEach { it.draw(c, p.x, p.y) }

            // HUD(타이머, 경험치바, 체력바, 무기 업그레이드 동그라미)
            drawHUD(c)
        }

        joystick.draw(c)
    }

    /** 🔹 HUD (기획서 5번 화면) 그리기 */
    private fun drawHUD(c: Canvas) {
        val p = player ?: return

        // 1) 경과 시간 (7분부터 빨간색, 8분에서 멈춤)
        val secTotal = (elapsedMs / 1000).toInt()
        val min = secTotal / 60
        val sec = secTotal % 60
        val timeStr = String.format("%d:%02d", min, sec)

        timerPaint.color =
            if (elapsedMs >= 7 * 60 * 1000L) Color.RED else Color.WHITE
        c.drawText(timeStr, width / 2f, 60f, timerPaint)

        // 2) 경험치 바 (플레이어 exp / expToNext 사용)
        val barLeft = 40f
        val barRight = width - 40f
        val expTop = 80f
        val barHeight = 24f

        c.drawRect(barLeft, expTop, barRight, expTop + barHeight, barBgPaint)
        val expRatio =
            (p.exp.toFloat() / p.expToNext.toFloat()).coerceIn(0f, 1f)
        c.drawRect(
            barLeft,
            expTop,
            barLeft + (barRight - barLeft) * expRatio,
            expTop + barHeight,
            expBarPaint
        )

        // 3) 체력 바
        val hpTop = expTop + 40f
        c.drawRect(barLeft, hpTop, barRight, hpTop + barHeight, barBgPaint)
        val hpRatio = (p.hp / p.maxHp).coerceIn(0f, 1f)
        c.drawRect(
            barLeft,
            hpTop,
            barLeft + (barRight - barLeft) * hpRatio,
            hpTop + barHeight,
            hpBarPaint
        )

        // 4) 좌측 무기 업그레이드 동그라미
        val startX = 40f
        var y = hpTop + 80f
        val gapY = 40f
        val circleR = 10f
        val circleGap = 32f

        fun drawRow(label: String, level: Int) {
            c.drawText(label, startX, y, weaponTextPaint)
            val baseX = startX + 80f
            for (i in 0 until 4) { // 최대 4단계
                val cx = baseX + i * circleGap
                val cy = y - 14f
                if (i < level) {
                    c.drawCircle(cx, cy, circleR, circleFillPaint)
                } else {
                    c.drawCircle(cx, cy, circleR, circleEmptyPaint)
                }
            }
            y += gapY
        }

        // 현재 무기 레벨 표시(없으면 0)
        drawRow("검", getWeaponLevel<Sword>())
        drawRow("도끼", getWeaponLevel<Axe>())
        drawRow("활", getWeaponLevel<Bow>())
        drawRow("부적", getWeaponLevel<Talisman>())

        // 5) 적 수 표시 (왼쪽 상단)
        c.drawText("ENEMY: ${enemies.size}", 24f, hpTop + barHeight + 120f, hud)
    }

    // 특정 타입 무기의 레벨을 반환 (없으면 0)
    private inline fun <reified T : Weapon> getWeaponLevel(): Int {
        for (w in weapons) {
            if (w is T) return w.level
        }
        return 0
    }

    /** 🔹 레벨업 카드 화면 (무기 추가 / 무기 강화 3개 중 택1) */
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
        val totalWidth =
            cardWidth * currentLevelUpOptions.size + spacing * (currentLevelUpOptions.size - 1)
        val leftStart = (width - totalWidth) / 2f

        for (i in currentLevelUpOptions.indices) {
            val left = leftStart + i * (cardWidth + spacing)
            val rect = RectF(left, top, left + cardWidth, top + cardHeight)
            c.drawRoundRect(rect, 30f, 30f, cardPaint)
            c.drawText(currentLevelUpOptions[i].description, rect.centerX(), rect.centerY(), cardText)
        }
    }

    // -------------------------------------------------------------
    // 입력 처리
    // -------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // ... (기존 코드 동일) ...
        when (gameState) {
            GameState.SELECT_WEAPON -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.x
                    val y = event.y
                    val leftRange = width / 6f..(width / 6f + width / 3f)
                    val rightRange =
                        (width / 2f + width / 12f)..(width / 2f + width / 12f + width / 3f)

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
        val totalWidth =
            cardWidth * currentLevelUpOptions.size + spacing * (currentLevelUpOptions.size - 1)
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
                val w =
                    weapons.firstOrNull { weaponTypeOf(it) == option.weaponType }
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

        // 레벨업 발생 시 콜백 연결 (Player 안에 onLevelUp: (() -> Unit)? 가 있다고 가정)
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
        lastSpawnMs = System.currentTimeMillis()

        gameStartMs = lastSpawnMs
        elapsedMs = 0L
    }

    /** 🔹 현재 상태(보유 무기)에 따라 레벨업 옵션 3개 생성 */
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

    /** 🔹 적 스폰 */
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
    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
    }
}
