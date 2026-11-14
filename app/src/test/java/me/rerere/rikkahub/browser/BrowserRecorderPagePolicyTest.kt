package me.rerere.rikkahub.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BrowserRecorderPagePolicyTest {
    @Test fun `approved card and URL query are accepted, untrusted URLs rejected`() {
        val parent = Files.createTempDirectory("record-page").toFile()
        try {
            val cards = File(parent, "webview").apply { mkdirs() }
            val html = File(cards, "record.html").apply { writeText("ok") }
            val js = File(cards, "record.js").apply { writeText("ok") }
            val outside = File(parent, "secret.html").apply { writeText("secret") }
            assertTrue(BrowserRecorderPagePolicy.isTrusted(html.toURI().toString(), cards))
            assertTrue(BrowserRecorderPagePolicy.isTrusted(html.toURI().toString() + "?q=1#rec", cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted(js.toURI().toString(), cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted(outside.toURI().toString(), cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted("https://example.com", cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted("file:relative.html", cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted(null, cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted("file:///data/local/tmp/nope.html", cards))
            assertFalse(BrowserRecorderPagePolicy.isTrusted("file://evil.example/tmp/x.html", cards))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `symlink escaping staged folder is rejected`() {
        val parent = Files.createTempDirectory("record-symlink").toFile()
        try {
            val cards = File(parent, "webview").apply { mkdirs() }
            val secret = File(parent, "secret.html").apply { writeText("secret") }
            val link = File(cards, "linked.html")
            try { Files.createSymbolicLink(link.toPath(), secret.toPath()) }
            catch (_: UnsupportedOperationException) { return }
            catch (_: java.nio.file.FileSystemException) { return }
            assertFalse(BrowserRecorderPagePolicy.isTrusted(link.toURI().toString(), cards))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `recording id cannot become arbitrary file path`() {
        assertTrue(BrowserRecorderPagePolicy.isSafeRecordingId("rec_1794628800000_" + "a".repeat(32)))
        assertFalse(BrowserRecorderPagePolicy.isSafeRecordingId("../../shared_prefs/token"))
        assertFalse(BrowserRecorderPagePolicy.isSafeRecordingId("rec_1_abc.m4a"))
        assertFalse(BrowserRecorderPagePolicy.isSafeRecordingId(""))
        assertFalse(BrowserRecorderPagePolicy.isSafeRecordingId("rec_1794628800000_" + "G".repeat(32)))
        assertFalse(BrowserRecorderPagePolicy.isSafeRecordingId("rec_1794628800000_" + "a".repeat(32) + "../"))
    }
}
