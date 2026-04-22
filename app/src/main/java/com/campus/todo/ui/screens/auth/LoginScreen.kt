package com.campus.todo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.ui.AppViewModelFactory

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun LoginScreen(
    factory: AppViewModelFactory,
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = viewModel(factory = factory)
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var tipMessage by remember { mutableStateOf("") }

    val shape = RoundedCornerShape(20.dp)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF121A32),
        unfocusedContainerColor = Color(0xFF121A32),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = Color(0xFFF2F5FF),
        unfocusedTextColor = Color(0xFFF2F5FF),
        focusedPlaceholderColor = Color(0xFF7D88A5),
        unfocusedPlaceholderColor = Color(0xFF7D88A5),
        cursorColor = Color(0xFFF2F5FF)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A1A), Color(0xFF070D20), Color(0xFF040917))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(46.dp))
            Text(
                text = if (mode == AuthMode.LOGIN) {
                    stringResource(R.string.auth_login_title)
                } else {
                    stringResource(R.string.auth_register_title)
                },
                color = Color(0xFFF6F8FF),
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(32.dp))
            AuthLabel(stringResource(R.string.auth_username_label))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = username,
                onValueChange = {
                    username = it
                    tipMessage = ""
                },
                singleLine = true,
                shape = shape,
                placeholder = { Text(stringResource(R.string.auth_username_placeholder), fontSize = 12.sp) },
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), shape)
            )

            Spacer(modifier = Modifier.height(12.dp))
            AuthLabel(stringResource(R.string.auth_password_label))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = {
                    password = it
                    tipMessage = ""
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = shape,
                placeholder = { Text(stringResource(R.string.auth_password_placeholder), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF9CA6BE),
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                    )
                },
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), shape)
            )

            if (mode == AuthMode.REGISTER) {
                Spacer(modifier = Modifier.height(12.dp))
                AuthLabel(stringResource(R.string.auth_confirm_password_label))
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        tipMessage = ""
                    },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = shape,
                    placeholder = { Text(stringResource(R.string.auth_confirm_password_placeholder), fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF9CA6BE),
                            modifier = Modifier.clickable { confirmPasswordVisible = !confirmPasswordVisible }
                        )
                    },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x1AFFFFFF), shape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF9E58), Color(0xFFF06B86), Color(0xFFD958DF))
                        )
                    )
                    .clickable {
                        if (mode == AuthMode.LOGIN) {
                            vm.login(username, password) { message, ok ->
                                tipMessage = message.orEmpty()
                                if (ok) {
                                    onLoggedIn()
                                }
                            }
                        } else {
                            vm.register(username, password, confirmPassword) { message, ok ->
                                tipMessage = message.orEmpty()
                                if (ok) {
                                    mode = AuthMode.LOGIN
                                    password = ""
                                    confirmPassword = ""
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mode == AuthMode.LOGIN) {
                        stringResource(R.string.auth_login_button)
                    } else {
                        stringResource(R.string.auth_register_button)
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (tipMessage.isNotBlank()) {
                Text(
                    text = tipMessage,
                    color = if (tipMessage.endsWith("成功")) Color(0xFFA6EBC6) else Color(0xFFFF9B9B),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == AuthMode.LOGIN) {
                    Text(
                        text = stringResource(R.string.auth_no_account_hint),
                        color = Color(0xA6D5DCEC),
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(R.string.auth_go_register),
                        color = Color(0xFFF08B4A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable {
                                mode = AuthMode.REGISTER
                                tipMessage = ""
                            }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auth_has_account_hint),
                        color = Color(0xA6D5DCEC),
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(R.string.auth_go_login),
                        color = Color(0xFFF08B4A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable {
                                mode = AuthMode.LOGIN
                                confirmPassword = ""
                                tipMessage = ""
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFDCE4F7),
        fontSize = 13.sp
    )
}
