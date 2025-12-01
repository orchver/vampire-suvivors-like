package com.vampiresurvivorslike

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = DBHelper(this)

        setContent {
            MaterialTheme {
                // 배경색 설정 (어두운 테마 느낌)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212) // 짙은 검은색 배경
                ) {
                    LoginScreen(
                        onLoginClick = { id, pw ->
                            if (dbHelper.login(id, pw)) {
                                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, TitleActivity::class.java)
                                intent.putExtra("USER_ID", id) // [추가] ID 전달
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "로그인 실패: 아이디나 비번을 확인하세요", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSignUpClick = { id, pw ->
                            if (dbHelper.insertUser(id, pw)) {
                                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "회원가입 실패: 이미 있는 아이디입니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String) -> Unit
) {
    // 입력값을 저장할 상태 변수
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), // 화면 테두리 여백
        horizontalAlignment = Alignment.CenterHorizontally, // 가로 중앙 정렬
        verticalArrangement = Arrangement.Center // 세로 중앙 정렬
    ) {
        // 1. 게임 타이틀 (백귀야행)
        Image(
            painter = painterResource(id = R.drawable.title),
            contentDescription = "Main Title",
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth, // 가로를 꽉 채움
            modifier = Modifier
                .fillMaxWidth(0.8f) // 화면 너비의 80%만큼 차지하게 설정 (너무 크면 0.6f 등으로 조절)
                .aspectRatio(1f)    // 이미지 비율을 1:1(정사각형)로 고정하여 찌그러짐 방지
                .padding(bottom = 16.dp)
        )

        // 2. 아이디 입력창
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("아이디", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp)) // 간격

        // 3. 비밀번호 입력창
        OutlinedTextField(
            value = pw,
            onValueChange = { pw = it },
            label = { Text("비밀번호", color = Color.Gray) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(), // 비밀번호 가리기
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp)) // 버튼과 입력창 사이 간격

        // 4. 로그인 버튼
        Button(
            onClick = { onLoginClick(id, pw) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // 붉은 버튼
            shape = RoundedCornerShape(8.dp) // 모서리 둥글게
        ) {
            Text("로그인", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp)) // 버튼 사이 간격

        // 5. 회원가입 버튼
        OutlinedButton(
            onClick = { onSignUpClick(id, pw) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("회원가입", fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212) // 실제 화면과 똑같이 배경색 적용
        ) {
            LoginScreen(
                onLoginClick = { _, _ -> }, // 미리보기이므로 빈 동작 전달
                onSignUpClick = { _, _ -> }
            )
        }
    }
}