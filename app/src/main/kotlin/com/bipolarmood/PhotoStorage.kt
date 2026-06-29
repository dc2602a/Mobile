package com.bipolarmood

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object PhotoStorage {
    private const val DIR_NAME = "diary_photos"

    suspend fun persistPhotos(context: Context, uris: List<String>): List<String> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        uris.mapNotNull { uriString -> persistSingle(context, dir, uriString) }
    }

    private fun persistSingle(context: Context, dir: File, uriString: String): String? {
        return runCatching {
            if (uriString.startsWith("/")) {
                val existing = File(uriString)
                if (existing.exists() && existing.canonicalPath.startsWith(dir.canonicalPath)) {
                    return uriString
                }
            }
            val uri = Uri.parse(uriString)
            val input = when (uri.scheme) {
                "file" -> uri.path?.let { File(it).inputStream() }
                else -> context.contentResolver.openInputStream(uri)
            } ?: return null
            val file = File(dir, "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            input.use { stream -> file.outputStream().use { out -> stream.copyTo(out) } }
            file.absolutePath
        }.getOrNull()
    }
}
