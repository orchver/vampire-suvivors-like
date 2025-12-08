package com.vampiresurvivorslike

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable

class TitleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LoginActivity에서 보낸 ID 받기 (없으면 guest)
        val userId = intent.getStringExtra("USER_ID") ?: "guest"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
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
                    .padding(bottom = 32.dp)
            )

            val buttonData = listOf(
                Triple("새로운 게임", R.drawable.start, 0.4f),
                Triple("저장된 게임", R.drawable.load, 0.4f),
                Triple("설정", R.drawable.settings, 0.6f),
                Triple("나가기", R.drawable.exit, 0.4f)
            )

            // 3. 리스트를 순회하며 이미지 버튼 생성
            buttonData.forEach { (label, imageRes, widthRatio) ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = label,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(widthRatio)
                        .clickable {
                            when (label) {
                                "새로운 게임" -> {
                                    Toast.makeText(context, "START NEW GAME", Toast.LENGTH_SHORT).show()
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
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun TitleScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E)
        ) {
            TitleScreen(userId = "Player1")
        }
    }
}
