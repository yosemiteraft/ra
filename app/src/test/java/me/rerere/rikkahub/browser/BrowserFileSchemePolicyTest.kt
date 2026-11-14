package me.rerere.rikkahub.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BrowserFileSchemePolicyTest {
    @Test fun `approved local HTML and sibling JS resolve`() {
        val root = Files.createTempDirectory("browser-file-policy").toFile()
        try {
            val html = File(root, "index.html").apply { writeText("<script src='app.js'></script>") }
            val js = File(root, "app.js").apply { writeText("window.ready = true") }
            assertTrue(BrowserFileSchemePolicy.isAllowed(html.toURI().toString(), listOf(root)))
            assertTrue(BrowserFileSchemePolicy.isAllowed(js.toURI().toString(), listOf(root)))
            assertTrue(BrowserFileSchemePolicy.isAllowed(html.toURI().toString() + "?mode=demo#button", listOf(root)))
        } finally { root.deleteRecursively() }
    }

    @Test fun `reject private files traversal missing files and nonfile schemes`() {
        val parent = Files.createTempDirectory("browser-file-policy").toFile()
        try {
            val approved = File(parent, "webview").apply { mkdirs() }
            val index = File(approved, "index.html").apply { writeText("ok") }
            val secret = File(parent, "secret.db").apply { writeText("secret") }
            assertTrue(BrowserFileSchemePolicy.isAllowed(index.toURI().toString(), listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed(secret.toURI().toString(), listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed(
                "file://" + approved.absolutePath + "/../secret.db", listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed(
                File(approved, "missing.html").toURI().toString(), listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed("content://provider/item", listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed("file://otherhost/path", listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed("file:relative.html", listOf(approved)))
            assertFalse(BrowserFileSchemePolicy.isAllowed("file:///data/data/app/shared_prefs/token.xml", listOf(approved)))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `named skill pages allowed but credentials and unrelated files denied`() {
        val parent = Files.createTempDirectory("browser-file-policy").toFile()
        try {
            val skills = File(parent, "skills").apply { mkdirs() }
            val piano = File(skills, "virtual-piano").apply { mkdirs() }
            val html = File(piano, "ui.html").apply { writeText("<html></html>") }
            val css = File(piano, "assets/app.css").apply { parentFile!!.mkdirs(); writeText("body{}") }
            val secret = File(piano, ".env").apply { writeText("API_KEY=secret") }
            val markdown = File(piano, "SKILL.md").apply { writeText("skill") }
            val rootFile = File(skills, "root.html").apply { writeText("no") }
            assertTrue(BrowserFileSchemePolicy.isAllowed(html.toURI().toString(), emptyList(), skills))
            assertTrue(BrowserFileSchemePolicy.isAllowed(css.toURI().toString(), emptyList(), skills))
            assertFalse(BrowserFileSchemePolicy.isAllowed(secret.toURI().toString(), emptyList(), skills))
            assertFalse(BrowserFileSchemePolicy.isAllowed(markdown.toURI().toString(), emptyList(), skills))
            assertFalse(BrowserFileSchemePolicy.isAllowed(rootFile.toURI().toString(), emptyList(), skills))
            val hiddenDir = File(piano, ".private").apply { mkdirs() }
            val hiddenHtml = File(hiddenDir, "secrets.html").apply { writeText("not for browser") }
            assertFalse(BrowserFileSchemePolicy.isAllowed(hiddenHtml.toURI().toString(), emptyList(), skills))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `block remote to local or approved to private navigation`() {
        val parent = Files.createTempDirectory("browser-file-policy").toFile()
        try {
            val allowed = File(parent, "webview").apply { mkdirs() }
            val html = File(allowed, "index.html").apply { writeText("ok") }.toURI().toString()
            val other = File(allowed, "second.html").apply { writeText("ok") }.toURI().toString()
            val secret = File(parent, "secret.txt").apply { writeText("secret") }.toURI().toString()
            assertTrue(BrowserFileSchemePolicy.blockPageFileNavigation("https://example.com", html, listOf(allowed)))
            assertTrue(BrowserFileSchemePolicy.blockPageFileNavigation(html, secret, listOf(allowed)))
            assertFalse(BrowserFileSchemePolicy.blockPageFileNavigation(html, other, listOf(allowed)))
            assertFalse(BrowserFileSchemePolicy.blockPageFileNavigation(html, "https://example.com", listOf(allowed)))
            assertTrue(BrowserFileSchemePolicy.blockPageFileNavigation(secret, secret, listOf(allowed)))
            assertFalse(BrowserFileSchemePolicy.blockPageFileNavigation(secret, other, listOf(allowed)))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `symlink to outside root is denied when supported`() {
        val parent = Files.createTempDirectory("browser-file-policy").toFile()
        try {
            val approved = File(parent, "webview").apply { mkdirs() }
            val secret = File(parent, "secret.txt").apply { writeText("secret") }
            val link = File(approved, "link.txt")
            try { Files.createSymbolicLink(link.toPath(), secret.toPath()) }
            catch (_: UnsupportedOperationException) { return }
            catch (_: java.nio.file.FileSystemException) { return }
            assertFalse(BrowserFileSchemePolicy.isAllowed(link.toURI().toString(), listOf(approved)))
        } finally { parent.deleteRecursively() }
    }
}
