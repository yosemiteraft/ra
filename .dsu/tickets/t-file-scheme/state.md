# t-file-scheme handoff (2026-11-13)

## For human fast reading:
The Mac built the Android debug APK from `file-scheme` at `4a33bec0`. All five file policy tests passed. On `emulator-5554`, RikkaHub sent the prompt to local Qwen, obtained a WebView card through `run_js`, and opened it with **Open in browser**. Taps on the model-generated page changed the live DOM count from 0 to 1 to 6, then back to 0. The Agent also opened a staged local page and the WebView loaded its sibling CSS and JS. The full app unit suite has five Mac path failures outside this patch. Headless and remaining browser checks are open. No GitHub Actions.

## Build and test evidence

- Local and remote `file-scheme` matched at `4a33bec03a5a35f0eddb24891c78d058f3d50e70` before this handoff update. The branch base is `a88f5854b4d2e2706ea2ae8304a7d66a3f9f2bbc` on `origin/master`. `git diff --check origin/master...HEAD` passed before the build.
- Ran `git submodule update --init --recursive`. Gradle installed Android SDK Platform 37.0 and NDK 28.2.13676358.
- `./gradlew :app:testDebugUnitTest --tests '*BrowserFileSchemePolicyTest' :app:assembleDebug` succeeded. JUnit reported 5 tests, 0 failures, 0 skipped. The APK is `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
- `./gradlew :app:testDebugUnitTest` ran 1,712 tests, with 1,707 passing and five failing. The failing classes are `PathSafetyGuardTest` (two), `FileBatchToolsTest`, `FileManagerToolTest`, and `TranscribeAudioToolValidationTest`. On this case-insensitive Mac, Java returns `/System` for `File("/system").canonicalPath`, but the unchanged guard compares it with lowercase Android paths. See the local report at `app/build/reports/tests/testDebugUnitTest/index.html`.

## Emulator evidence

- Installed the ARM64 APK on `emulator-5554` (Android API 34). Its package is `excp.rikkahub.debug`. The existing `me.rerere.rikkahub.debug` app was left installed.
- Local Qwen 3.5 9B generated a complete offline HTML/CSS/JS page on the Mac. The demo staged that output under `files/skills/pocket-counter-demo/counter.html` with a JS skill entry point. In the emulator, the chat prompt asked Qwen to call `run_js` for that skill. After one-time approval, the tool rendered a **Direct viewer** card. **Open in browser** opened the page with `?source=chat#counter`; real taps produced DOM counts `0 → 1 → 6 → 0 → 5`. The app did not generate or install a new skill from scratch in the chat.
- A second chat prompt called `browser_open` on `file:///data/user/0/excp.rikkahub.debug/files/webview/smoke.html?mode=smoke#go`. The real WebView kept the full URL, displayed `Sibling JS loaded`, and returned the CSS background `rgb(232, 241, 255)`.
- The tool chain after `browser_open` is not confirmed. Returning to the chat to approve `browser_eval_js` closed the browser activity. That evaluation returned `browser_not_open`. The manual card clicks above are independent evidence of WebView interactivity.
- The local evidence folder is `.working/file-scheme/`. It contains the generation prompt, chat prompt, model-generated page, screenshots, and DOM verification log. These fixtures are outside the application source.

## Remaining checks

- Validate `browser_get_text`, `browser_eval_js`, and `browser_click` while the browser session stays bound. Use a headless session if configured.
- Verify the remaining device scenarios in [Build and run](../../build-and-run.md): HTTP and about URLs, direct denial of a synthetic file outside the allowed roots, and page-initiated navigation. Keep the direct URL gate separate from file-origin JavaScript behavior.
- `allowFileAccessFromFileURLs=true` remains. The navigation policy does not turn a file URL into a sandbox for untrusted JavaScript. See [Design](../../dev-design.md) before changing the legacy skill viewer.

## Related material

- [Ticket and acceptance matrix](t-file-scheme.md)
- [Development index](../../index.md)
- [Module implementation](../../modules/m-browser/impl.md)
- [Source audit](../../topics/review-file-scheme.md)
