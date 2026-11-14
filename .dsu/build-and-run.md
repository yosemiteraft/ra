# Build and run — Mac handoff / t-file-scheme

## For human fast reading:
**No GitHub Actions.** Run Gradle on Mac with Android SDK 37 and JDK 17+; use real Android emulator or ADB device. These commands reproduce the Mac build and emulator checks. See actual results in [ticket state](tickets/t-file-scheme/state.md).

## 1. Checkout and diff review
```bash
git clone https://github.com/yosemiteraft/ra.git
cd ra
git fetch origin
git switch -c file-scheme --track origin/file-scheme
git status --short
git submodule update --init --recursive
git diff --stat origin/master...HEAD
git diff --check origin/master...HEAD
git diff origin/master...HEAD -- app/src/main app/src/test .dsu
```
If already cloned, run `git fetch origin` then `git switch file-scheme` (or create the tracking branch if absent). Ensure working tree is clean before edits. Do not overwrite local Mac work.

## 2. Mac prerequisites and build
```bash
/usr/libexec/java_home -V
java -version
xcode-select -p || true
echo "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-unset}}"
ls -l ./gradlew
chmod +x ./gradlew
./gradlew --version
./gradlew :app:testDebugUnitTest --tests '*BrowserFileSchemePolicyTest'
./gradlew :app:assembleDebug
find app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print
```
The Android app compiles with `compileSdk = 37`, Java source/target 17, and ABI splits (`app/build.gradle.kts`). Install Android SDK Platform 37 and project-required tools through Android Studio if missing. Use the **actual** debug APK (often split/universal named), not an assumed `app-debug.apk`.

## 3. Install and stage controlled local HTML
```bash
adb devices
adb install -r PATH_TO_ACTUAL_DEBUG_APK
adb shell run-as excp.rikkahub.debug mkdir -p files/webview
printf '%s' '<!doctype html><meta charset="utf-8"><title>File Scheme Smoke</title><link rel="stylesheet" href="style.css"><button id="go" onclick="document.body.dataset.clicked=1">Go</button><script src="app.js"></script>' | adb shell "run-as excp.rikkahub.debug sh -c 'cat > files/webview/index.html'"
printf '%s' 'document.body.dataset.loaded="yes";' | adb shell "run-as excp.rikkahub.debug sh -c 'cat > files/webview/app.js'"
printf '%s' 'button{font-size:24px}' | adb shell "run-as excp.rikkahub.debug sh -c 'cat > files/webview/style.css'"
adb shell run-as excp.rikkahub.debug pwd
```
The debug package is `excp.rikkahub.debug`; exact absolute private data path comes from the `pwd` output. If `run-as` is rejected, verify this really is a debuggable build and the installed package matches the chosen APK.

## 4. End-to-end smoke scenarios
1. In RikkaHub Settings → Browser enable the needed browser tools. Ask Agent to call `browser_open({"url":"file://<PWD>/files/webview/index.html?mode=smoke#go"})` with the actual absolute `PWD` from the device. Verify page content, CSS and JS load.
2. Call `browser_eval_js` with `document.body.dataset.loaded` (expected `yes`), `browser_click` with selector `#go`, then `browser_eval_js` with `document.body.dataset.clicked` (expected `1`). Check `browser_get_text` and screenshot where applicable.
3. Open a real installed JS skill page `files/skills/<skill-name>/ui.html`; check sibling assets. Confirm `browser_open` rejects that skill's `SKILL.md` and hidden `.env`, even if file exists.
4. Confirm `https://example.com` and `about:blank` still work. `content:`/`javascript:`/`intent:` should yield `scheme_not_allowed`.
5. Confirm `file://<PWD>/shared_prefs/...xml` returns `file_access_denied`. Test path traversal and symlink edges via JUnit.
6. From remote web page, attempt top-level navigation into `file://`; from permitted local page, attempt top-level navigation to a private file. Repeat in headless mode when Telegram/cron prerequisites are configured.
7. **Security audit:** run a disposable local HTML attempting to fetch a private data file. Existing `allowFileAccessFromFileURLs=true` makes this an outstanding security concern, not a promised denial. Do not expose real secrets during testing; use dummy values.

### Open an interactive card from chat

1. Enable **JS Skills** in the test assistant's local tools and select a model that supports tool calls.
2. Send: “Show me a playable piano card. Call `run_js` with `skill_name: virtual-piano` and `script: scripts/index.html`. Leave the card in chat so I can open it.”
3. Approve that invocation. Tap **Open in browser** on the returned card, then tap a key. The bundled skill is an alternative to staging the model-generated Pocket Counter page.
4. For the Pocket Counter run on this Mac, use the exact prompts and generated page in `.working/file-scheme/`. The [ticket state](tickets/t-file-scheme/state.md) describes how the demo staged the page and what the device showed.

## 5. Record evidence before marking done
Save commands, exit codes, Gradle test report and meaningful redacted `adb logcat` output in ticket state; link any tests added. Fix problems, rerun, then `git diff --check`, `git status`, commit on `file-scheme`, push to GitHub. Never run GitHub Actions.

## 6. Known limitations / triage
- `shouldOverrideUrlLoading` does not authorize every WebView subresource, redirect or JavaScript file request; a passing navigation test is not a sandbox proof.
- If existing JS skill media fails with hardening switches, preserve evidence and propose a separate HTTPS asset loader/isolation plan.
- If background WebView screenshot is blank, check `HeadlessBrowserSession` offscreen render path (software layer) separately from file URL validation.
- See [review](topics/review-file-scheme.md) and [module implementation](modules/m-browser/impl.md).

## t-record: Mac build and actual device recording test

No GitHub Actions. Use the existing project setup and Android SDK as above.

```bash
git fetch origin && git switch file-scheme && git pull --ff-only
git diff --check origin/master...HEAD
./gradlew :app:testDebugUnitTest --tests '*BrowserRecorderPagePolicyTest'
./gradlew :app:assembleDebug
# After installing the real debug APK:
adb shell run-as excp.rikkahub.debug mkdir -p files/webview
adb shell "run-as excp.rikkahub.debug sh -c 'cat > files/webview/t-record.html'" < app/src/main/assets/demo/t-record.html
adb shell run-as excp.rikkahub.debug pwd
```

Construct `file://<PWD>/files/webview/t-record.html` using the actual output of `run-as ... pwd`. In RikkaHub Agent, enable `browser_open` and ask the model to open that local URL. Or ask it to generate equivalent standalone HTML and write it under `files/webview` using the file tools the user has already tested. Confirm that the page displays **four** buttons. Start → native Android permission + consent → speak several seconds → Stop → `recording_saved` event (id, path, bytes > 0) → Play → audible sound → `playback_finished`.

Verify file exists using `adb shell run-as excp.rikkahub.debug ls -l files/recordings`. Negative scenarios: decline consent, deny mic permission, try an ordinary https page, navigate away during capture, background the app, call `RikkaRecorder.play("../../secret")` (must be denied). **Do not** treat Android media APIs as verified until a real emulator with a microphone or physical phone has recorded audible sound. The HTML is shipped in APK assets for reproducibility but must be staged into the approved app `files/webview` tree before `browser_open`; APK assets aren't authorized by the file-scheme path policy.

For genuine Agent generation (not staging a bundled example), prompt:
> Generate a self-contained local HTML/JavaScript recorder card under the app's files/webview folder, display it via browser_open, use RikkaRecorder.start(), stop(), play(id), stopPlayback() and the rikka-recorder CustomEvent to render status and saved file metadata. Ask me to test the Android microphone confirmation, save, and playback. Do not use the browser's MediaRecorder/getUserMedia; use the Android native bridge.

Keep test commands, output, failure cases, actual audio evidence and final SHA in [.dsu ticket state](tickets/t-record/state.md).

### t-record required Mac verification evidence table

In `t-record/state.md`, record the actual Gradle exit code for `testDebugUnitTest` and `assembleDebug`; Android device model/API level; actual `browser_open` URL; whether `typeof RikkaRecorder` is `object` in the page; consent dialog screenshot or confirmation; RECORD_AUDIO permission grant/deny outcomes; `recording_started`, `recording_saved`, `playing`, `playback_finished` UI events; actual nonzero m4a file byte size from `adb run-as`; and confirmation that playback is **audible**. Record denial navigation and background cases too. A source-only success or Node fake playback is not enough.

### t-record model-generated HTML acceptance
Use a tool-capable RikkaHub model that has `browser_open` enabled. Say only “生成一个有开始录音、停止保存、播放、停止播放按钮的本地 HTML/JavaScript 录音小程序，并在 RikkaHub 中打开。” Confirm from Agent's actual generated HTML that it used `RikkaRecorder.start()` / `stop()` / `play(id)` and listened for `rikka-recorder`; confirm the file tool wrote the generated HTML into the app's approved `files/webview` directory. Then verify actual `browser_open` URL and the four-button/recording/playback Android sequence. Distinguish this autonomous generation test from the bundled sample HTML test.
