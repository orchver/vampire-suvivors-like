package com.vampiresurvivorslike

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // 전체 배경색을 짙은 검은색으로 통일
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        Scaffold(
            containerColor = Color(0xFF121212), // Scaffold 배경색도 통일
            topBar = {
                TopAppBar(
                    title = { Text("설정", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White // 아이콘 흰색
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF121212) // 앱바 배경색
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp), // 여백을 조금 더 넉넉하게
                horizontalAlignment = Alignment.Start // 왼쪽 정렬
            ) {
                // 섹션 1: 사운드
                Text(
                    text = "사운드",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F), // 붉은색 포인트
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                VolumeControl()

                Spacer(modifier = Modifier.height(32.dp))

                /*
                // 구분선
                HorizontalDivider(thickness = 1.dp, color = Color.DarkGray)

                Spacer(modifier = Modifier.height(32.dp))

                // 섹션 2: 그래픽 (예시)
                Text(
                    text = "그래픽",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F), // 붉은색 포인트
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "준비 중입니다...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                 */
            }
        }
    }

    @Composable
    fun VolumeControl() {
        val context = LocalContext.current
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var currentVolume by remember {
            mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = currentVolume.toFloat(),
                        onValueChange = { newValue ->
                            currentVolume = newValue.toInt()
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                currentVolume,
                                0
                            )
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        steps = if (maxVolume > 1) maxVolume - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD32F2F),       // 붉은색 핸들
                            activeTrackColor = Color(0xFFD32F2F), // 붉은색 채워진 트랙
                            inactiveTrackColor = Color.DarkGray   // 회색 빈 트랙
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 현재 볼륨 % 표시
                Text(
                    text = "${(currentVolume.toFloat() / maxVolume * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp) // 너비 고정해서 숫자 바뀔 때 흔들림 방지
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun SettingsScreenPreview() {
        MaterialTheme {
            Surface(color = Color(0xFF121212)) {
                SettingsScreen()
            }
        }
    }
}
