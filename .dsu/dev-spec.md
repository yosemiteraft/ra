# Development functional specification — WebView file scheme

## For human fast reading:
An Agent can call `browser_open` with a full `file:///...` URL for an *existing approved local file* to display trusted HTML inside the app. Existing http(s)/about tool behavior remains. Navigation denial is **not** a security guarantee against hostile local JavaScript. [Ticket](tickets/t-file-scheme/t-file-scheme.md) defines acceptance; [design](dev-design.md) explains tradeoffs.

## Intended observable behavior
1. Given a present HTML file at `context.filesDir/webview/index.html`, `browser_open({"url":"file:///.../files/webview/index.html"})` opens it in the foreground or the existing headless browser path.
2. Existing files below `context.cacheDir/skill-webview` are also directly navigable.
3. Existing web asset files below `context.filesDir/skills/<skill-name>/` are navigable when the file extension is in the policy's web-asset allowlist. `SkillManager.getSkillsDir()` identifies the real skills root; JS skills may produce `ui.html` using `RunJsTool.resolveSkillWebviewUrl`.
4. Query and fragment, e.g. `file:///.../ui.html?mode=demo#panel`, remain in the URL, but file existence is checked on the URI's path.
5. Tool navigations to other existing files (preferences, database, `.env`, `SKILL.md`) return `{"error":"file_access_denied", ...}`; other schemes such as `content:`, `javascript:` and `intent:` return `scheme_not_allowed`.
6. The existing Browser tools (`browser_get_text`, `browser_eval_js`, click/scroll) should work after a permitted page loads. Foreground/headless rendering and JS/CSS/media resource behavior require device tests.
7. Page-initiated remote→file and file→unapproved-file **top-level navigations** should be rejected by the foreground/headless WebViewClients. This does not cover every URL / resource request.

## Error and compatibility boundaries
- Model `browser_open` retains the current scheme-less input behavior (not hardened here).
- UI SkillWebviewCard app-originated loading follows the existing direct path and is not subject to the model-tool's initial URL filter.
- File access is only possible for paths readable by Android application UID; this feature grants no OS permissions.
- The feature does **not** include unrestricted device storage access, a secure untrusted HTML viewer, or extra Android user permissions.

## Acceptance verification
The Mac agent must run Gradle unit tests, compile the debug app, install it on emulator/phone, demonstrate interactive local HTML (CSS/JS), test the same for headless if configured, then record actual outputs in [state.md](tickets/t-file-scheme/state.md). See [build-and-run](build-and-run.md). GitHub Actions prohibited.

## t-record: Agent-generated HTML/JavaScript recording card

The user asks the Agent to create a local HTML/JavaScript mini-app with four buttons: start recording, stop/save, play last recording and stop playback, then open it in the existing BrowserActivity using `browser_open`. This is a full-screen browser page launched from chat, **not** an inline chat WebView. The sample at `app/src/main/assets/demo/t-record.html` documents the required JavaScript contract; Agent can also generate equivalent files under `context.filesDir/webview/`.

Pressing Start invokes `RikkaRecorder.start()`, which presents a native confirmation followed by the Android microphone runtime permission prompt if not granted. After authorization, native MediaRecorder captures AAC in m4a format to `context.filesDir/recordings/rec_<timestamp>_<uuid>.m4a`. Stop invokes `RikkaRecorder.stop()` and the page receives `recording_saved` with a recording id and file metadata. Play invokes `RikkaRecorder.play(id)`, uses native MediaPlayer (not a `file://` load) and receives `playing` then `playback_finished`. All events are dispatched as `window` `rikka-recorder` CustomEvent detail objects.

After a successful `browser_open` the chat transcript keeps a clickable card (page title, URL and an **Open in browser** action) that reopens that exact page in BrowserActivity. The generated program therefore stays reachable after the browser is closed or the app is restarted, without going through Settings → Browser. The card is rendered by `BrowserOpenToolUI` (`app/src/main/java/me/rerere/rikkahub/ui/components/message/tools/BrowserToolUIs.kt`) from the persisted tool output, so it also appears for older messages in history. A denied permission, user cancellation, missing recording or very short recording reports an `error` event with a stable `code`. Backgrounding or leaving the page stops recording.

Only browser pages under the app-owned `files/webview` root with `.html`/`.htm` filenames may trigger the bridge; remote web pages should never get mic capability. This initial validation does not isolate untrusted iframes. Each start requires native consent; recordings are not exposed through direct `file://` browser_open. The feature must be built and exercised on an Android device before claiming completion.
