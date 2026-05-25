package com.luleme.ui.screens.lock

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.luleme.ui.auth.SystemAuth
import com.luleme.ui.theme.CutePink

@Composable
fun LockScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var authStarted by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("请使用系统锁屏或生物识别解锁") }
    var canUseSystemAuth by remember { mutableStateOf(true) }

    fun showSystemAuthPrompt() {
        val hostActivity = activity ?: run {
            message = "当前页面不支持系统认证"
            canUseSystemAuth = false
            return
        }

        if (!SystemAuth.canAuthenticate(context)) {
            message = "请先在系统设置中启用锁屏密码或生物识别"
            canUseSystemAuth = false
            return
        }

        val prompt = BiometricPrompt(
            hostActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authStarted = false
                    message = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    message = "验证失败，请重试"
                }
            }
        )
        authStarted = true
        prompt.authenticate(SystemAuth.promptInfo(context))
    }

    LaunchedEffect(Unit) {
        if (!authStarted) {
            showSystemAuthPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = CutePink,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "欢迎回来",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    canUseSystemAuth = true
                    showSystemAuthPrompt()
                },
                enabled = canUseSystemAuth
            ) {
                Text("重新验证")
            }
            if (!canUseSystemAuth) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    }
                ) {
                    Text("前往系统设置")
                }
            }
        }
    }
}
