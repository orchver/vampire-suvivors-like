package com.vampiresurvivorslike

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.vampiresurvivorslike.input.Joystick
import com.vampiresurvivorslike.player.Player
import com.vampiresurvivorslike.weapons.*
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // 🔹 게임 상태 (무기 선택 / 플레이 중)
    private enum class GameState { SELECT_WEAPON, PLAYING }
    private var gameState = GameState.SELECT_WEAPON

    private lateinit var thread: Thread
    @Volatile private var running = false

    private val bg = Paint().apply { color = Color.BLACK }
    private val hud = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 36f }

    private var player: Player? = null
    private val enemies = mutableListOf<Enemy>()
    private val weapons = mutableListOf<Weapon>()

    private val joystick = Joystick()
    private var lastFrameNs = 0L
    private var lastSpawnMs = 0L

    // 🔹 무기 선택 관련 데이터
    private val availableTypes = listOf("sword", "axe", "bow", "talisman")
    private var option1 = ""
    private var option2 = ""

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this).also { it.start() }
        lastFrameNs = System.nanoTime()

        // 🔹 무기 두 가지를 무작위로 선택
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
        if (gameState == GameState.PLAYING) {
            player?.updateByJoystick(joystick.axisX, joystick.axisY, dtSec, width, height)
            for (e in enemies) e.update(player!!.x, player!!.y)
            val nowMs = System.currentTimeMillis()
            for (w in weapons) w.update(player!!, enemies, nowMs)
            if (nowMs - lastSpawnMs >= 2000L && enemies.size < 25) {
                spawnEnemies(3)
                lastSpawnMs = nowMs
            }
        }
    }

    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            when (gameState) {
                GameState.SELECT_WEAPON -> drawWeaponSelectScreen(c)
                GameState.PLAYING -> drawGamePlay(c)
            }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    /** 🔹 무기 선택 화면 그리기 */
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

        // 왼쪽 옵션 상자
        val leftRect = RectF(width / 6f, height / 2f, width / 6f + optionW, height / 2f + optionH)
        // 오른쪽 옵션 상자
        val rightRect = RectF(width / 2f + width / 12f, height / 2f, width / 2f + width / 12f + optionW, height / 2f + optionH)

        c.drawRoundRect(leftRect, 40f, 40f, rectPaint)
        c.drawRoundRect(rightRect, 40f, 40f, rectPaint)

        paint.textSize = 50f
        c.drawText(option1.uppercase(), leftRect.centerX(), leftRect.centerY() + 20f, paint)
        c.drawText(option2.uppercase(), rightRect.centerX(), rightRect.centerY() + 20f, paint)
    }

    /** 🔹 실제 게임 화면 그리기 */
    private fun drawGamePlay(c: Canvas) {
        enemies.forEach { it.draw(c) }
        player?.draw(c)
        weapons.forEach { it.draw(c, player!!.x, player!!.y) }
        c.drawText("ENEMY: ${enemies.size}", 24f, 48f, hud)
        joystick.draw(c)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (gameState) {
            GameState.SELECT_WEAPON -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.x
                    val y = event.y
                    val centerY = height / 2f + 90f
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
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 🔹 무기 선택 후 플레이어 생성 및 게임 시작 */
    private fun chooseWeapon(type: String) {
        val w = WeaponFactory.createWeapon(type)
        val p = Player(w)
        p.x = width / 2f
        p.y = height / 2f
        player = p
        weapons.clear()
        weapons.add(w)
        enemies.clear()
        spawnEnemies(5)
        gameState = GameState.PLAYING
        lastSpawnMs = System.currentTimeMillis()
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
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false }
}
