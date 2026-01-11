package com.luleme.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.luleme.ui.components.pin.NumPad
import com.luleme.ui.components.pin.PinDots
import com.luleme.ui.theme.CutePink
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Check PIN when input reaches 4 digits
    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            // Small delay for UX
            delay(100)
            if (viewModel.verifyPin(pinInput)) {
                onUnlocked()
            } else {
                errorMessage = "密码错误"
                pinInput = ""
                delay(1000)
                errorMessage = ""
            }
        }
    }

    // Auto unlock if disabled (handled by ViewModel logic + this effect)
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            // If verifyPin with empty string returns true, it means lock is disabled
            if (viewModel.verifyPin("")) {
                onUnlocked()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Only show content if we need to unlock (otherwise we navigated away)
            // But we can render it anyway
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
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
                    text = if (errorMessage.isNotEmpty()) errorMessage else "请输入密码解锁",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (errorMessage.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                PinDots(
                    length = 4,
                    inputLength = pinInput.length,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                NumPad(
                    onNumberClick = { num ->
                        if (pinInput.length < 4) {
                            pinInput += num
                            errorMessage = ""
                        }
                    },
                    onDeleteClick = {
                        if (pinInput.isNotEmpty()) {
                            pinInput = pinInput.dropLast(1)
                            errorMessage = ""
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
