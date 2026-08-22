package space.iamjustkrishna.readx.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * High-performance, two-tier (Memory + Disk) Cover Page Cache for PDF documents.
 * Eliminates stutter, lag, and repeated CPU rendering when scrolling or navigating screens.
 */
object PdfCoverCache {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val memoryCacheSize = (maxMemoryKb / 8).coerceAtLeast(1024 * 8) // ~1/8th of available RAM

    private val memoryCache = object : LruCache<String, Bitmap>(memoryCacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun getCacheKey(uri: Uri): String {
        val uriStr = uri.toString()
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(uriStr.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getDiskCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "pdf_covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Synchronous memory cache probe. Returns immediately (0ms) without I/O if available.
     */
    fun getBitmapFromMemory(uri: Uri): Bitmap? {
        val key = getCacheKey(uri)
        return memoryCache.get(key)
    }

    /**
     * Retrieves cover bitmap from Memory -> Disk -> PDF Renderer.
     */
    suspend fun getCoverBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int = 200,
        targetHeight: Int = 280
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = getCacheKey(uri)

        // 1. Memory Cache Check
        memoryCache.get(key)?.let { return@withContext it }

        // 2. Disk Cache Check
        val diskFile = File(getDiskCacheDir(context), "$key.jpg")
        if (diskFile.exists() && diskFile.length() > 0) {
            runCatching {
                val diskBitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (diskBitmap != null) {
                    memoryCache.put(key, diskBitmap)
                    return@withContext diskBitmap
                }
            }
        }

        // 3. Render from PDF
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val scale = 0.4f
                            val width = ((page.width * scale).toInt()).coerceIn(120, 360)
                            val height = ((page.height * scale).toInt()).coerceIn(160, 480)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Save to disk cache
                            runCatching {
                                FileOutputStream(diskFile).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }
                            }

                            // Save to memory cache
                            memoryCache.put(key, bmp)
                            return@withContext bmp
                        }
                    }
                }
            }
        }
        return@withContext null
    }

    /**
     * Preloads covers in the background for a smooth UX.
     */
    fun preloadCovers(context: Context, uris: List<Uri>, maxCount: Int = 30, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            uris.take(maxCount).forEach { uri ->
                val key = getCacheKey(uri)
                if (memoryCache.get(key) == null) {
                    getCoverBitmap(context, uri)
                }
            }
        }
    }

    fun clearCache(context: Context): Long {
        var freedBytes = 0L
        memoryCache.evictAll()
        val dir = getDiskCacheDir(context)
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
        }
        return freedBytes
    }
}
