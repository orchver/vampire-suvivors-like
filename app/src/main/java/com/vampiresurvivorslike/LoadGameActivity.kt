package com.vampiresurvivorslike

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple

class LoadGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TitleActivity에서 전달받은 로그인 유저 ID
        val currentUserId = intent.getStringExtra("USER_ID") ?: "guest"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212) // 어두운 배경
                ) {
                    LoadGameScreen(currentUserId) { slotIndex ->
                        // 슬롯 선택 시 MainActivity로 이동하며 게임 시작
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("USER_ID", currentUserId)
                        intent.putExtra("loadSaved", true)      // 로드 모드 활성화
                        intent.putExtra("SLOT_INDEX", slotIndex) // 선택한 슬롯 번호 전달
                        startActivity(intent)
                        finish() // 로드 화면 종료
                    }
                }
            }
        }
    }
}

@Composable
fun LoadGameScreen(currentUserId: String, onSlotSelected: (Int) -> Unit) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val pref = remember { context.getSharedPreferences("VampireSave", Context.MODE_PRIVATE) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("저장된 게임 불러오기", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(vertical = 20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(5) { index ->
                // 저장 데이터 읽기
                val json = pref.getString("save_slot_$index", null)
                var slotText = "슬롯 ${index + 1} (빈 슬롯)"
                var subText = ""
                var isMyData = true
                var isClickable = false

                if (json != null) {
                    try {
                        val data = gson.fromJson(json, GameSaveData::class.java)
                        if (data.userId == currentUserId) {
                            slotText = "슬롯 ${index + 1} [Lv.${data.playerLevel}]"
                            subText = "${data.saveDate} | ${formatTimeCompose(data.elapsedMs)}"
                            isClickable = true
                        } else {
                            slotText = "슬롯 ${index + 1} (다른 유저: ${data.userId})"
                            subText = "불러올 수 없습니다"
                            isMyData = false
                        }
                    } catch (e: Exception) {
                        slotText = "데이터 오류"
                    }
                }

                // 슬롯 버튼 UI
                val borderColor = if (isMyData) Color.Gray else Color.Red
                val textColor = if (isMyData) Color.White else Color.Gray

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable(interactionSource = interactionSource, indication = null, enabled = isClickable) {
                            onSlotSelected(index)
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(text = slotText, color = textColor, fontSize = 18.sp)
                        if (subText.isNotEmpty()) {
                            Text(text = subText, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 뒤로가기 버튼
        Button(
            onClick = { (context as? ComponentActivity)?.finish() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp)
        ) {
            Text("취소")
        }
    }
}

// Compose용 시간 포맷 함수
fun formatTimeCompose(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}
