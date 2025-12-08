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
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.BasicTextField


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = DBHelper(this)

        setContent {
            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    LoginScreen(
                        onLoginClick = { id, pw ->
                            if (dbHelper.login(id, pw)) {
                                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, TitleActivity::class.java)
                                intent.putExtra("USER_ID", id)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Login Failed, Check your ID/PW", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSignUpClick = { id, pw ->
                            if (dbHelper.insertUser(id, pw)) {
                                Toast.makeText(this, "Registration Success", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Registration Failed, ID already exitsts", Toast.LENGTH_SHORT).show()
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 게임 타이틀 (백귀야행)
            Image(
                painter = painterResource(id = R.drawable.title),
                contentDescription = "Main Title",
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .padding(bottom = 16.dp)
            )

            // 2. 아이디 입력창
            ImageTextField(
                value = id,
                onValueChange = { id = it },
                placeholder = "ID",
                bgImage = R.drawable.login_window
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 비밀번호 입력창
            ImageTextField(
                value = pw,
                onValueChange = { pw = it },
                placeholder = "PASSWORD",
                bgImage = R.drawable.login_window,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. 로그인 버튼
            Image(
                painter = painterResource(id = R.drawable.login),
                contentDescription = "로그인",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
                    .clickable { onLoginClick(id, pw) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 회원가입 버튼
            Image(

                painter = painterResource(id = R.drawable.register),
                contentDescription = "회원가입",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
                    .clickable {
                        onSignUpClick(id, pw)
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E)
        ) {
            LoginScreen(
                onLoginClick = { _, _ -> },
                onSignUpClick = { _, _ -> }
            )
        }
    }
}

@Composable
fun ImageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    bgImage: Int,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    // 이미지와 텍스트 필드를 겹침
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.Black, // 글자색
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    innerTextField() // 실제 커서와 텍스트
                }
            },
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }
}
