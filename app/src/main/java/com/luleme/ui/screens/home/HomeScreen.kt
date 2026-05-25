package com.luleme.ui.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luleme.ui.components.CuteCard
import com.luleme.ui.theme.CuteOrange
import com.luleme.ui.theme.CutePink
import com.luleme.ui.theme.CuteYellow
import com.luleme.ui.theme.SecondaryLight
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "哎呀，出错了: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is HomeUiState.Success -> {
                HomeContent(
                    state = state,
                    onRecordClick = { viewModel.recordToday() }
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState.Success,
    onRecordClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp), // Space for FAB
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header with Greeting
        item {
            HeaderSection()
        }

        // 2. Main Status Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                TodayStatusCard(todayCount = state.todayRecords.size)
            }
        }

        // 3. Stats Section
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column {
                    Text(
                        text = "本周概览",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatsCard(
                                title = "次数",
                                value = "${state.weekCount}",
                                unit = "次",
                                icon = Icons.Rounded.Star,
                                iconTint = CuteYellow
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // Placeholder for future stat, using age for now or just generic info
                            StatsCard(
                                title = "状态",
                                value = if (state.todayRecords.isNotEmpty()) "贤者模式" else "活跃",
                                unit = "",
                                icon = Icons.Rounded.Favorite,
                                iconTint = CutePink
                            )
                        }
                    }
                }
            }
        }

        // 4. Health Tip
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                HealthTipCard(
                    frequency = state.weekCount,
                    age = state.age,
                    todayCount = state.todayRecords.size
                )
            }
        }
    }

    // Floating Action Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        TakeoffButton(
            hasRecordedToday = state.todayRecords.isNotEmpty(),
            onTakeoff = onRecordClick
        )
    }
}

@Composable
fun TakeoffButton(
    hasRecordedToday: Boolean,
    onTakeoff: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isTakingOff by remember { mutableStateOf(false) }
    var animatingHasRecordedToday by remember { mutableStateOf(hasRecordedToday) }
    val takeoffProgress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isTakingOff) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "button_scale"
    )

    val handleTakeoff = {
        if (!isTakingOff) {
            isTakingOff = true
            animatingHasRecordedToday = hasRecordedToday
            onTakeoff()
            
            scope.launch {
                takeoffProgress.snapTo(0f)
                takeoffProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 720
                        0f at 0 using FastOutSlowInEasing
                        0.18f at 90 using FastOutSlowInEasing
                        1f at 720 using FastOutSlowInEasing
                    }
                )
                takeoffProgress.snapTo(0f)
                isTakingOff = false
                animatingHasRecordedToday = hasRecordedToday
            }
        }
    }

    val showRecordedStyle = if (isTakingOff) animatingHasRecordedToday else hasRecordedToday

    if (!showRecordedStyle) {
        Box(contentAlignment = Alignment.Center) {
            TakeoffPulse(progress = takeoffProgress.value)
            ExtendedFloatingActionButton(
                onClick = { handleTakeoff() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 32.dp)
                    .scale(scale),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                TakeoffIcon(progress = takeoffProgress.value)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "起飞",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    } else {
        Box(contentAlignment = Alignment.Center) {
            TakeoffPulse(progress = takeoffProgress.value)
            Button(
                onClick = { handleTakeoff() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .height(48.dp)
                    .scale(scale)
            ) {
                TakeoffIcon(progress = takeoffProgress.value, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("再来一发")
            }
        }
    }
}

@Composable
private fun BoxScope.TakeoffPulse(progress: Float) {
    if (progress <= 0f) return

    val primaryAlpha = (1f - progress).coerceIn(0f, 1f)
    val secondaryProgress = ((progress - 0.18f) / 0.82f).coerceIn(0f, 1f)
    val secondaryAlpha = (1f - secondaryProgress).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .matchParentSize()
            .scale(1f + progress * 0.34f)
            .alpha(primaryAlpha * 0.26f)
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .scale(1f + secondaryProgress * 0.58f)
            .alpha(secondaryAlpha * 0.16f)
            .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.extraLarge)
    )
}

@Composable
private fun TakeoffIcon(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val launchProgress = ((progress - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val doneProgress = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Rounded.FlightTakeoff,
            contentDescription = null,
            modifier = modifier
                .graphicsLayer {
                    translationX = launchProgress * 42f
                    translationY = -launchProgress * 36f
                    rotationZ = -18f * launchProgress
                    alpha = 1f - launchProgress
                }
        )
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = modifier
                .alpha(doneProgress)
                .graphicsLayer {
                    scaleX = 0.72f + doneProgress * 0.28f
                    scaleY = 0.72f + doneProgress * 0.28f
                }
        )
    }
}

@Composable
fun HeaderSection() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    val greeting = getGreetingMessage()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = CuteOrange,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun getGreetingMessage(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 0..4 -> "凌晨好！机长"
        in 5..10 -> "早上好！机长"
        in 11..12 -> "中午好！机长"
        in 13..17 -> "下午好！机长"
        else -> "晚上好！机长"
    }
}

@Composable
fun TodayStatusCard(todayCount: Int) {
    val hasRecordedToday = todayCount > 0
    val gradientColors = if (hasRecordedToday) {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    } else {
        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(gradientColors))
            .padding(vertical = 24.dp, horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.3f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasRecordedToday) Icons.Rounded.Check else Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (hasRecordedToday) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.size(20.dp))
            
            Column {
                Text(
                    text = if (hasRecordedToday) "今日已起飞 $todayCount 次 ✨" else "今日还没起飞",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (hasRecordedToday) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasRecordedToday) "保持好心情~" else "别忘了爱自己哦",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (hasRecordedToday) Color.White.copy(alpha = 0.9f) 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    CuteCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HealthTipCard(frequency: Int, age: Int, todayCount: Int) {
    val recommended = getRecommendedWeeklyFrequency(age)
    val maxRecommended = recommended.last
    
    val message = when {
        todayCount >= 2 -> "今天起飞有点多次啦，注意身体哦机长 ✈️"
        todayCount == 1 -> "今天已经起飞啦，心情不错吧~ ✨"
        frequency > maxRecommended -> "最近有点频繁呢，注意劳逸结合哦 💙"
        else -> "节奏很健康！继续保持~ ✨"
    }
    
    val isHighFreq = todayCount >= 2 || frequency > maxRecommended
    
    CuteCard(
        backgroundColor = if (isHighFreq) MaterialTheme.colorScheme.tertiaryContainer 
                         else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column {
            Text(
                text = "💡 健康小贴士",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isHighFreq) MaterialTheme.colorScheme.onTertiaryContainer 
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHighFreq) MaterialTheme.colorScheme.onTertiaryContainer 
                        else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

fun getRecommendedWeeklyFrequency(age: Int): IntRange {
    return when (age) {
        in 18..25 -> 2..3
        in 26..35 -> 1..2
        in 36..45 -> 1..1
        else -> 1..1
    }
}
