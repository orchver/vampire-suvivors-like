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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    fun SettingsScreen() {

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.title_bg), // 배경 이미지 리소스 (준비 필요)
                contentDescription = null,
                contentScale = ContentScale.Crop, // 화면 꽉 채우기
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Scaffold(
                containerColor = Color.Transparent, // Scaffold 배경색도 통일
                topBar = {
                    TopAppBar(
                        title = { Text("SETTINGS", color = Color.White, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0x80121212)
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 섹션 1: 사운드
                    Text(
                        text = "SOUND",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    VolumeControl()

                    Spacer(modifier = Modifier.height(32.dp))

                }
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
                            thumbColor = Color(0xFFD32F2F),
                            activeTrackColor = Color(0xFFD32F2F),
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 현재 볼륨 % 표시
                Text(
                    text = "${(currentVolume.toFloat() / maxVolume * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun SettingsScreenPreview() {
        MaterialTheme {
            Surface(color = Color(0xFF1E1E1E)) {
                SettingsScreen()
            }
        }
    }
}
