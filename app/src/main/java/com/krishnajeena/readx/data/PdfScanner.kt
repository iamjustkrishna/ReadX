package com.krishnajeena.readx.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

data class ScannedPdf(
    val uri: Uri,
    val title: String,
    val sizeBytes: Long,
    val dateModified: Long
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedDate: String
        get() {
            if (dateModified <= 0) return ""
            val date = java.util.Date(dateModified * 1000)
            val format = java.text.SimpleDateFormat("MMM dd, yyyy  HH:mm", java.util.Locale.getDefault())
            return format.format(date)
        }
}


class PdfScanner(private val context: Context) {

    fun scanPdfs(): List<ScannedPdf> {
        val resultMap = LinkedHashMap<String, ScannedPdf>()

        // 1. MediaStore Scan
        scanMediaStore(resultMap)

        // 2. Direct File System Scan Fallback (Downloads, Documents, ExternalStorage)
        scanFileSystem(resultMap)

        return resultMap.values.sortedByDescending { it.dateModified }
    }

    private fun scanMediaStore(resultMap: MutableMap<String, ScannedPdf>) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf", "%.pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idColumn >= 0) cursor.getLong(idColumn) else -1L
                    val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val name = (if (nameColumn >= 0) cursor.getString(nameColumn) else null)
                        ?: dataPath?.let { File(it).name }
                        ?: "Document.pdf"
                    val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                    val date = if (dateColumn >= 0) cursor.getLong(dateColumn) else 0L

                    if (id != -1L) {
                        val contentUri: Uri = ContentUris.withAppendedId(collection, id)
                        val key = dataPath ?: name
                        resultMap[key] = ScannedPdf(contentUri, name, size, date)
                    }
                }
            }
        }
    }

    private fun scanFileSystem(resultMap: MutableMap<String, ScannedPdf>) {
        runCatching {
            val root = Environment.getExternalStorageDirectory()
            if (root != null && root.exists() && root.canRead()) {
                val foldersToScan = listOf(
                    root,
                    File(root, "Download"),
                    File(root, "Documents"),
                    File(root, "PDF")
                )
                foldersToScan.forEach { folder ->
                    if (folder.exists()) {
                        scanDirectoryRecursive(folder, resultMap, depth = 0)
                    }
                }
            }
        }
    }

    private fun scanDirectoryRecursive(
        dir: File,
        resultMap: MutableMap<String, ScannedPdf>,
        depth: Int
    ) {
        if (depth > 4) return // Avoid going too deep
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryRecursive(file, resultMap, depth + 1)
            } else if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true)) {
                val key = file.absolutePath
                if (!resultMap.containsKey(key)) {
                    val uri = Uri.fromFile(file)
                    resultMap[key] = ScannedPdf(
                        uri = uri,
                        title = file.name,
                        sizeBytes = file.length(),
                        dateModified = file.lastModified() / 1000
                    )
                }
            }
        }
    }
}
