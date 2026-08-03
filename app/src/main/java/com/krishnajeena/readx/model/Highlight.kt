package com.krishnajeena.readx.model

import java.util.UUID

/**
 * A user-created highlight on a PDF page. Stored per-document in
 * SharedPreferences via [com.krishnajeena.readx.data.HighlightRepository].
 */
data class Highlight(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val startChar: Int,
    val endChar: Int,
    /** The highlighted text content (snapshot at creation time). */
    val text: String,
    /** ARGB colour; default is yellow. */
    val color: Long = 0xFFFFEB3B,
    /** Optional user note attached to this highlight. */
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
