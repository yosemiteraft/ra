package me.rerere.rikkahub.browser

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * t-record. Activity-scoped native recorder for an Agent-generated local HTML card.
 *
 * API for trusted local HTML:
 *   RikkaRecorder.start()
 *   RikkaRecorder.stop()
 *   RikkaRecorder.play(recordingId)
 *   RikkaRecorder.stopPlayback()
 *   window.addEventListener("rikka-recorder", e => console.log(e.detail))
 *
 * @JavascriptInterface is callable from iframes: never rely on the caller frame
 * being trusted. We verify the CURRENT top-level URL, require native user consent
 * on every start, and keep the resulting audio in app-private files/recordings.
 * No arbitrary path or Android permission is accepted from JavaScript.
 */
internal class BrowserRecorderBridge(
    private val activity: BrowserActivity,
    private val webView: WebView,
) {
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingId: String? = null
    private var player: MediaPlayer? = null
    private var disposed = false
    private var consentPending = false
    private var pendingPageUrl: String? = null
    private var consentDialog: AlertDialog? = null
    private var activePageUrl: String? = null
    private var playbackPageUrl: String? = null
    private val maxDurationMs = 300_000L

    private val recordDir: File get() = File(activity.filesDir, "recordings")
    private val cardsDir: File get() = File(activity.filesDir, "webview")

    @JavascriptInterface
    fun start() = onMain {
        if (!checkCard()) return@onMain
        if (recorder != null || consentPending) {
            emit("error", "already_recording_or_pending")
            return@onMain
        }
        consentPending = true
        pendingPageUrl = webView.url
        consentDialog = AlertDialog.Builder(activity)
            .setTitle("Record audio")
            .setMessage("The local recorder card requests microphone access. Only allow a card you trust. Start recording?")
            .setPositiveButton("Start") { _, _ ->
                if (!checkCard() || webView.url != pendingPageUrl) {
                    consentPending = false
                    pendingPageUrl = null
                    return@setPositiveButton
                }
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    consentPending = false
                    pendingPageUrl = null
                    startRecording()
                } else {
                    val requested = activity.requestRecorderPermission { granted ->
                        val samePage = consentPending && webView.url == pendingPageUrl
                        consentPending = false
                        pendingPageUrl = null
                        if (granted && samePage && checkCard()) startRecording()
                        else if (samePage) emit("error", "microphone_permission_denied")
                        // Old page was cancelled/reloaded while Android owned the dialog:
                        // never send its result to a different document.
                    }
                    if (!requested) {
                        consentPending = false
                        pendingPageUrl = null
                        emit("error", "microphone_permission_unavailable")
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                consentPending = false
                pendingPageUrl = null
                emit("error", "user_cancelled")
            }
            .setOnCancelListener {
                consentPending = false
                pendingPageUrl = null
                emit("error", "user_cancelled")
            }
            .show()
    }

    @JavascriptInterface
    fun stop() = onMain {
        if (!checkCard()) return@onMain
        finishRecording(notify = true)
    }

    @JavascriptInterface
    fun play(id: String) = onMain {
        if (!checkCard()) return@onMain
        if (recorder != null) {
            emit("error", "recording_in_progress")
            return@onMain
        }
        if (!BrowserRecorderPagePolicy.isSafeRecordingId(id)) {
            emit("error", "invalid_recording_id")
            return@onMain
        }
        val file = File(recordDir, "$id.m4a")
        val approvedFile = runCatching {
            file.isFile && file.canonicalFile.parentFile == recordDir.canonicalFile
        }.getOrDefault(false)
        if (!approvedFile) {
            emit("error", "recording_not_found")
            return@onMain
        }
        stopPlayer()
        playbackPageUrl = webView.url
        try {
            val playback = MediaPlayer()
            player = playback // Own/release even when setDataSource or prepare throws.
            playback.setDataSource(file.absolutePath)
            playback.setOnCompletionListener { completed ->
                // Ignore a stale completion from a previous playback session.
                if (player === completed) {
                    stopPlayer()
                    emit("playback_finished", id = id)
                }
            }
            playback.prepare()
            playback.start()
            emit("playing", id = id)
        } catch (e: Exception) {
            stopPlayer()
            emit("error", "playback_failed")
        }
    }

    @JavascriptInterface
    fun stopPlayback() = onMain {
        if (!checkCard()) return@onMain
        stopPlayer()
        emit("playback_stopped")
    }

    private fun startRecording() {
        if (!checkCard() || recorder != null || disposed) return
        stopPlayer() // Prevent playback feedback into the microphone.
        val id = "rec_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "")}"
        val file = File(recordDir, "$id.m4a")
        try {
            check(recordDir.isDirectory || recordDir.mkdirs()) { "Cannot create recordings directory" }
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(activity)
            } else {
                MediaRecorder()
            }
            recorder = r
            activePageUrl = webView.url
            recordingFile = file
            recordingId = id
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.setMaxDuration(maxDurationMs.toInt())
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    activity.runOnUiThread { if (recorder === r) finishRecording(notify = true) }
                }
            }
            r.prepare()
            r.start()
            emit("recording_started", id = id)
        } catch (e: Exception) {
            recorder?.let { r ->
                runCatching { r.reset() }
                runCatching { r.release() }
            }
            recorder = null
            activePageUrl = null
            recordingFile = null
            recordingId = null
            file.delete()
            emit("error", "recording_start_failed")
        }
    }

    private fun finishRecording(notify: Boolean) {
        val r = recorder ?: run {
            if (notify) emit("error", "not_recording")
            return
        }
        val file = recordingFile
        val id = recordingId
        recorder = null
        activePageUrl = null
        recordingFile = null
        recordingId = null
        val saved = try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            false
        } finally {
            runCatching { r.reset() }
            runCatching { r.release() }
        }
        // A navigation/background abort must never persist an orphaned, unannounced recording.
        if (!notify) {
            file?.delete()
            return
        }
        if (!saved || file == null || !file.isFile || file.length() == 0L) {
            file?.delete()
            if (notify) emit("error", "recording_too_short_or_failed")
        } else {
            if (notify) emit("recording_saved", id = id, file = file)
        }
    }

    private fun stopPlayer() {
        val current = player
        player = null
        playbackPageUrl = null
        if (current != null) {
            runCatching { if (current.isPlaying) current.stop() }
            runCatching { current.release() }
        }
    }

    /**
     * WebViewClient.onPageStarted is called for ALL main-frame loads, including reloads
     * of the same URL. Invalidate the old JS execution context unconditionally:
     * checking URL equality alone leaves a live microphone after page reload.
     */
    fun onTopLevelPageLoadStarted(@Suppress("UNUSED_PARAMETER") url: String) {
        consentDialog?.dismiss()
        consentDialog = null
        consentPending = false
        pendingPageUrl = null
        finishRecording(notify = false)
        stopPlayer()
    }

    fun onBackground() {
        consentDialog?.dismiss()
        consentDialog = null
        consentPending = false
        pendingPageUrl = null
        finishRecording(notify = false)
        stopPlayer()
    }

    fun release() {
        disposed = true
        onBackground()
    }

    private fun checkCard(): Boolean {
        if (!disposed && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            isTrustedCard(webView.url)) return true
        emit("error", "untrusted_page")
        return false
    }

    private fun isTrustedCard(url: String?): Boolean =
        BrowserRecorderPagePolicy.isTrusted(url, cardsDir)

    private fun emit(type: String, error: String? = null, id: String? = null, file: File? = null) {
        if (disposed || !isTrustedCard(webView.url)) return
        val json = JSONObject().apply {
            put("type", type)
            error?.let { put("code", it) }
            id?.let { put("id", it) }
            file?.let {
                put("fileName", it.name)
                put("absolutePath", it.absolutePath)
                put("bytes", it.length())
            }
        }
        // JSON generated by JSONObject, never concatenated with untrusted JS text.
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('rikka-recorder', {detail: ${json}}));",
            null
        )
    }

    private fun onMain(action: () -> Unit) {
        activity.runOnUiThread {
            if (!disposed) action()
        }
    }
}
