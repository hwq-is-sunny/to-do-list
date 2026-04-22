package com.campus.todo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.session.AccountStore
import com.campus.todo.data.session.SessionStore
import kotlinx.coroutines.launch

class LoginViewModel(
    private val accountStore: AccountStore,
    private val sessionStore: SessionStore
) : ViewModel() {

    fun login(
        username: String,
        password: String,
        onResult: (String?, Boolean) -> Unit
    ) {
        val name = username.trim()
        val pwd = password.trim()
        if (name.isBlank()) {
            onResult("请输入账号", false)
            return
        }
        if (pwd.isBlank()) {
            onResult("请输入密码", false)
            return
        }
        viewModelScope.launch {
            when (accountStore.verifyLogin(name, pwd)) {
                AccountStore.LoginResult.AccountNotFound -> onResult("没有这个账户", false)
                AccountStore.LoginResult.PasswordMismatch -> onResult("密码错误", false)
                AccountStore.LoginResult.Success -> {
                    sessionStore.login(name)
                    onResult("登录成功", true)
                }
            }
        }
    }

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        onResult: (String?, Boolean) -> Unit
    ) {
        val name = username.trim()
        val pwd = password.trim()
        val confirm = confirmPassword.trim()
        if (name.isBlank()) {
            onResult("请输入账号", false)
            return
        }
        if (pwd.isBlank()) {
            onResult("请输入密码", false)
            return
        }
        if (confirm.isBlank()) {
            onResult("请再次输入密码", false)
            return
        }
        if (pwd != confirm) {
            onResult("密码不一致", false)
            return
        }
        viewModelScope.launch {
            when (accountStore.register(name, pwd)) {
                AccountStore.RegisterResult.Exists -> onResult("该账号已存在", false)
                AccountStore.RegisterResult.Success -> onResult("注册成功", true)
            }
        }
    }
}
