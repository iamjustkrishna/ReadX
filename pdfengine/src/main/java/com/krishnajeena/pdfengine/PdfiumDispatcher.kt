package com.krishnajeena.pdfengine

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * The single entry point for all pdfium calls. pdfium is not thread-safe
 * (global state, even across documents), so every native call is confined to
 * this one thread. Serialization through the dispatcher also means close()
 * naturally queues after in-flight work.
 */
internal object Pdfium {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "pdfium").apply { priority = Thread.NORM_PRIORITY + 1 }
    }

    val dispatcher = executor.asCoroutineDispatcher()

    private var initialized = false

    suspend fun <T> run(block: () -> T): T = withContext(dispatcher) {
        if (!initialized) {
            NativePdfBridge.initLibrary()
            initialized = true
        }
        block()
    }
}
