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

// Gson 관련 import
import android.content.SharedPreferences
import com.google.gson.Gson

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // [추가] 현재 로그인한 유저 ID (기본값은 guest)
    var currentUserId: String = "guest"

    // 🔹 타이머 & 경험치 바
    // 🚨 [수정] 중복 선언된 변수들 정리 (하나만 남김)
    private var gameStartMs: Long = 0L          // 게임 시작시간
    private var elapsedMs: Long = 0L           // 게임 지난시간
    private val maxTimeMs = 8 * 60 * 1000L     // 8분

    // 진행상황 저장 기능
    fun saveGame(slotIndex: Int = -1) {
        val p = player ?: return // 플레이어가 없으면 저장 안함

        val dateFormat =
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
        val dateStr = dateFormat.format(java.util.Date())

        // 1. 현재 무기 리스트를 저장용 정보로 변환
        val weaponSaveList = weapons.map { w ->
            WeaponSaveInfo(
                type = weaponTypeOf(w), // 기존에 만들어둔 함수 활용
                level = w.level
            )
        }

        // 2. 저장할 전체 데이터 객체 생성
        val saveData = GameSaveData(
            userId = currentUserId,
            saveDate = dateStr,
            elapsedMs = elapsedMs,
            playerHp = p.hp,
            playerMaxHp = p.maxHp,
            playerExp = p.exp,
            playerLevel = p.level,
            playerX = p.x,
            playerY = p.y,
            weapons = weaponSaveList
        )

        // 3. JSON 변환 및 SharedPreferences 저장
        val gson = Gson()
        val jsonString = gson.toJson(saveData)

        val pref = context.getSharedPreferences("VampireSave", Context.MODE_PRIVATE)
        pref.edit().putString("save_slot_$slotIndex", jsonString).apply()

        if (slotIndex != -1) {
            post {
                android.widget.Toast.makeText(
                    context,
                    "${slotIndex + 1}번 슬롯에 저장됨!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 진행상황 로드 기능
    fun loadGame(slotIndex: Int = 0): Boolean {
        val pref = context.getSharedPreferences("VampireSave", Context.MODE_PRIVATE)
        val fileName = "save_slot_$slotIndex"
        val jsonString = pref.getString(fileName, null) ?: return false

        try {
            val gson = Gson()
            val data = gson.fromJson(jsonString, GameSaveData::class.java)

            // 1. 게임 시간 복구
            elapsedMs = data.elapsedMs
            gameStartMs = System.currentTimeMillis() - elapsedMs

            totalGameTime = elapsedMs / 1000f

            // 2. 무기 복구
            weapons.clear()
            if (data.weapons.isNotEmpty()) {
                for (info in data.weapons) {
                    // 🚨 [수정] WeaponFactory에 context 전달
                    val newWeapon = WeaponFactory.createWeapon(context, info.type)
                    // 레벨만큼 강화 반복
                    repeat(info.level) {
                        newWeapon.upgrade()
                    }
                    weapons.add(newWeapon)
                }
            } else {
                // 🚨 [수정] WeaponFactory에 context 전달
                weapons.add(WeaponFactory.createWeapon(context, "sword"))
            }

            // 3. 플레이어 복구
            // 🚨 [수정] Player 생성자에 context 전달
            val restoredPlayer = Player(context, weapons[0])
            restoredPlayer.apply {
                hp = data.playerHp
                maxHp = data.playerMaxHp
                exp = data.playerExp
                level = data.playerLevel
                x = data.playerX
                y = data.playerY

                // 경험치 요구량 재계산
                expToNext = 200 * (1 shl (level - 1))

                // 레벨업 콜백 연결
                onLevelUp = {
                    levelUpQueue++
                    if (gameState != GameState.LEVEL_UP) {
                        prepareLevelUpOptions()
                        gameState = GameState.LEVEL_UP
                    }
                }
            }
            player = restoredPlayer

            // 4. 적 및 아이템 초기화
            enemyManager.enemies.clear()
            expOrbs.clear()

            // 5. 상태 변경
            gameState = GameState.PLAYING

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

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

    // 적 숫자 표시용
    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
    }

    // 🔹 전체 게임 상태
    private enum class GameState { SELECT_WEAPON, PLAYING, LEVEL_UP, PAUSED, SAVE_SELECT, GAME_OVER }

    private var gameState = GameState.SELECT_WEAPON

    // 🔹 레벨업 카드 타입
    private enum class OptionType { ADD_WEAPON, UPGRADE_WEAPON }

    // [EnemyManager용 시간]
    private var totalGameTime = 0f

    // 🔹 레벨업 카드 데이터
    private data class LevelUpOption(
        val type: OptionType,
        val weaponType: String,
        val description: String
    )

    private val pauseBtnRect = RectF() // 게임 중 일시정지 버튼 위치
    private val menuRects = Array(3) { RectF() } // 일시정지 메뉴 버튼들 (재개, 저장, 나가기)
    private val slotRects = Array(5) { RectF() } // 슬롯 5개 버튼들
    private val backBtnRect = RectF() // 슬롯 화면에서 뒤로가기

    private var currentLevelUpOptions: List<LevelUpOption> = emptyList()

    private lateinit var thread: Thread
    @Volatile
    private var running = false

    private val bg = Paint().apply { color = Color.BLACK }

    // 🚩 [기반] EnemyManager 사용
    private val enemyManager = EnemyManager(context)

    private var player: Player? = null
    private val weapons = mutableListOf<Weapon>()

    private val joystick = Joystick()
    private var lastFrameNs = 0L

    // 🔹 경험치 구슬
    private val expOrbs = mutableListOf<ExpOrb>()

    // 🔹 ExpOrb 정의
    private data class ExpOrb(
        var x: Float,
        var y: Float,
        val value: Int,
        val radius: Float = 10f
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN }
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
            GameState.SELECT_WEAPON -> {}

            GameState.PLAYING -> {
                val p = player ?: return
                // 시간 누적
                totalGameTime += dtSec

                // 1. 플레이어 이동 & 무적 타이머
                p.updateByJoystick(joystick.axisX, joystick.axisY, dtSec, width, height)
                p.updateTimer(dtSec)

                // 2. 🚩 [기반] 적 이동 및 스폰
                enemyManager.updateAll(dtSec, p.x, p.y, totalGameTime, p, width, height)

                // 충돌 검사
                enemyManager.checkCollisions(p)

                // 3. 타이머 갱신
                val nowMs = System.currentTimeMillis()
                if (gameStartMs != 0L) {
                    val diff = nowMs - gameStartMs
                    elapsedMs = diff.coerceAtMost(maxTimeMs)
                }

                // 4. 무기 업데이트
                for (w in weapons) {
                    w.update(p, enemyManager.enemies, nowMs)
                }

                // 5. 죽은 적 처리 (경험치 생성)
                val itE = enemyManager.enemies.iterator()
                while (itE.hasNext()) {
                    val e = itE.next()
                    if (!e.isAlive) {
                        expOrbs += ExpOrb(e.x, e.y, 10)
                        itE.remove()
                    }
                }

                // 6. 🚩 [이식] 강력한 자석 효과
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

                // 7. 경험치 습득
                val itO = expOrbs.iterator()
                while (itO.hasNext()) {
                    val orb = itO.next()
                    if (orb.isCollected(p.x, p.y, p.radius)) {
                        p.gainExp(orb.value)
                        itO.remove()
                    }

                }
                // 8. HP가 0이면 게임 오버 상태로 전환
                if (p.hp <= 0f) {
                    gameState = GameState.GAME_OVER
                }
            }

            GameState.LEVEL_UP -> {}
            GameState.PAUSED -> {}
            GameState.SAVE_SELECT -> {}
            GameState.GAME_OVER -> { }
        }
    }

    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            when (gameState) {
                GameState.SELECT_WEAPON -> drawWeaponSelectScreen(c)
                GameState.PLAYING -> drawGamePlay(c)
                GameState.LEVEL_UP -> drawLevelUpScreen(c)
                GameState.PAUSED -> drawPauseMenu(c)
                GameState.SAVE_SELECT -> drawSaveSelectScreen(c)
                GameState.GAME_OVER -> drawGameOverScreen(c)
            }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

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

    private fun drawGamePlay(c: Canvas) {
        val p = player

        enemyManager.enemies.forEach { it.draw(c) }
        expOrbs.forEach { it.draw(c) }

        if (p != null) {
            p.draw(c)
            weapons.forEach { it.draw(c, p.x, p.y) }
            drawHUD(c)
        }

        joystick.draw(c)
    }

    private fun drawPauseMenu(c: Canvas) {
        // 반투명 검은 배경
        c.drawColor(Color.argb(150, 0, 0, 0))

        val labels = listOf("RESUME", "SAVE", "TITLE")
        val btnW = 400f
        val btnH = 100f
        val gap = 40f
        val startY = height / 2f - (labels.size * (btnH + gap)) / 2f

        val rectPaint = Paint().apply { color = Color.LTGRAY }
        val textPaint =
            Paint().apply { color = Color.BLACK; textSize = 50f; textAlign = Paint.Align.CENTER }

        for (i in labels.indices) {
            val cx = width / 2f
            val cy = startY + i * (btnH + gap) + btnH / 2f
            val rect = RectF(cx - btnW / 2, cy - btnH / 2, cx + btnW / 2, cy + btnH / 2)
            menuRects[i] = rect

            c.drawRoundRect(rect, 20f, 20f, rectPaint)

            val fm = textPaint.fontMetrics
            val baseline = rect.centerY() - (fm.descent + fm.ascent) / 2
            c.drawText(labels[i], rect.centerX(), baseline, textPaint)
        }
    }

    private fun drawGameOverScreen(c: Canvas) {
        c.drawColor(Color.argb(200, 0, 0, 0))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("GAME OVER", width / 2f, height / 3f, titlePaint)

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        val p = player
        if (p != null) {
            val secTotal = (elapsedMs / 1000).toInt()
            val min = secTotal / 60
            val sec = secTotal % 60
            val timeStr = String.format("%d:%02d", min, sec)

            c.drawText("생존 시간 : $timeStr", width / 2f, height / 3f + 80f, infoPaint)
            c.drawText("도달 레벨 : ${p.level}", width / 2f, height / 3f + 140f, infoPaint)
        }

        c.drawText("화면을 터치하면 타이틀로 돌아갑니다", width / 2f, height * 0.7f, infoPaint)
    }

    private fun drawSaveSelectScreen(c: Canvas) {
        c.drawColor(Color.argb(220, 0, 0, 0))

        val titlePaint =
            Paint().apply { color = Color.WHITE; textSize = 60f; textAlign = Paint.Align.CENTER }
        c.drawText("SAVE SLOT", width / 2f, 100f, titlePaint)

        val btnW = 700f
        val btnH = 120f
        val gap = 30f
        val startY = 200f

        val slotBgPaint = Paint().apply { color = Color.DKGRAY }
        val lockedPaint =
            Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 5f }

        val mainTextPaint =
            Paint().apply { color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER }
        val subTextPaint =
            Paint().apply { color = Color.LTGRAY; textSize = 28f; textAlign = Paint.Align.CENTER }
        val emptyTextPaint =
            Paint().apply { color = Color.GRAY; textSize = 40f; textAlign = Paint.Align.CENTER }

        val pref = context.getSharedPreferences("VampireSave", Context.MODE_PRIVATE)
        val gson = Gson()

        for (i in 0 until 5) {
            val cx = width / 2f
            val cy = startY + i * (btnH + gap) + btnH / 2f
            val rect = RectF(cx - btnW / 2, cy - btnH / 2, cx + btnW / 2, cy + btnH / 2)
            slotRects[i] = rect

            c.drawRoundRect(rect, 20f, 20f, slotBgPaint)

            val json = pref.getString("save_slot_$i", null)

            if (json != null) {
                try {
                    val data = gson.fromJson(json, GameSaveData::class.java)
                    val infoText = "${data.userId} / ${data.saveDate}"
                    val progressText = "Lv.${data.playerLevel} (${formatTime(data.elapsedMs)})"

                    c.drawText(infoText, cx, cy - 15f, mainTextPaint)
                    c.drawText(progressText, cx, cy + 35f, subTextPaint)

                    if (data.userId != currentUserId) {
                        c.drawRoundRect(rect, 20f, 20f, lockedPaint)
                    }
                } catch (e: Exception) {
                    c.drawText("데이터 오류", cx, cy + 10f, emptyTextPaint)
                }
            } else {
                c.drawText("SLOT ${i + 1}", cx, cy + 15f, emptyTextPaint)
            }
        }

        backBtnRect.set(width / 2f - 100f, height - 150f, width / 2f + 100f, height - 50f)
        val backPaint = Paint().apply { color = Color.RED }
        c.drawRoundRect(backBtnRect, 20f, 20f, backPaint)
        c.drawText("BACK", backBtnRect.centerX(), backBtnRect.centerY() + 15f, mainTextPaint)
    }

    private fun formatTime(ms: Long): String {
        val sec = ms / 1000
        return "${sec / 60}:${String.format("%02d", sec % 60)}"
    }

    private fun drawHUD(c: Canvas) {
        val p = player ?: return

        val secTotal = (elapsedMs / 1000).toInt()
        val min = secTotal / 60
        val sec = secTotal % 60
        val timeStr = String.format("%d:%02d", min, sec)

        timerPaint.color = if (elapsedMs >= 7 * 60 * 1000L) Color.RED else Color.WHITE
        c.drawText(timeStr, width / 2f, 60f, timerPaint)

        val barLeft = 40f
        val barRight = width - 40f
        val expTop = 80f
        val barHeight = 24f

        c.drawRect(barLeft, expTop, barRight, expTop + barHeight, barBgPaint)
        val expRatio = (p.exp.toFloat() / p.expToNext.toFloat()).coerceIn(0f, 1f)
        c.drawRect(
            barLeft,
            expTop,
            barLeft + (barRight - barLeft) * expRatio,
            expTop + barHeight,
            expBarPaint
        )

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

        val startX = 40f
        var y = hpTop + 80f
        val gapY = 40f
        val circleR = 10f
        val circleGap = 32f

        fun drawRow(label: String, level: Int) {
            c.drawText(label, startX, y, weaponTextPaint)
            val baseX = startX + 80f
            for (i in 0 until 4) {
                val cx = baseX + i * circleGap
                val cy = y - 14f
                if (i < level) c.drawCircle(cx, cy, circleR, circleFillPaint)
                else c.drawCircle(cx, cy, circleR, circleEmptyPaint)
            }
            y += gapY
        }

        drawRow("검", getWeaponLevel<Sword>())
        drawRow("도끼", getWeaponLevel<Axe>())
        drawRow("활", getWeaponLevel<Bow>())
        drawRow("부적", getWeaponLevel<Talisman>())


        val btnSize = 80f
        val margin = 20f
        pauseBtnRect.set(width - btnSize - margin, margin, width - margin, margin + btnSize)

        val btnPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.FILL }
        c.drawRoundRect(pauseBtnRect, 10f, 10f, btnPaint)

        val textPaint =
            Paint().apply { color = Color.WHITE; textSize = 50f; textAlign = Paint.Align.CENTER }
        val fontMetrics = textPaint.fontMetrics
        val baseline = pauseBtnRect.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2
        c.drawText("||", pauseBtnRect.centerX(), baseline, textPaint)
    }

    private inline fun <reified T : Weapon> getWeaponLevel(): Int {
        for (w in weapons) {
            if (w is T) return w.level
        }
        return 0
    }

    private fun drawLevelUpScreen(c: Canvas) {
        drawGamePlay(c)
        val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 64f; textAlign = Paint.Align.CENTER
        }
        c.drawText("LEVEL UP!", width / 2f, height / 4f, titlePaint)

        val cardPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; style = Paint.Style.FILL }
        val cardText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER
        }

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
            c.drawText(
                currentLevelUpOptions[i].description,
                rect.centerX(),
                rect.centerY(),
                cardText
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
                if (event.action == MotionEvent.ACTION_DOWN && pauseBtnRect.contains(event.x, event.y)) {
                    gameState = GameState.PAUSED
                    return true
                }

                val handled = joystick.onTouchEvent(event)
                if (handled) performClick()
                return handled || super.onTouchEvent(event)
            }

            GameState.LEVEL_UP -> {
                if (event.action == MotionEvent.ACTION_DOWN) handleLevelUpTouch(event.x, event.y)
                return true
            }

            GameState.PAUSED -> {
                val x = event.x
                val y = event.y
                for (i in menuRects.indices) {
                    if (menuRects[i].contains(x, y)) {
                        when (i) {
                            0 -> gameState = GameState.PLAYING
                            1 -> gameState = GameState.SAVE_SELECT
                            2 -> {
                                (context as? android.app.Activity)?.finish()
                            }
                        }
                        break
                    }
                }
                return true
            }

            GameState.SAVE_SELECT -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.x
                    val y = event.y
                    val pref = context.getSharedPreferences("VampireSave", Context.MODE_PRIVATE)
                    val gson = Gson()

                    for (i in slotRects.indices) {
                        if (slotRects[i].contains(x, y)) {
                            val json = pref.getString("save_slot_$i", null)
                            var canSave = true
                            if (json != null) {
                                try {
                                    val data = gson.fromJson(json, GameSaveData::class.java)
                                    if (data.userId != currentUserId) {
                                        canSave = false
                                    }
                                } catch (e: Exception) {
                                }
                            }

                            if (canSave) {
                                saveGame(i)
                                gameState = GameState.PAUSED
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "다른 유저의 슬롯입니다! 덮어쓸 수 없습니다.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            return true
                        }
                    }
                    if (backBtnRect.contains(x, y)) {
                        gameState = GameState.PAUSED
                    }
                }
                return true
            }

            GameState.GAME_OVER -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    (context as? android.app.Activity)?.finish()
                }
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

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

    private fun applyLevelUpChoice(index: Int) {
        if (index !in currentLevelUpOptions.indices) return
        val option = currentLevelUpOptions[index]
        when (option.type) {
            OptionType.ADD_WEAPON -> {
                val newWeapon = WeaponFactory.createWeapon(context, option.weaponType)
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
        val w = WeaponFactory.createWeapon(context, type)
        val p = Player(context, w)
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

        enemyManager.enemies.clear()
        expOrbs.clear()
        gameState = GameState.PLAYING

        gameStartMs = System.currentTimeMillis()
        elapsedMs = 0L
    }

    private fun prepareLevelUpOptions() {
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

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hgt: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false }

    fun isGameOver(): Boolean = (gameState == GameState.GAME_OVER)
}

data class GameSaveData(
    val userId: String = "guest",
    val saveDate: String = "",
    val elapsedMs: Long,
    val playerHp: Float,
    val playerMaxHp: Float,
    val playerExp: Int,
    val playerLevel: Int,
    val playerX: Float,
    val playerY: Float,
    val weapons: List<WeaponSaveInfo>
)

data class WeaponSaveInfo(
    val type: String,
    val level: Int
)