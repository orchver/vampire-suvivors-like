package com.vampiresurvivorslike

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var running = false

    // ---------- 画笔 ----------
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }
    private val enemyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF5555.toInt() }
    private val projPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFF55.toInt() }
    private val gemPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF55FF88.toInt() }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 42f; typeface = Typeface.MONOSPACE
    }
    private val hudSmall   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt(); textSize = 28f; typeface = Typeface.MONOSPACE
    }
    private val joyBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF; style = Paint.Style.FILL }
    private val joyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val joyKnobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt() }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFFFFF; style = Paint.Style.FILL }
    private val btnStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val modalBg = Paint().apply { color = 0xCC000000.toInt() }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F2A38.toInt() }
    private val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5E7EB.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val cardText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 36f; typeface = Typeface.DEFAULT_BOLD }

    // ---------- 玩家 ----------
    private var px = 100f
    private var py = 100f
    private var vx = 0f
    private var vy = 0f
    private val playerRadius = 24f

    private var hp = 15
    private var maxHp = 15

    // ---------- 虚拟摇杆（左下固定） ----------
    private var joyBaseCx = 0f
    private var joyBaseCy = 0f
    private val joyBaseRadius = 110f
    private val joyKnobRadius = 38f
    private val joyMargin = 28f
    private var knobX = 0f
    private var knobY = 0f
    private var joyActive = false
    private var activePointerId = -1

    // 移动速度映射
    private val maxSpeed = 7.0f
    private val deadZone = 8f

    // ---------- 攻击按钮（右下） ----------
    private var atkCx = 0f
    private var atkCy = 0f
    private val atkR = 96f
    private var attackPressed = false
    private var attackCooldownMs = 100L
    private var lastAttackAt = 0L
    private var projSpeed = 12f
    private var projDamage = 10
    private var projCount = 1

    // ---------- 敌人与投射物 ----------
    data class Enemy(var x: Float, var y: Float, var hp: Int, var speed: Float, val r: Float = 18f)
    data class Proj(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float = 6f, var dmg: Int = 10)
    data class Gem(var x: Float, var y: Float, val r: Float = 10f, val xp: Int = 20)

    private val enemies = ArrayList<Enemy>()
    private val projs = ArrayList<Proj>()
    private val gems = ArrayList<Gem>()
    private var lastSpawnAt = 0L
    private var spawnIntervalMs = 1500L
    private var elapsedMs = 0L

    // ---------- 经验与升级 ----------
    private var xp = 0
    private var xpToNext = 200
    private var level = 1

    private var modalVisible = false
    private val upgradePool = listOf(
        "最大生命 +25" to { maxHp += 25; hp = min(hp + (0.75f * 25).toInt(), maxHp) },
        "投射物伤害 +5" to { projDamage += 5 },
        "攻击冷却 -10%" to { attackCooldownMs = (attackCooldownMs * 0.9).toLong().coerceAtLeast(120L) },
        "移动速度 +15%" to { /* 提升 maxSpeed 等效：用速度系数 */ speedScale *= 1.15f },
        "投射物数量 +1" to { projCount = (projCount + 1).coerceAtMost(5) },
        "投射物速度 +20%" to { projSpeed *= 1.2f }
    )
    private var speedScale = 1f
    private data class Card(val rect: RectF, val title: String, val apply: () -> Unit)
    private var cards: List<Card> = emptyList()

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    // 尺寸到位后，定位摇杆与按钮
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        joyBaseCx = joyMargin + joyBaseRadius
        joyBaseCy = h - joyMargin - joyBaseRadius
        knobX = joyBaseCx
        knobY = joyBaseCy

        atkCx = w - (joyMargin + atkR)
        atkCy = h - (joyMargin + atkR)

        if (px == 100f && py == 100f) { px = w * 0.5f; py = h * 0.5f }
    }

    // ---------- 生命周期 ----------
    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        gameThread = Thread(this, "GameLoop").also { it.start() }
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        gameThread?.joinSafely()
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    // ---------- 循环 ----------
    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dtMs = ((now - last) / 1_000_000).coerceAtLeast(0L)
            last = now

            if (!modalVisible) {
                elapsedMs += dtMs
                update(dtMs)
            }
            drawFrame()

            val ms = (System.nanoTime() - now) / 1_000_000
            val sleep = (16 - ms).coerceAtLeast(0L)
            if (sleep > 0) Thread.sleep(sleep)
        }
    }

    // ---------- 触控 ----------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (modalVisible) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val x = event.x; val y = event.y
                cards.firstOrNull { it.rect.contains(x, y) }?.let { card ->
                    card.apply.invoke()
                    modalVisible = false
                }
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x; val y = event.y
                if (isInAttackButton(x, y)) { attackPressed = true; tryAttack() }
                else if (x <= width * 0.5f) startJoystick(event, 0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val x = event.getX(idx); val y = event.getY(idx)
                if (isInAttackButton(x, y)) { attackPressed = true; tryAttack() }
                else if (!joyActive && x <= width * 0.5f) startJoystick(event, idx)
            }
            MotionEvent.ACTION_MOVE -> {
                if (joyActive) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx >= 0) updateJoystick(event.getX(idx), event.getY(idx))
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (joyActive && pointerId == activePointerId) stopJoystick()
                val x = event.getX(event.actionIndex); val y = event.getY(event.actionIndex)
                if (isInAttackButton(x, y)) attackPressed = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (joyActive) stopJoystick()
                attackPressed = false
            }
        }
        return true
    }

    private fun isInAttackButton(x: Float, y: Float): Boolean {
        return hypot(x - atkCx, y - atkCy) <= atkR
    }

    private fun startJoystick(event: MotionEvent, index: Int) {
        activePointerId = event.getPointerId(index)
        joyActive = true
        updateJoystick(event.getX(index), event.getY(index))
    }

    private fun updateJoystick(x: Float, y: Float) {
        val dx = x - joyBaseCx
        val dy = y - joyBaseCy
        val len = hypot(dx, dy)
        if (len <= joyBaseRadius) {
            knobX = x; knobY = y
        } else {
            val k = joyBaseRadius / (if (len == 0f) 1f else len)
            knobX = joyBaseCx + dx * k
            knobY = joyBaseCy + dy * k
        }
        val ndx = knobX - joyBaseCx
        val ndy = knobY - joyBaseCy
        val r = hypot(ndx, ndy)
        if (r <= deadZone) {
            vx = 0f; vy = 0f
        } else {
            val norm = r / joyBaseRadius
            val speed = (norm * (maxSpeed * speedScale)).toFloat()
            val nx = ndx / (if (r == 0f) 1f else r)
            val ny = ndy / (if (r == 0f) 1f else r)
            vx = nx * speed
            vy = ny * speed
        }
    }

    private fun stopJoystick() {
        joyActive = false
        activePointerId = -1
        knobX = joyBaseCx; knobY = joyBaseCy
        vx = 0f; vy = 0f
    }

    // ---------- Update ----------
    private fun update(dtMs: Long) {
        // 移动
        px += vx
        py += vy
        px = px.coerceIn(playerRadius, (width - playerRadius).coerceAtLeast(playerRadius))
        py = py.coerceIn(playerRadius, (height - playerRadius).coerceAtLeast(playerRadius))

        // 持续按住攻击按钮则自动连射
        if (attackPressed) tryAttack()

        // 敌人刷怪
        if (elapsedMs - lastSpawnAt >= spawnIntervalMs) {
            spawnEnemy()
            lastSpawnAt = elapsedMs
            // 逐渐加快
            spawnIntervalMs = (spawnIntervalMs * 0.98).toLong().coerceAtLeast(500L)
        }

        // 敌人AI：朝玩家移动
        enemies.forEach {
            val dx = px - it.x
            val dy = py - it.y
            val d = hypot(dx, dy).coerceAtLeast(1f)
            it.x += (dx / d) * it.speed
            it.y += (dy / d) * it.speed
        }

        // 投射物移动
        val itProj = projs.iterator()
        while (itProj.hasNext()) {
            val p = itProj.next()
            p.x += p.vx; p.y += p.vy
            // 出界移除
            if (p.x < -20 || p.x > width + 20 || p.y < -20 || p.y > height + 20) {
                itProj.remove(); continue
            }
            // 碰撞敌人
            var hit = false
            for (e in enemies) {
                if (dist2(p.x, p.y, e.x, e.y) <= (p.r + e.r) * (p.r + e.r)) {
                    e.hp -= p.dmg
                    hit = true
                    break
                }
            }
            if (hit) itProj.remove()
        }

        // 敌人死亡 -> 掉宝石
        val itEnemy = enemies.iterator()
        while (itEnemy.hasNext()) {
            val e = itEnemy.next()
            if (e.hp <= 0) {
                itEnemy.remove()
                gems += Gem(e.x, e.y)
            }
        }

        // 玩家与敌人碰撞受伤
        for (e in enemies) {
            if (dist2(px, py, e.x, e.y) <= (playerRadius + e.r) * (playerRadius + e.r)) {
                // 简单CD：每次撞到扣血并把敌人轻推开
                hp -= 1
                val dx = e.x - px; val dy = e.y - py
                val d = hypot(dx, dy).coerceAtLeast(1f)
                e.x += (dx / d) * 10f
                e.y += (dy / d) * 10f
            }
        }
        hp = hp.coerceIn(0, maxHp)

        // 吸附宝石：靠近则拾取
        val itGem = gems.iterator()
        while (itGem.hasNext()) {
            val g = itGem.next()
            val d2 = dist2(px, py, g.x, g.y)
            if (d2 <= (playerRadius + 18f) * (playerRadius + 18f)) {
                xp += g.xp
                itGem.remove()
            } else if (d2 <= 180f * 180f) {
                // 临近自动吸附
                val dx = px - g.x; val dy = py - g.y
                val d = sqrt(d2).coerceAtLeast(1f)
                g.x += (dx / d) * 6f
                g.y += (dy / d) * 6f
            }
        }

        // 升级
        while (xp >= xpToNext) {
            xp -= xpToNext
            level += 1
            maxHp += 25
            // 回复75%缺失生命
            val missing = maxHp - hp
            hp = (hp + (missing * 0.75f)).toInt().coerceAtMost(maxHp)
            xpToNext *= 2
            showLevelUpModal()
            break
        }
    }

    private fun tryAttack() {
        val now = System.currentTimeMillis()
        if (now - lastAttackAt < attackCooldownMs) return
        lastAttackAt = now
        shootProjectiles(projCount)
    }

    private fun shootProjectiles(count: Int) {
        // 方向：朝最近敌人；若没有敌人则朝玩家面前
        val dir = findShootDirection()
        val (dx, dy) = dir
        // 多发散射
        val spreadRad = 12 * PI / 180.0
        val baseAng = atan2(dy.toDouble(), dx.toDouble())
        val start = -((count - 1) / 2.0)
        for (i in 0 until count) {
            val ang = baseAng + (start + i) * spreadRad
            val vx = (cos(ang) * projSpeed).toFloat()
            val vy = (sin(ang) * projSpeed).toFloat()
            projs += Proj(px, py, vx, vy, dmg = projDamage)
        }
    }

    private fun findShootDirection(): Pair<Float, Float> {
        if (enemies.isEmpty()) return 1f to 0f
        var best: Enemy? = null
        var bestD2 = Float.MAX_VALUE
        for (e in enemies) {
            val d2 = dist2(px, py, e.x, e.y)
            if (d2 < bestD2) { bestD2 = d2; best = e }
        }
        val e = best!!
        val dx = e.x - px; val dy = e.y - py
        val d = hypot(dx, dy).coerceAtLeast(1f)
        return (dx / d) to (dy / d)
    }

    private fun spawnEnemy() {
        // 从屏幕边缘外生成
        val side = Random.nextInt(4)
        val margin = 40
        val x = when (side) {
            0 -> -margin.toFloat()
            1 -> (width + margin).toFloat()
            else -> Random.nextInt(width).toFloat()
        }
        val y = when (side) {
            2 -> -margin.toFloat()
            3 -> (height + margin).toFloat()
            else -> Random.nextInt(height).toFloat()
        }
        val hp = 20 + level * 2
        val speed = 1.4f + min(level, 30) * 0.03f
        enemies += Enemy(x, y, hp, speed)
    }

    // ---------- 升级弹窗 ----------
    private fun showLevelUpModal() {
        // 随机抽三张卡（不重复）
        val indices = upgradePool.indices.shuffled().take(3)
        val titles = indices.map { upgradePool[it].first }
        val apply = indices.map { upgradePool[it].second }

        val w = width.toFloat(); val h = height.toFloat()
        val cardW = w * 0.78f
        val cardH = 160f
        val gap   = 28f
        val startY = h * 0.28f

        cards = List(3) { i ->
            val top = startY + i * (cardH + gap)
            val rect = RectF((w - cardW) / 2f, top, (w + cardW) / 2f, top + cardH)
            Card(rect, titles[i], apply[i])
        }
        modalVisible = true
    }

    // ---------- Render ----------
    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            c.drawColor(Color.BLACK)

            // 宝石
            gems.forEach { c.drawCircle(it.x, it.y, it.r, gemPaint) }

            // 敌人
            enemies.forEach { c.drawCircle(it.x, it.y, it.r, enemyPaint) }

            // 投射物
            projs.forEach { c.drawCircle(it.x, it.y, it.r, projPaint) }

            // 玩家
            c.drawCircle(px, py, playerRadius, playerPaint)

            // HUD
            c.drawText("HP $hp/$maxHp  LV $level  EXP $xp/$xpToNext", 24f, 60f, textPaint)

            // 左下摇杆
            c.drawCircle(joyBaseCx, joyBaseCy, joyBaseRadius, joyBasePaint)
            c.drawCircle(joyBaseCx, joyBaseCy, joyBaseRadius, joyStrokePaint)
            c.drawCircle(knobX, knobY, joyKnobRadius, joyKnobPaint)

            // 右下攻击按钮 + 冷却扇形
            c.drawCircle(atkCx, atkCy, atkR, btnPaint)
            c.drawCircle(atkCx, atkCy, atkR, btnStroke)
            // 冷却提示
            val cd = (System.currentTimeMillis() - lastAttackAt).coerceAtLeast(0L)
            val ratio = (cd.toFloat() / attackCooldownMs).coerceIn(0f, 1f)
            val rect = RectF(atkCx - atkR, atkCy - atkR, atkCx + atkR, atkCy + atkR)
            val sweep = 360f * ratio
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF; style = Paint.Style.FILL }
            c.drawArc(rect, -90f, sweep, true, arcPaint)
            val label = if (ratio >= 1f) "ATTACK" else "RECHARGE"
            val tw = hudSmall.measureText(label)
            c.drawText(label, atkCx - tw / 2f, atkCy + 10f, hudSmall)

            // 升级弹窗
            if (modalVisible) {
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), modalBg)
                val title = "Level Up! 选择一项升级"
                val tW = textPaint.measureText(title)
                c.drawText(title, (width - tW) / 2f, height * 0.22f, textPaint)

                cards.forEach { card ->
                    c.drawRoundRect(card.rect, 16f, 16f, cardPaint)
                    c.drawRoundRect(card.rect, 16f, 16f, cardStroke)
                    val tw2 = cardText.measureText(card.title)
                    c.drawText(card.title, card.rect.centerX() - tw2 / 2f, card.rect.centerY() + 12f, cardText)
                }
            }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    // ---------- Utils ----------
    private fun Thread?.joinSafely() {
        try { this?.join(300) } catch (_: InterruptedException) {}
    }
    private fun dist2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return dx*dx + dy*dy
    }
}
