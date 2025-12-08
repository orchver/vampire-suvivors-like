package com.vampiresurvivorslike.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.vampiresurvivorslike.R
import com.vampiresurvivorslike.weapons.Weapon

class Player(context: Context, var weapon: Weapon) {

    var x = 0f
    var y = 0f
    val radius = 24f // 히트박스 크기는 유지 (게임 밸런스 위해)

    // 🔹 그래픽 관련 변수
    private var idleBitmap: Bitmap
    private var walkBitmap: Bitmap
    private var isMoving: Boolean = false
    private var facingRight: Boolean = true

    // ⭐ [수정] 이미지 확대 배율 (3배)
    private val visualScale = 3.0f
    private var bitmapSize = 0

    // 무적 관련
    private var isInvincible = false
    private var invincibleTimer = 0f
    private val INVINCIBILITY_DURATION = 0.5f

    // ─ 체력 및 스탯 (기존 변수명 100% 유지) ─
    var maxHp = 100f
    var hp = 100f
    var moveSpeed = 260f // BossEnemy와 호환되는 변수명

    var level: Int = 1
    var exp: Int = 0
    var expToNext: Int = 200

    var onLevelUp: (() -> Unit)? = null

    init {
        // 1. 이미지 로드
        val rawIdle = BitmapFactory.decodeResource(context.resources, R.drawable.player_idle)
        val rawWalk = BitmapFactory.decodeResource(context.resources, R.drawable.player_walk)

        // 2. 프레임 자르기
        val idleW = rawIdle.width / 5
        val walkW = rawWalk.width / 6

        val cropIdle = Bitmap.createBitmap(rawIdle, 0, 0, idleW, rawIdle.height)
        val cropWalk = Bitmap.createBitmap(rawWalk, 0, 0, walkW, rawWalk.height)

        // 3. ⭐ 크기 3배로 설정
        // 히트박스(radius*2) 기준 3배 크기로 비트맵 생성
        bitmapSize = (radius * 2 * visualScale).toInt()

        idleBitmap = Bitmap.createScaledBitmap(cropIdle, bitmapSize, bitmapSize, true)
        walkBitmap = Bitmap.createScaledBitmap(cropWalk, bitmapSize, bitmapSize, true)
    }

    // 조이스틱 업데이트
    fun updateByJoystick(ax: Float, ay: Float, dtSec: Float, w: Int, h: Int) {
        isMoving = (ax != 0f || ay != 0f)
        if (ax < 0) facingRight = false
        else if (ax > 0) facingRight = true

        x = (x + ax * moveSpeed * dtSec).coerceIn(radius, w - radius)
        y = (y + ay * moveSpeed * dtSec).coerceIn(radius, h - radius)
    }

    fun draw(canvas: Canvas) {
        val bitmap = if (isMoving) walkBitmap else idleBitmap

        canvas.save()

        // 좌우 반전
        if (!facingRight) {
            canvas.scale(-1f, 1f, x, y)
        }

        // ⭐ [중요] 이미지가 3배 커졌으므로, 중심점을 다시 맞춰줍니다.
        canvas.drawBitmap(bitmap, x - bitmapSize / 2f, y - bitmapSize / 2f, null)

        canvas.restore()
    }

    // ─ 이하 기존 로직 그대로 유지 ─
    fun heal(amount: Float) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }

    fun gainExp(amount: Int) {
        exp += amount
        while (exp >= expToNext) {
            exp -= expToNext
            level += 1
            expToNext += 340
            maxHp += 25f
            val missing = maxHp - hp
            val healInt = (missing * 0.75f).toInt()
            hp += healInt
            if (hp > maxHp) hp = maxHp
            onLevelUp?.invoke()
        }
    }

    fun updateTimer(dt: Float) {
        if (isInvincible) {
            invincibleTimer -= dt
            if (invincibleTimer <= 0f) isInvincible = false
        }
    }

    fun takeDamage(amount: Float) {
        if (isInvincible) return
        hp -= amount
        if (hp < 0) hp = 0f
        isInvincible = true
        invincibleTimer = INVINCIBILITY_DURATION
    }
}