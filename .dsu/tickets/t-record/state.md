# t-record state — 2026-11-13

## For human fast reading:
Repository: `yosemiteraft/ra`, branch `file-scheme`. Ticket: [t-record](t-record.md). This implementation builds on the reported working file-scheme HTML demo. Source implementation, HTML example, tests, review and synchronized DSU documents have been committed to the `file-scheme` branch. **No Android Gradle build/device microphone test has been run in this execution**, so the end-to-end hardware requirement is NOT verified. No GitHub Actions.

## Implemented source
- `BrowserRecorderBridge.kt`: native JavaScript interface `RikkaRecorder`, explicit per-start native confirmation, Android RECORD_AUDIO runtime request, MediaRecorder m4a file write, MediaPlayer playback, CustomEvent callback and cleanup on stop/navigation/background.
- `BrowserRecorderPagePolicy.kt`: only app-staged `files/webview` HTML may initiate API calls; recording id input format validated.
- `BrowserActivity.kt`: register bridge before navigation, mic permission launcher, destroy/stop cleanup.
- `BrowserView.kt`: onWebViewReady callback executes before initial loadUrl.
- `BrowserRecorderPagePolicyTest.kt`: tests for trusted local HTML vs other URLs, symlink escape, recording id format.
- `app/src/main/assets/demo/t-record.html`: reproducible JavaScript UI and events. Agent-generated equivalent page must be staged in `files/webview` and opened via `browser_open`.
- `AndroidManifest.xml` already declares RECORD_AUDIO; no manifest change required.
- Previous `record_audio` agent timed recorder tool is unchanged.

## Current verification status
- GitHub source review and file/DSU link consistency checks are possible via connector.
- No Android SDK/ADB or emulator available here; `./gradlew :app:assembleDebug`, JUnit Gradle tests, and microphone recording/playback have **not** been executed.
- Unverified: user-generated page file write through model tools on this branch and true end-to-end app behavior; Mac agent must confirm.
- Security limitation: addJavascriptInterface methods can be called from frames when top-level page is trusted; explicit native consent mitigates, but does not provide untrusted iframe isolation. Existing file-origin JS settings also need a separate hardening effort.

## Mac continuation — do not stop before verification
1. Read root `AGENTS.md`, [index](../../index.md), [ticket](t-record.md), [design](../../dev-design.md), [review](../../topics/review-t-record.md).
2. Run `git diff origin/master...file-scheme`, `git diff --check`, JUnit policy tests and `./gradlew :app:assembleDebug`. Inspect/fix compilation and test failures. **No GitHub Actions**.
3. Install on Android, stage `app/src/main/assets/demo/t-record.html` to private `files/webview/t-record.html`, open via Agent `browser_open` and test native start/stop/play; verify file size and audibility.
4. Repeat using the Agent's own generated HTML source; ensure actual model file-write tool path and file permissions work. Capture exact tool calls/returns.
5. Test denied permission, cancellation, navigation, background, bad ID, remote URL and local iframe attempt. Decide whether stronger origin-bound WebMessageListener isolation is necessary for untrusted generated apps.
6. Record evidence (build output/test report/recording length/logcat), update this state and any affected DSU spec/design/impl/index/review docs, commit and push to `file-scheme`.

## Source revision
The ticket source and synchronized docs were committed on `file-scheme` starting at feature commit `fa2afa5474890fb2498fe133e3ac32bc537430ca`, with subsequent reviewed fix commits. The latest complete reviewed source, model-facing tool contract and lifecycle/media fixes are at `22463f7ed8202758e757e4593b7924fc97cac177`. Always fetch the branch HEAD for the latest version; do not assume this state includes later hardware verification.

## Additional source review and regression fix
The first feature commit has been reviewed for BrowserController ordering. Binding before `WebView.loadUrl` was identified as a browser_open race. A two-stage callback now registers the JS interface before loading but binds the controller after `loadUrl(initialUrl)`; see [review](../../topics/review-t-record.md) and [module implementation](../../modules/m-browser/impl.md). Android build and actual microphone tests remain pending and must not be marked passed.

## Follow-up security review
After the first feature and browser_open race-fix commits, review identified pending-permission navigation risk. `BrowserRecorderBridge.pendingPageUrl` now ties native confirmation/permission completion to the original local page. `onNavigation` revokes it on top-level page change. Still requires Mac device tests: leave/replace page while microphone permission dialog is pending, and verify no recording begins. All listed changes are source-only until hardware tests run.

## Privacy / final code hardening
Android Auto Backup and device-transfer rules now explicitly exclude `files/recordings` (even though current rules are include-only for uploads). Audio files remain in app-private `files/recordings`; users can play via the bridge without exposing the directory to `browser_open`. Native consent state is reset on cancellation, and playback file canonicalization errors return a controlled error. Confirm files persist across app restart but are excluded from device backup/transfer. Not yet verified on device.

## Verification matrix (review cutoff: 2026-11-13)

| Acceptance item | Evidence | State |
|---|---|---|
| Native Bridge registered before first HTML page load | Reviewed `BrowserActivity.onWebViewReady` → `loadUrl` → `onInitialLoadStarted` ordering | SOURCE REVIEWED, not device-tested |
| Android RECORD_AUDIO permission declared | Manifest inspection | SOURCE REVIEWED |
| Local HTML/ID trust policy | Standalone `kotlinc` policy mirror, 11/11 assertions passed | HOST MIRROR PASS, JUnit not run |
| Four-button demo JS callback flow | Node.js syntax + mock DOM/Bridge flow passed | HOST MOCK PASS, not WebView |
| Android native consent and MediaRecorder actual recording | Emulator `ra_record_test` (API 34), consent+permission dialogs, m4a 204,419 B (15.2 s) and 230,845 B (17.4 s) | PASS on emulator (software mic); ear-check not done |
| Persistent m4a file and audible playback via MediaPlayer | `dumpsys audio` MediaPlayer `state:started` (uid 10192, 44100 Hz mono), page `playback_finished`; m4a mean -35.7 dB / peak -15.6 dB | PASS on emulator (routing verified, not heard) |
| Agent generation → file tool → browser_open → HTML button → microphone | DeepSeek Chat in app: `write_text_file` → `browser_open` → Start/Stop/Play on `agent-record.html` | PASS on emulator |
| Remote/iframe security isolation | `addJavascriptInterface` exposes all frames | KNOWN OPEN SECURITY LIMITATION |
| File-scheme regression | Local `file://` page loaded from address bar and from chat card on emulator | PASS on emulator |
| Backup rules exclude audio | Reviewed legacy and API 31+ XML | SOURCE REVIEWED |

## Final review and source checkpoint
Current reviewed source commit before this status documentation update: `1036f2ece2dbe017215e3a865e00fad23e17c1a6`. The GitHub branch may have a newer documentation-only HEAD; always fetch `file-scheme`. The source demonstrates the Bridge implementation but no honest assertion of Android end-to-end success is possible without Android SDK and microphone hardware. The Mac agent should complete the precise hardware test steps in [build-and-run](../../build-and-run.md), update all relevant DSU docs and this state before marking the ticket DONE.

## Model-to-Bridge discovery checkpoint
Source review found a missing step: the model must be told that the Android Bridge exists. Updated the live `BrowserTools.browserOpenTool` tool description to advertise `RikkaRecorder.start()`, `stop()`, `play(id)`, `stopPlayback()`, structured `rikka-recorder` events, native permission/consent, foreground-only availability and the approved `files/webview/*.html` origin. The Agent-generated card remains an Android acceptance test; a tool-description change alone does not prove the model used it correctly.

## Final GitHub checkout audit and release status
At source review head `d1be17db6d5d4767ece832c7ad9307f5418a8419`, GitHub connector inspection confirmed all four `@JavascriptInterface` methods, user confirmation, Android RECORD_AUDIO request, MediaRecorder/MediaPlayer calls, JSON CustomEvent callback, bridge initialization before initial page load, BrowserController bound after `loadUrl`, Agent `browser_open` tool discovery contract, and exclusion of private audio from legacy + current Android backups. The current feature is **IMPLEMENTED AND COMMITTED, BUT HARDWARE E2E UNVERIFIED**. Only Mac/Android verification may change this to DONE. No GitHub Actions were run.

## Final review: playback-page ownership
The latest source correction tracks the exact `playbackPageUrl` for each playback session; navigating to another page, even if both pages are approved local HTML, stops MediaPlayer. `activePageUrl` already provides the corresponding recording guard. Mac verification must include: start playback, navigate from `t-record.html` to a second approved local HTML page, and confirm audio stops; also test navigation to a remote URL. These behaviors have not yet been validated on a device.

## Final MediaPlayer native resource review
Source reviewed and patched: `play(id)` stores the MediaPlayer instance before `setDataSource`/`prepare` so a failed initialization can be released. Playback completion is ignored if the callback does not belong to the current player, preventing cross-session interference. Mac device tests should include invalid/corrupt m4a playback and rapid stop/play cycles, in addition to successful audible playback. No Android audio runtime results yet.

## Final GitHub diff and DSU consistency audit
- Ticket starting branch HEAD: `2e1d1bcb1be5f86070e49f1c5dc461e14012848e`.
- Reviewed source commit (before this documentation checkpoint): `22463f7ed8202758e757e4593b7924fc97cac177`.
- GitHub Compare reported **9 ticket commits** and **22 ticket-specific modified/added files**, with only the intended `file-scheme` branch updated. Check the latest branch HEAD for the documentation checkpoint.
- Fetched live repository tree and read **16 DSU/root Markdown documents**. Parsed all repository-relative Markdown links against the live tree: **0 broken links**. Trailing Markdown whitespace: **0 occurrences**.
- Source inspection checked four `@JavascriptInterface` annotated methods, native consent and runtime RECORD_AUDIO request, MediaRecorder saved file, guarded native MediaPlayer playback, ordered bridge registration before initial page load, BrowserController binding after load begins, model-visible browser_open contract and both Android backup exclusions.
- Independent host mirror policy checks **11/11 pass**; Node.js demo JavaScript syntax and mock DOM/Bridge event flow **pass**. These are explicitly **not equivalent** to Android Gradle/JUnit or device microphone verification.
- Remaining concrete external blocker: this execution environment lacks Android SDK/ADB/emulator/physical microphone. Mac agent must run `./gradlew :app:testDebugUnitTest --tests '*BrowserRecorderPagePolicyTest'`, `./gradlew :app:assembleDebug`, Android native consent/capture/save/playback and model-generated HTML card E2E before setting status **DONE**. Current status is **IMPLEMENTED / COMMITTED / DEVICE VALIDATION PENDING**. No GitHub Actions.


## Mac emulator E2E — 2026-11-13 (separate AVD, keyboard fix)

Environment: new AVD `ra_record_test` (android-34 google_apis arm64, pixel_6) on **emulator-5556**. `sapien_phone_emulator` (emulator-5554) belongs to another agent and was not touched. The AVD was created with `hw.keyboard=no`, so the Mac keyboard could not type into it; fixed by setting `hw.keyboard=yes` in `~/.android/avd/ra_record_test.avd/config.ini`, cold-restarting, and `settings put secure show_ime_with_hard_keyboard 1`. `dumpsys input` now shows `qwerty2 ... built-in keyboard`.

Build on this Mac: `:app:testDebugUnitTest --tests '*BrowserRecorderPagePolicyTest'` = 3 tests, 0 failures; `:app:assembleDebug` = BUILD SUCCESSFUL before and after the chat-card change.

Model path for the E2E (test environment only, not a product change): the debug app was given a DeepSeek provider (`deepseek-chat`, key from the configured `DEEPSEEK_API_KEY` credential) by patching `files/datastore/settings.preferences_pb` (backup kept at `/tmp/settings.backup.pb`) and enabling `browser`, `files`, `download` local tools for the Default Assistant. `write_text_file` requires one tap on Approve; this is the app's normal tool-approval flow.

Actual flow observed:
1. Chat prompt asked for a recorder card at `/data/user/0/excp.rikkahub.debug/files/webview/agent-record.html`; the model called `write_text_file` (approval once) and wrote **9,292 bytes** of its own HTML/JS. The generated page looks the bridge up lazily (`window.RikkaRecorder`) and does not use `getUserMedia`.
2. The model called `browser_open` with `file:///data/user/0/excp.rikkahub.debug/files/webview/agent-record.html`; BrowserActivity displayed the four buttons, status "Ready".
3. Start → native "Record audio" consent → recording; Stop → "Saved · 225.4 KB", id `rec_1794611770688_3152222dbecb452ca413e9aaa328c390`, file 230,845 bytes. `ffprobe`: AAC, 44.1 kHz, mono, 17.41 s. `ffmpeg volumedetect`: mean -35.7 dB, max -15.6 dB (not silent).
4. Play → `dumpsys audio` shows `MediaPlayer ... state:started` for uid 10192 with sampleRate 44100/mono on the emulator output device, then the page shows "Playback finished".
5. Negative case: Cancel on the native consent dialog → page shows `Error: user_cancelled`. (The generated page keeps the stale `code:` line after later events — a cosmetic bug in the model's HTML, not the bridge.)

Earlier session on this emulator also saved a 204,419 B / 15.2 s recording from the bundled `t-record.html` (mean -36.4 dB, max -21.1 dB) — still present in `files/recordings/`.

## Chat-reachability fix (acceptance failure reported by user)

`browser_open` alone left only a tool-call row, so once BrowserActivity closed the generated program could not be reopened from chat — the user had to go to Settings → Browser and retype the URL. This is now a ticket requirement (flow item 4, acceptance bullet 2) and is implemented:
- `app/src/main/java/me/rerere/rikkahub/ui/components/message/tools/BrowserToolUIs.kt` — `BrowserOpenToolUI` renders a card (page title, URL, **Open in browser**) in the tool step's always-visible Summary slot. It reads the persisted tool output (`current_url`, `title`) with `input.url` fallback, so history messages get the card too.
- Registered in `ToolUIRegistry` (`ToolUI.kt`), strings in `res/values/strings.xml`.
- Verified on emulator-5556: the card appeared in the existing conversation under the `browser_open` step; tapping **Open in browser** launched BrowserActivity at `agent-record.html`, and the recorder worked from that reopened page. No JUnit test covers this UI yet.

Not yet done: navigation/background/remote-URL/invalid-id cases on this emulator, playback audibility by human ear, and the JUnit card test. Source and docs for this session are committed on `file-scheme`.
