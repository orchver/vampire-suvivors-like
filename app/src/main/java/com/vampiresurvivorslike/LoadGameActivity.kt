package com.vampiresurvivorslike

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

class LoadGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TitleActivity에서 전달받은 로그인 유저 ID
        val currentUserId = intent.getStringExtra("USER_ID") ?: "guest"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E) // 어두운 배경
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.title_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.3f))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GAME LOAD",
                color = Color.Black,
                fontSize = 24.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(5) { index ->
                    // 저장 데이터 읽기
                    val json = pref.getString("save_slot_$index", null)
                    var slotText = "SLOT ${index + 1} (EMPTY SLOT)"
                    var subText = ""
                    var isMyData = true
                    var isClickable = false

                    if (json != null) {
                        try {
                            val data = gson.fromJson(json, GameSaveData::class.java)
                            if (data.userId == currentUserId) {
                                slotText = "SLOT ${index + 1} [Lv.${data.playerLevel}]"
                                subText = "${data.saveDate} | ${formatTimeCompose(data.elapsedMs)}"
                                isClickable = true
                            } else {
                                slotText = "SLOT ${index + 1} (Other User: ${data.userId})"
                                subText = "Cannot be loaded"
                                isMyData = false
                            }
                        } catch (e: Exception) {
                            slotText = "Data error"
                        }
                    }

                    val slotBackgroundColor = Color.Black.copy(alpha = 0.5f)

                    // 슬롯 버튼 UI
                    val borderColor = if (isMyData) Color.Gray else Color.Red
                    val textColor = if (isMyData) Color.White else Color.LightGray

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(color = slotBackgroundColor, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = isClickable
                            ) {
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
                Text("BACK")
            }
        }
    }
}

// Compose용 시간 포맷 함수
fun formatTimeCompose(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}

@Preview(showBackground = true)
@Composable
fun LoadGameScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E)
        ) {
            // 미리보기용 더미 데이터 설정이 필요할 수 있지만,
            // SharedPreference는 프리뷰에서 동작하지 않으므로
            // 기본적으로 "EMPTY SLOT"들이 보이거나 예외 처리가 된 화면이 뜹니다.
            LoadGameScreen(
                currentUserId = "Player1",
                onSlotSelected = { /* 클릭해도 아무 동작 안 함 */ }
            )
        }
    }
}
