# Ticket t-record — Agent-generated HTML/JavaScript Android voice recording card

## For human fast reading:
The user requests a real end-to-end demonstrator: tell RikkaHub Agent to **generate an HTML/JavaScript card** and show it through the already working local `browser_open` FILE pathway. Clicking JS buttons must trigger Android **native** audio recording, save an actual file and play it. Central issue: prove the JavaScript ↔ Android WebView Bridge, not a browser-only microphone simulation.

## Required user-facing flow
1. User asks Agent: “Create and display an HTML/JavaScript recorder card.”
2. Agent writes generated `index.html` or equivalent to the already approved app-private `files/webview` directory and calls `browser_open(file://<app-files>/webview/<page>.html)`.
3. BrowserActivity displays visible HTML controls: Start recording, Stop/save, Play, Stop playback, plus status and file details.
4. **The chat transcript keeps a clickable object for the opened program.** After the Agent calls `browser_open`, the conversation renders a persistent card with the page title, the URL and an **Open in browser** action. Tapping it reopens that exact HTML/JavaScript program in BrowserActivity. The user must NOT have to go through Settings → Browser or retype a `file://` URL once the browser is closed or the app is restarted. This is required because the generated program is the deliverable the user wants to keep playing with, and `browser_open` alone (a tool-call row) is not actionable.
5. Start calls `RikkaRecorder.start()`; native Android asks for specific consent and RECORD_AUDIO runtime permission if needed; MediaRecorder starts recording.
6. Stop calls `RikkaRecorder.stop()`; Android persists valid AAC/m4a in `files/recordings`, and JS receives `recording_saved` with an opaque recording id, file name, size and path.
7. Play calls `RikkaRecorder.play(id)`; native MediaPlayer reads the previously saved file and plays audible sound; page receives playback event.
8. Denied permission, cancelled consent, wrong id, too-short sample, background and navigation must be handled safely.
9. Source implementation, JUnit policy tests, Mac build & device E2E evidence, strict diff review, synchronized DSU files, commit to `file-scheme`, **no GitHub Actions**.

## Scope clarification
This is a **visible BrowserActivity HTML page launched from Agent chat**, not a truly inline chat-history WebView; that would be separate functionality. Reuse the proven file scheme path. Existing Android `record_audio` agent tool performs timed capture but **does not implement interactive JS start/stop**; this ticket provides an Activity-owned native JS Bridge.

## Acceptance and safety
- Real generated page opens; JS can invoke `RikkaRecorder` bridge; events reach DOM.
- The generated program stays reachable from chat: a clickable card with an **Open in browser** action survives BrowserActivity being closed and app restart, and reopens the same file URL.
- Mic permission and explicit native recording consent work.
- Saved file nonempty, survives page navigation/activity recreation, can be played from id with an audible recording.
- Remote pages and arbitrary recording path IDs are rejected.
- No hidden/background capture without active page; app background/close stops recording.
- Native WebView `addJavascriptInterface` does not isolate iframes. Only trusted local HTML without untrusted scripts/iframes is acceptable; production isolation is future work.
- Unit tests and Android build/device verification must be recorded in [state](state.md) with accurate pass/fail/blocked status.

See [dev spec](../../dev-spec.md), [dev design](../../dev-design.md), [implementation](../../modules/m-browser/impl.md), [review](../../topics/review-t-record.md).

## Model discovery requirement
The actual `BrowserTools.browserOpenTool` description must tell the Agent about the Android-native `RikkaRecorder` JavaScript API, `rikka-recorder` events and the restriction to visible local files/webview HTML. Without this, a model receiving the user's natural-language request may generate standard browser getUserMedia code that never calls the native Bridge. Acceptance includes checking that the Agent's generated HTML invokes `RikkaRecorder.start/stop/play` rather than a mock or plain Web Audio API.
