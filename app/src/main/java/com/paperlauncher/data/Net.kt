package com.paperlauncher.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Http {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        request("GET", url, headers, null)
    }

    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: String = ""): String =
        withContext(Dispatchers.IO) { request("POST", url, headers, body) }

    suspend fun json(url: String, headers: Map<String, String> = emptyMap()) = JSONObject(get(url, headers))

    suspend fun bitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.inputStream.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    private fun request(method: String, url: String, headers: Map<String, String>, body: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "PaperLauncher/1.0")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw HttpException(code, text)
        return text
    }
}

class HttpException(val code: Int, body: String) : Exception("HTTP $code ${body.take(120)}")
