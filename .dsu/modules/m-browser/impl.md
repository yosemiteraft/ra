# Browser source implementation — file scheme

## For human fast reading:
Use this as a verified source-map for the Mac agent, not a duplicate of the feature spec. Source ref: branch `file-scheme`; latest Git SHA is in [ticket state](../../tickets/t-file-scheme/state.md). Read the code before changing it.

## Files and functions
| Source path | Exact function / responsibility |
|---|---|
| `app/src/main/java/me/rerere/rikkahub/data/ai/tools/local/BrowserTools.kt` | `browserOpenTool`: parse URL scheme and return `scheme_not_allowed` / `file_access_denied` before regular foreground/headless dispatch; use `FileFolders.SKILLS` |
| `app/src/main/java/me/rerere/rikkahub/browser/BrowserFileSchemePolicy.kt` | `isAllowed`: canonical file/root, URL query/fragment, skill file whitelist; `blockPageFileNavigation`: top-level navigation rule |
| `app/src/main/java/me/rerere/rikkahub/browser/BrowserView.kt` | foreground `WebViewClient.shouldOverrideUrlLoading` invokes shared block rule |
| `app/src/main/java/me/rerere/rikkahub/browser/HeadlessBrowserSession.kt` | headless `WebViewClient.shouldOverrideUrlLoading` invokes the same rule |
| `app/src/main/java/me/rerere/rikkahub/browser/BrowserWebViewConfig.kt` | existing `configureWebViewForRikka`: `allowFileAccess=true`, `allowFileAccessFromFileURLs=true` (unchanged, security caveat) |
| `app/src/main/java/me/rerere/rikkahub/data/files/SkillManager.kt` | `getSkillsDir`: real skill tree `context.filesDir/skills` |
| `app/src/main/java/me/rerere/rikkahub/skills/js/RunJsTool.kt` | `resolveSkillWebviewUrl`: skill UI HTML URLs, including query/fragment |
| `app/src/test/java/me/rerere/rikkahub/browser/BrowserFileSchemePolicyTest.kt` | local policy JUnit tests for authorized/disauthorized URLs, hidden skill files, relative paths, symlink, and navigation |

## Data flow with concrete example
`browser_open({"url":"file:///data/user/0/excp.rikkahub.debug/files/skills/virtual-piano/ui.html?mode=test"})` → `BrowserTools.browserOpenTool` → `BrowserFileSchemePolicy.isAllowed(url, [filesDir/webview,cacheDir/skill-webview],filesDir/skills)` → existing `BrowserActivity.intent` or `HeadlessBrowserSession.start` → `WebView.loadUrl(url)`. The absolute sandbox path varies per installed package/device; the example is illustrative, not discovered from a device.

## Source diff / validation
Inspect `git diff master...file-scheme -- app/src/main app/src/test`, then run `./gradlew :app:testDebugUnitTest --tests '*BrowserFileSchemePolicyTest'` and `./gradlew :app:assembleDebug`. Check JS/CSS and page navigation on device. The [ticket state](../../tickets/t-file-scheme/state.md) records the Mac build, unit tests, and emulator results.

## Important implementation limitation
`shouldOverrideUrlLoading` does not protect every subresource or JS-initiated request. A local page may still be able to read private files via the existing permissive file-origin WebSettings; see [review](../../topics/review-file-scheme.md).

## t-record actual source map

- `BrowserActivity.kt`: `onWebViewReady` creates `BrowserRecorderBridge` and calls `addJavascriptInterface("RikkaRecorder")`; ActivityResultContracts.RequestPermission supplies mic permission callback; `onStop` and `onDestroy` release recording resources.
- `BrowserView.kt`: `WebViewHost` registers bridge using `onWebViewReady` before initial `loadUrl`. Both foreground browser and callback remain unchanged otherwise.
- `BrowserRecorderBridge.kt`: `start` shows native consent/permission; `startRecording` initializes MediaRecorder; `finishRecording` calls stop and returns the saved id; `play` uses MediaPlayer; `stopPlayback`/onBackground/release clean up; `emit` calls `evaluateJavascript` with JSON-escaped CustomEvent data.
- `BrowserRecorderPagePolicy.kt`: standalone trusted top-level HTML path policy, recording ID validation.
- `BrowserRecorderPagePolicyTest.kt`: local allowed/denied HTML path, symlink, recording ID tests.
- `app/src/main/assets/demo/t-record.html`: real HTML/JS example with four buttons, status UI, event handler.
- `app/src/main/java/me/rerere/rikkahub/ui/components/message/tools/BrowserToolUIs.kt`: `BrowserOpenToolUI` renders the chat card for `browser_open` (reads `output.current_url`/`output.title`, falls back to `input.url`); the **Open in browser** button calls `BrowserActivity.intent(context, url)`. Registered in `ToolUIRegistry` next to the other tool renderers, so it re-renders from persisted messages, not from live BrowserController state. Strings: `chat_message_tool_browser_open_*` in `res/values/strings.xml`.

**Important**: `addJavascriptInterface` exposes methods to frames within the approved top-level document. The user must trust the entire HTML card and its embedded content. Mac build and Android audio capture/playback verification is required; source creation is not proof of successful hardware recording.

### t-record verified ordering / browser_open regression fix
`BrowserView.WebViewHost` now performs **(1)** `onWebViewReady(this)` to register the `RikkaRecorder` bridge, **(2)** `loadUrl(initialUrl)`, **(3)** `onInitialLoadStarted(this)` to bind the BrowserController. Do not bind before initial load starts; fresh `browser_open` uses `awaitBind` and otherwise may mistake about:blank for the requested page. `BrowserActivity` installs the interface and cookie policy in step 1, and binds the controller in step 3.

- `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`: explicit `recordings/` exclusion across legacy backups and API 31+ cloud/transfer policies.

### t-record host checks
The pure path/ID policy logic was mirrored in an independent Kotlin harness and 11 checks passed. The demo's JavaScript was copied into a Node syntax check/mock DOM and worked through start/stop/save/play event transitions. The full Android SDK, Gradle JUnit suite and Android microphone hardware tests were **not executed**. See [ticket state](../../tickets/t-record/state.md).

- `BrowserTools.kt:browserOpenTool`: model-facing tool description now exposes the `RikkaRecorder` JavaScript contract, required native confirmation, local HTML restriction and lack of headless support. This closes the model-discovery gap without introducing another AI tool.

- `BrowserRecorderBridge.onNavigation(url)` tracks both `activePageUrl` and `playbackPageUrl` and releases the mic/player on navigation away from the exact starting card, including approved-local-to-approved-local navigation.

- `BrowserRecorderBridge.play(id)`: the player instance is stored immediately after construction, before `setDataSource`/`prepare`; on any exception the native player is released. `setOnCompletionListener` checks the callback's player identity to ignore stale completions.
