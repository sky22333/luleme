package com.luleme.ui.screens.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.luleme.domain.model.Record
import com.luleme.ui.components.CuteCard
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("本周", "本月", "全部")
    var editingRecord by remember { mutableStateOf<Record?>(null) }
    var addingDate by remember { mutableStateOf<LocalDate?>(null) }
    var deletingRecord by remember { mutableStateOf<Record?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> WeekView(uiState.weekData)
            1 -> MonthView(
                state = uiState,
                onPreviousMonth = viewModel::showPreviousMonth,
                onNextMonth = viewModel::showNextMonth,
                onCurrentMonth = viewModel::showCurrentMonth,
                onDateClick = viewModel::selectDate
            )
            2 -> AllTimeView(uiState)
        }
    }

    val selectedDate = uiState.selectedDate
    if (selectedDate != null) {
        ModalBottomSheet(onDismissRequest = viewModel::clearSelectedDate) {
            DayRecordsContent(
                date = selectedDate,
                records = uiState.selectedDateRecords,
                onAdd = { addingDate = selectedDate },
                onEdit = { editingRecord = it },
                onDelete = { deletingRecord = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
            )
        }
    }

    val dateForAdd = addingDate
    if (dateForAdd != null) {
        RecordEditorDialog(
            title = "添加记录",
            initialDate = dateForAdd,
            initialTime = LocalTime.now().withSecond(0).withNano(0),
            initialNote = "",
            onDismiss = { addingDate = null },
            onSave = { timestamp, note ->
                viewModel.addRecord(timestamp, note)
                addingDate = null
            }
        )
    }

    val recordForEdit = editingRecord
    if (recordForEdit != null) {
        val dateTime = recordForEdit.timestamp.toLocalDateTime()
        RecordEditorDialog(
            title = "编辑记录",
            initialDate = dateTime.toLocalDate(),
            initialTime = dateTime.toLocalTime().withSecond(0).withNano(0),
            initialNote = recordForEdit.note.orEmpty(),
            onDismiss = { editingRecord = null },
            onSave = { timestamp, note ->
                viewModel.updateRecord(recordForEdit.copy(timestamp = timestamp, note = note))
                editingRecord = null
            }
        )
    }

    val recordForDelete = deletingRecord
    if (recordForDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text("删除这条记录吗？") },
            text = { Text("删除后无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(recordForDelete.id)
                        deletingRecord = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecord = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun WeekView(weekData: Map<DayOfWeek, Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val total = weekData.values.sum()
        Text(
            text = "$total",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("本周记录", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        if (weekData.isNotEmpty()) {
            val maxCount = weekData.values.maxOrNull() ?: 1
            var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                DayOfWeek.entries.forEach { day ->
                    val count = weekData[day] ?: 0
                    val isSelected = selectedDay == day
                    val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    
                    var animatedHeight by remember { mutableStateOf(0f) }
                    
                    LaunchedEffect(count) {
                        animatedHeight = heightFraction
                    }
                    
                    val animatedFraction by animateFloatAsState(
                        targetValue = animatedHeight,
                        animationSpec = tween(durationMillis = 800, delayMillis = day.ordinal * 100)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDay = if (isSelected) null else day }
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f) // Fill available vertical space
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter // Align bar to bottom
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Always show count
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(if (animatedFraction < 0.05f && count > 0) 0.05f else animatedFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (LocalDate.now().dayOfWeek == day) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = if (LocalDate.now().dayOfWeek == day) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthView(
    state: StatisticsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val monthData = state.monthData
    val visibleMonth = state.visibleMonth
    Column(modifier = Modifier.padding(16.dp)) {
        CuteCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${visibleMonth.year}年${visibleMonth.monthValue}月",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "共 ${monthData.values.sum()} 次",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "下个月")
                    }
                }

                if (visibleMonth.year != LocalDate.now().year || visibleMonth.month != LocalDate.now().month) {
                    TextButton(
                        onClick = onCurrentMonth,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("回到本月")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { 
                        Text(
                            text = it, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val dates = monthData.keys.sorted()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(300.dp) // Approximate height for calendar
                ) {
                    val firstDay = dates.firstOrNull()
                    if (firstDay != null) {
                        val paddingDays = firstDay.dayOfWeek.value - 1
                        items(paddingDays) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }
                    }

                    items(dates) { date ->
                        val count = monthData[date] ?: 0
                        val isToday = date == LocalDate.now()
                        val textColor = when {
                            count >= 3 -> Color.White
                            count == 2 -> MaterialTheme.colorScheme.onTertiary
                            count == 1 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .clickable { onDateClick(date) }
                                .background(
                                    color = when {
                                        count >= 3 -> MaterialTheme.colorScheme.error
                                        count == 2 -> MaterialTheme.colorScheme.tertiary
                                        count == 1 -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count >= 3) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else if (count > 0) {
                                // Empty for 1-2, just color
                            }

                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            if (isToday && count == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayRecordsContent(
    date: LocalDate,
    records: List<Record>,
    onAdd: () -> Unit,
    onEdit: (Record) -> Unit,
    onDelete: (Record) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("M月d日")),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (records.isEmpty()) "暂无记录" else "记录 ${records.size} 次",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.timestamp.toLocalDateTime().toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!record.note.isNullOrBlank()) {
                        Text(
                            text = record.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onEdit(record) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { onDelete(record) }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加一条记录")
        }
    }
}

@Composable
fun RecordEditorDialog(
    title: String,
    initialDate: LocalDate,
    initialTime: LocalTime,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (timestamp: Long, note: String?) -> Unit
) {
    var dateText by remember { mutableStateOf(initialDate.format(DateTimeFormatter.ISO_DATE)) }
    var timeText by remember { mutableStateOf(initialTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var noteText by remember { mutableStateOf(initialNote) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        error = null
                    },
                    label = { Text("日期") },
                    placeholder = { Text("yyyy-MM-dd") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = {
                        timeText = it
                        error = null
                    },
                    label = { Text("时间") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("备注，可选") },
                    minLines = 2
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timestamp = parseEditorTimestamp(dateText, timeText)
                    if (timestamp == null) {
                        error = "日期或时间格式不正确"
                    } else {
                        onSave(timestamp, noteText.trim().takeIf { it.isNotEmpty() })
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun parseEditorTimestamp(dateText: String, timeText: String): Long? {
    return try {
        val date = LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_DATE)
        val time = LocalTime.parse(timeText.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

@Composable
fun AllTimeView(state: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(title = "累计记录", value = "${state.totalCount}", unit = "次")
        StatCard(title = "最长连续", value = "${state.maxStreak}", unit = "天")
        StatCard(title = "平均频率", value = String.format("%.1f", state.averageFrequency), unit = "次/周")
        
        AdviceCard(frequency = state.averageFrequency)
    }
}

@Composable
fun AdviceCard(frequency: Float) {
    val message = when {
        frequency < 1.0f -> "频率控制得很好，继续保持！👍"
        frequency in 1.0f..3.0f -> "频率适中，生活很健康哦~ ✨"
        frequency > 3.0f -> "最近有点频繁，要注意身体休息哦 🛌"
        else -> "开始记录你的生活吧！"
    }
    
    CuteCard(
        backgroundColor = if (frequency > 3.0f) MaterialTheme.colorScheme.tertiaryContainer 
                         else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column {
            Text(
                text = "💡 近期建议",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (frequency > 3.0f) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (frequency > 3.0f) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, unit: String) {
    CuteCard {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
