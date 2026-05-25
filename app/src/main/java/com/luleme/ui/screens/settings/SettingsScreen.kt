package com.luleme.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luleme.BuildConfig
import com.luleme.ui.components.CuteSwitch
import com.luleme.ui.components.SettingGroup
import com.luleme.ui.components.SettingItem
import com.luleme.ui.auth.SystemAuth
import com.luleme.ui.theme.CutePink
import com.luleme.ui.theme.SecondaryLight
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showWebDavRestoreDialog by remember { mutableStateOf(false) }
    var webDavBusy by remember { mutableStateOf(false) }

    fun toggleSystemLock(enabled: Boolean) {
        if (enabled) {
            if (!SystemAuth.canAuthenticate(context)) {
                Toast.makeText(context, "请先在系统设置中启用锁屏密码或生物识别", Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                return
            }
        }
        viewModel.toggleLock(enabled)
    }

    // Export/Import Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = viewModel.getAllRecordsJson()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Unable to open backup file")
                    Toast.makeText(context, "数据导出成功 ✨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败了 😣", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val json = stream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
                        if (viewModel.restoreData(json)) {
                            Toast.makeText(context, "数据恢复成功 ✨", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "数据格式不对哦 😣", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "恢复失败了 😣", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("要清空所有数据吗？") },
            text = { Text("这个操作不能撤销哦，所有记录都会消失。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                        Toast.makeText(context, "数据已清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("再想想") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showWebDavDialog) {
        var url by remember(showWebDavDialog, uiState.webDavUrl) { mutableStateOf(uiState.webDavUrl) }
        var username by remember(showWebDavDialog, uiState.webDavUsername) { mutableStateOf(uiState.webDavUsername) }
        var password by remember(showWebDavDialog) { mutableStateOf("") }
        var directory by remember(showWebDavDialog, uiState.webDavDirectory) { mutableStateOf(uiState.webDavDirectory) }

        AlertDialog(
            onDismissRequest = { if (!webDavBusy) showWebDavDialog = false },
            title = { Text("WebDAV 配置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("https://example.com/dav") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (uiState.webDavPasswordSaved) "新密码（留空不改）" else "密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "备份文件会按当前导出格式上传，请妥善保管 WebDAV 账号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = directory,
                        onValueChange = { directory = it },
                        label = { Text("远程目录（可选）") },
                        placeholder = { Text("luleme") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            webDavBusy = true
                            val saved = viewModel.saveWebDavConfig(url, username, password, directory)
                            webDavBusy = false
                            if (saved) {
                                showWebDavDialog = false
                                Toast.makeText(context, "WebDAV 配置已保存", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "WebDAV 配置保存失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !webDavBusy
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWebDavDialog = false },
                    enabled = !webDavBusy
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showWebDavRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!webDavBusy) showWebDavRestoreDialog = false },
            title = { Text("从 WebDAV 恢复？") },
            text = { Text("恢复会用云端最新备份替换本地记录，请确认已经选对备份来源。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            webDavBusy = true
                            val restored = viewModel.restoreFromWebDav()
                            webDavBusy = false
                            showWebDavRestoreDialog = false
                            Toast.makeText(
                                context,
                                if (restored) "WebDAV 恢复成功 ✨" else "WebDAV 恢复失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !webDavBusy
                ) { Text("确认恢复") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWebDavRestoreDialog = false },
                    enabled = !webDavBusy
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        item {
            SettingGroup(title = "个人信息") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = "年龄: ${uiState.age} 岁",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.age.toFloat(),
                        onValueChange = { viewModel.updateAge(it.toInt()) },
                        valueRange = 18f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        item {
            SettingGroup(title = "安全与隐私") {
                SettingItem(
                    icon = Icons.Rounded.Lock,
                    title = "应用锁",
                    subtitle = "使用系统锁屏密码或生物识别",
                    iconTint = CutePink,
                    trailingContent = {
                        CuteSwitch(
                            checked = uiState.lockEnabled,
                            onCheckedChange = { toggleSystemLock(it) }
                        )
                    },
                    onClick = { toggleSystemLock(!uiState.lockEnabled) }
                )
            }
        }

        item {
            SettingGroup(title = "数据管理") {
                SettingItem(
                    icon = Icons.Rounded.Download,
                    title = "备份数据",
                    subtitle = "用于数据迁移等场景",
                    iconTint = SecondaryLight,
                    onClick = { exportLauncher.launch("luleme_data.json") }
                )
                
                SettingItem(
                    icon = Icons.Rounded.Upload,
                    title = "恢复数据",
                    subtitle = "从 JSON 文件导入",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
                )
                
                SettingItem(
                    icon = Icons.Rounded.Delete,
                    title = "清空所有数据",
                    subtitle = "慎重操作",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }
        }

        item {
            SettingGroup(title = "WebDAV 云备份") {
                SettingItem(
                    icon = Icons.Rounded.Lock,
                    title = "WebDAV 配置",
                    subtitle = if (uiState.webDavUrl.isBlank()) "未配置" else uiState.webDavUrl,
                    iconTint = CutePink,
                    onClick = { showWebDavDialog = true }
                )

                SettingItem(
                    icon = Icons.Rounded.Upload,
                    title = "测试连接",
                    subtitle = "验证服务器地址和账号密码",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (!webDavBusy) {
                            scope.launch {
                                webDavBusy = true
                                val ok = viewModel.testWebDavConnection()
                                webDavBusy = false
                                Toast.makeText(
                                    context,
                                    if (ok) "WebDAV 连接正常" else "WebDAV 连接失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                SettingItem(
                    icon = Icons.Rounded.Upload,
                    title = "备份到 WebDAV",
                    subtitle = "上传为 luleme-backup-latest.json",
                    iconTint = SecondaryLight,
                    onClick = {
                        if (!webDavBusy) {
                            scope.launch {
                                webDavBusy = true
                                val ok = viewModel.backupToWebDav()
                                webDavBusy = false
                                Toast.makeText(
                                    context,
                                    if (ok) "WebDAV 备份成功 ✨" else "WebDAV 备份失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                SettingItem(
                    icon = Icons.Rounded.Download,
                    title = "从 WebDAV 恢复",
                    subtitle = "下载云端最新备份并替换本地记录",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { if (!webDavBusy) showWebDavRestoreDialog = true }
                )
            }
        }
        
        item {
            Text(
                text = "Luleme v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clickable { uriHandler.openUri("https://github.com/sky22333/luleme") },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
