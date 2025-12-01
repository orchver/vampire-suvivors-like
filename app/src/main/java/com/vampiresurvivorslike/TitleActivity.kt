package com.vampiresurvivorslike

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vampiresurvivorslike.SettingsActivity

class TitleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LoginActivity에서 보낸 ID 받기 (없으면 guest)
        val userId = intent.getStringExtra("USER_ID") ?: "guest"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212) // 로그인 화면과 동일한 짙은 검은색 배경
                ) {
                    TitleScreen(userId = userId)
                }
            }
        }
    }
}

@Composable
fun TitleScreen(userId: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 게임 타이틀 이미지 (로그인 화면과 동일)
        Image(
            painter = painterResource(id = R.drawable.title),
            contentDescription = "Main Title",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .padding(bottom = 32.dp) // 버튼과 간격 띄우기
        )



        // 버튼 텍스트 목록
        val buttonLabels = listOf("새로운 게임", "저장된 게임", "설정", "나가기")

        // 각 텍스트에 대해 스타일이 적용된 버튼 생성
        buttonLabels.forEach { label ->
            val isPrimary = label == "새로운 게임" // "새로운 게임"만 빨간색 강조

            Button(
                onClick = {
                    when (label) {
                        "새로운 게임" -> {
                            Toast.makeText(context, "새로운 게임을 시작합니다.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.putExtra("loadSaved", false)
                            intent.putExtra("USER_ID", userId)
                            context.startActivity(intent)
                        }
                        "저장된 게임" -> {
                            val intent = Intent(context, LoadGameActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            context.startActivity(intent)
                        }
                        "설정" -> {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                        "나가기" -> {
                            (context as? ComponentActivity)?.finish()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = if (isPrimary) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // 붉은색
                } else {
                    ButtonDefaults.buttonColors(containerColor = Color.DarkGray) // 나머지는 어두운 회색
                    // 혹은 테두리만 있는 스타일 -> 아래 주석 해제
                    // ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = label, fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TitleScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212)
        ) {
            TitleScreen(userId = "Player1")
        }
    }
}
