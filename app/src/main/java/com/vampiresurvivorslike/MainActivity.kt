package com.vampiresurvivorslike

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // 로그인/타이틀 화면에서 넘어온 유저 ID 받기
        val userId = intent.getStringExtra("USER_ID") ?: "guest"
        val shouldLoad = intent.getBooleanExtra("loadSaved", false)
        val slotIndex = intent.getIntExtra("SLOT_INDEX", -1) // [추가] 슬롯 번호 받기

        gameView = GameView(this)
        gameView.currentUserId = userId
        setContentView(gameView)

        if (shouldLoad && slotIndex != -1) {
            gameView.post {
                // [수정] loadGame 함수에 슬롯 번호 전달 필요
                gameView.loadGame(slotIndex)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //죽을데 save 안함
        if (::gameView.isInitialized && !gameView.isGameOver()) {
            gameView.saveGame()
        }
        gameView.saveGame()
    }
}
