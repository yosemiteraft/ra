package me.rerere.rikkahub.browser

import java.io.File
import java.net.URI

/** t-record: Only app-staged, existing HTML under files/webview may request the mic.
 * This top-level URL check is not an iframe isolation boundary.
 */
internal object BrowserRecorderPagePolicy {
    fun isTrusted(url: String?, cardsDir: File): Boolean = runCatching {
        if (url.isNullOrBlank()) return@runCatching false
        val uri = URI(url)
        if (!uri.scheme.equals("file", ignoreCase = true)) return@runCatching false
        val file = File(uri.path).canonicalFile
        file.isFile &&
            file.extension.lowercase() in setOf("html", "htm") &&
            file.toPath().startsWith(cardsDir.canonicalFile.toPath()) &&
            BrowserFileSchemePolicy.isAllowed(url, listOf(cardsDir))
    }.getOrDefault(false)

    fun isSafeRecordingId(id: String): Boolean =
        Regex("^rec_[0-9]{10,15}_[a-f0-9]{32}$").matches(id)
}
