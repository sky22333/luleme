package com.luleme.data.webdav

import android.util.Base64
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class WebDavConfig(
    val url: String,
    val username: String,
    val password: String,
    val directory: String
)

@Singleton
class WebDavClient @Inject constructor() {

    fun testConnection(config: WebDavConfig) {
        val connection = request(config, method = "OPTIONS")
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw IOException("WebDAV 认证失败")
            }
            if (code !in 200..299) {
                throw IOException("WebDAV 连接失败: $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun uploadLatest(config: WebDavConfig, content: String) {
        val connection = request(config, method = "PUT", fileName = LATEST_FILE_NAME)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("WebDAV 上传失败: $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun downloadLatest(config: WebDavConfig): String {
        val connection = request(config, method = "GET", fileName = LATEST_FILE_NAME)
        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw IOException("未找到云端备份")
            }
            if (code !in 200..299) {
                throw IOException("WebDAV 下载失败: $code")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun request(
        config: WebDavConfig,
        method: String,
        fileName: String? = null
    ): HttpURLConnection {
        val connection = URL(config.toRemoteUrl(fileName)).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json, */*")
        if (config.username.isNotBlank() || config.password.isNotBlank()) {
            val credentials = "${config.username}:${config.password}"
            val token = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $token")
        }
        return connection
    }

    private fun WebDavConfig.toRemoteUrl(fileName: String?): String {
        val base = url.trim().trimEnd('/')
        val dir = directory.trim().trim('/').takeIf { it.isNotEmpty() }
        return listOfNotNull(base, dir, fileName).joinToString("/")
    }

    private companion object {
        const val LATEST_FILE_NAME = "luleme-backup-latest.json"
        const val TIMEOUT_MS = 15_000
    }
}
