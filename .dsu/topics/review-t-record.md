# t-record: source and bridge implementation review

## Scope
Reviewed `BrowserActivity`, `BrowserView`, `BrowserFileSchemePolicy`, RECORD_AUDIO manifest permission, existing `MicRecorderTool.kt`, and new recorder Bridge, HTML demo and page-origin tests. The original `record_audio` tool records a fixed duration and does not implement click-to-start/stop JS interactivity.

## Security and design checklist
1. HTML entry path: existing `browser_open` file policy limits local file navigation. Recorder Bridge has a **separate** trusted local HTML entry-point check; `recordings` is not added to the browser file allowlist.
2. Bridge registration must occur before the first `WebView.loadUrl` or the initial page might not see `window.RikkaRecorder`. BrowserView now calls `onWebViewReady(this)` first.
3. Native confirmation on every `RikkaRecorder.start`, then RECORD_AUDIO permission via ActivityResultContracts; no silent recording.
4. `@JavascriptInterface` callback is not on Android UI thread. All native recorder/player actions are marshalled with `runOnUiThread`. Audio results are JSON encoded and returned via `evaluateJavascript(new CustomEvent(...))`.
5. No arbitrary `play(filePath)`; only well-formed generated IDs under `files/recordings`. AAC/m4a saved privately.
6. Activity background/destroy/navigate-away stops recording; MediaRecorder and MediaPlayer are released; maximum capture duration 300 seconds.
7. Demonstration HTML includes the four required buttons and handles `recording_started`, `recording_saved`, `playing`, `playback_finished`, `playback_stopped` and `error`.
8. **HIGH outstanding security limitation:** `addJavascriptInterface` is exposed to every frame. The top-level trusted URL check does not authenticate an iframe origin or prevent untrusted local script code from invoking the mic consent dialog. Use only trusted pages for demo. An origin and main-frame authenticated bridge should be designed before supporting arbitrary untrusted HTML.
9. **Unverified:** full Android Gradle compile/test, actual device mic permission, recording, playback, and state callback; cannot claim success until Mac agent provides evidence.

## Review disposition
Feature implementation and sample UI prepared; actual hardware E2E acceptance pending Android SDK/device. Preserve existing `file-scheme` feature. User expects full Mac verification, not merely source code delivery; see [state](../tickets/t-record/state.md) and [runbook](../build-and-run.md).

## Post-commit regression review
The initial patch moved `onWebViewReady` ahead of `loadUrl(initialUrl)` to make the JS Bridge visible on the first page, but that also caused `BrowserController.bind` to signal readiness **before** navigation started. The browser_open fresh-Activity path could then observe about:blank and return too early. Fixed by adding `onInitialLoadStarted(WebView)` callback: the Bridge is registered in `onWebViewReady`, then `loadUrl` is called, and only then `BrowserController.bind` signals the browser controller. This preserves the original browser_open ordering while making the interface available to the initial page.

## Post-review: pending native consent must remain bound to the same page
The additional source review found a race: permission request could outlive navigation from one approved HTML card to another. The bridge now captures `pendingPageUrl` at `start()`, cancels native consent when the top-level URL changes, and checks URL equality again when permission result arrives. This avoids starting a microphone session for a different page after the user authorized an earlier one. The start prompt remains native and explicit. This is a code-path review result, **not** an Android runtime verification.

## Privacy and robustness follow-up
Both Android backup configurations were reviewed. Existing backup uses an include-only list limited to `files/upload`, but `files/recordings` is now explicitly excluded in `res/xml/backup_rules.xml` and both cloud/device-transfer sections of `data_extraction_rules.xml` as defense-in-depth. The JavaScript consent cancellation also clears `pendingPageUrl`, and playback file canonicalization failures now return `recording_not_found` instead of leaking an unhandled exception. Source review only; device validation pending.

## Independent, non-Android verification evidence
- Manually mirrored the current `BrowserFileSchemePolicy` and `BrowserRecorderPagePolicy` source into a local Kotlin/JVM harness, compiled with installed `kotlinc`, ran 11 deterministic checks; **all 11 passed** (trusted HTML, query/fragment, reject JS, unrelated files, https, relative file, null, good ID, traversal ID, malformed ID, and top-level disallowed navigation). The mirror was not an actual GitHub checkout or the repository's JUnit Gradle task.
- Manually copied the script block from the committed `t-record.html` into a Node.js syntax-check and mocked DOM/Bridge event harness. `node --check` passed and the mock start→recording_started→stop→recording_saved→play→playing→stopPlayback→playback_finished sequence passed. **No microphone/audio hardware was involved.**
- GitHub Compare against `2e1d1bcb1be5f86070e49f1c5dc461e14012848e` identified 21 ticket-specific changed files at source commit `1036f2ece2dbe017215e3a865e00fad23e17c1a6` (including six source/test/demo files, backup XML and DSU documents). No GitHub Actions used.
- Android SDK/adb not installed in this runtime; Gradle and Android device tests remain unexecuted. Neither the local JVM policy mirror nor Node.js mock proves `@JavascriptInterface`, permission dialogs, `MediaRecorder`, or `MediaPlayer` work on-device.

## Model-generated card gap and fix
An initial review only implemented the runtime Android Bridge. The model could still choose `navigator.mediaDevices.getUserMedia` because it had no visible description of `RikkaRecorder`. Fixed by updating the real `browser_open` tool description with methods, event schema, foreground-only usage and user authorization. Actual Agent-generation test remains pending Android app execution. Do not claim correct generated code solely from adding the description.

## Static integration checklist (source at model-discovery commit)
- [x] `RikkaRecorder.start`, `stop`, `play`, `stopPlayback` methods have `@JavascriptInterface`.
- [x] `BrowserActivity` registers `RikkaRecorder`; visible BrowserView loads initial HTML after registration, then signals BrowserController.
- [x] Microphone consent and RECORD_AUDIO permission in code; MediaRecorder and MediaPlayer native calls present.
- [x] Recording files use app-private files/recordings and opaque checked IDs; backup excludes recordings in both XML configurations.
- [x] Tool description advertises native JS API for model-generated HTML; WebView sample displays four buttons.
- [x] Standalone Kotlin path-policy mirror 11/11 and Node.js mock JS event sequence pass, with limitations noted above.
- [ ] Full Android build and repository JUnit Gradle task (no SDK here).
- [ ] Real Android UI confirmation, mic capture, nonempty m4a, audible playback, webview event path and Agent autonomous generation.
- [ ] Security boundary on untrusted iframe HTML (not claimed safe).

## Playback lifecycle correction
A final pass identified that playback originally stopped only when navigating to a non-trusted URL. Navigation to a different *approved* `files/webview/*.html` page could leave audio playing. Fixed by capturing `playbackPageUrl` at `RikkaRecorder.play(id)` and releasing MediaPlayer on **any** top-level navigation away from that exact page. Recorder already tracked `activePageUrl` for the corresponding microphone case. This change still requires Android navigation tests.

## MediaPlayer exception-path and stale callback correction
Final resource audit found an actual leak possibility: constructing `MediaPlayer().apply { setDataSource; prepare; start }` before assigning the resulting object to the `player` field meant an exception during `setDataSource` or `prepare` could leave an untracked native MediaPlayer. The code now creates the player in the try block and stores the instance **before** initialization, ensuring the catch block releases it. The completion listener checks `player === completed` to avoid a stale previous playback callback stopping a newer session. Android hardware failure cases still need device testing.
