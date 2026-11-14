package me.rerere.rikkahub.browser

import java.io.File
import java.net.URI

/**
 * Validates model-controlled file:// navigation. Ordinary approved roots contain
 * explicitly staged browser pages; a separate skill tree allows only web assets
 * beneath a named skill directory, never arbitrary skill credentials/config.
 *
 * Canonical paths prevent ../ and symlink escapes. This policy guards navigation,
 * NOT JavaScript-initiated file reads inside Chromium; only trusted local HTML
 * should be opened until the WebView file-origin settings are hardened.
 */
internal object BrowserFileSchemePolicy {
    private val SKILL_WEB_EXTENSIONS = setOf(
        "html", "htm", "js", "mjs", "css", "json", "svg", "png", "jpg",
        "jpeg", "webp", "gif", "ico", "woff", "woff2", "ttf",
        "mp3", "mp4", "ogg", "wav", "webm", "wasm",
    )

    fun isAllowed(
        url: String,
        roots: List<File>,
        skillAssetRoot: File? = null,
    ): Boolean = runCatching {
        val uri = URI(url.trim())
        if (!uri.scheme.equals("file", ignoreCase = true) ||
            uri.rawAuthority != null || !uri.path.orEmpty().startsWith("/")
        ) return@runCatching false
        // Query/fragment affect the HTML URL but not the filesystem path.
        val file = File(uri.path).canonicalFile
        if (!file.isFile) return@runCatching false
        if (roots.any { file.toPath().startsWith(it.canonicalFile.toPath()) }) {
            return@runCatching true
        }
        val skills = skillAssetRoot?.canonicalFile ?: return@runCatching false
        if (!file.toPath().startsWith(skills.toPath())) return@runCatching false
        // Require at least a named skill directory, not a file at skills root.
        val relative = skills.toPath().relativize(file.toPath())
        relative.nameCount >= 2 &&
            relative.none { it.toString().startsWith(".") } &&
            file.extension.lowercase() in SKILL_WEB_EXTENSIONS
    }.getOrDefault(false)

    /** Block page-initiated HTTP→file and all file→unapproved-file navigation.
     * App-initiated loadUrl bypasses shouldOverrideUrlLoading; browser_open
     * independently validates model-driven entry points. This is NOT a
     * subresource/XHR boundary. */
    fun blockPageFileNavigation(
        currentUrl: String?,
        destinationUrl: String,
        roots: List<File>,
        skillAssetRoot: File? = null,
    ): Boolean {
        if (!destinationUrl.startsWith("file:", ignoreCase = true)) return false
        if (!currentUrl.orEmpty().startsWith("file:", ignoreCase = true)) return true
        // Never trust an unapproved current file origin as an exemption: a
        // legacy app-launched file page must not navigate to a private file.
        return !isAllowed(destinationUrl, roots, skillAssetRoot)
    }
}
