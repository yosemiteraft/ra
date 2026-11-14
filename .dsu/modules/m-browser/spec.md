# Browser module contract — file scheme

## For human fast reading:
This module validates initial Agent browser URLs and page-initiated top-level file navigations. It does not make local HTML a security sandbox. Inputs/outputs must match [development functional spec](../../dev-spec.md).

## Entry interfaces
- `browserOpenTool(context: Context, invocationContext: ToolInvocationContext?) : Tool`. Input JSON `{"url": "<string>"}`. Missing URL → `missing_url`. Unsupported URI scheme → `scheme_not_allowed`. Disallowed or missing file URL → `file_access_denied`. Accepted URI → normal browser success/error envelope from existing tool dispatcher.
- `BrowserFileSchemePolicy.isAllowed(url: String, roots: List<File>, skillAssetRoot: File? = null): Boolean`: no Android dependency; checks on-disk existing file, canonical containment, file URI absolute path/authority, skill extension and hidden path constraints. Query/fragment supported.
- `BrowserFileSchemePolicy.blockPageFileNavigation(currentUrl: String?, destinationUrl: String, roots: List<File>, skillAssetRoot: File? = null): Boolean`: true means WebViewClient should consume/block top-level file navigation. Non-file destination → false; remote→file → true; file→unapproved-file → true; file→approved-file → false.

## Configuration dependencies
- `BrowserWebViewConfig.configureWebViewForRikka` remains source of shared Android WebSettings.
- `SkillManager.getSkillsDir()` uses `FileFolders.SKILLS = "skills"`.
- `BrowserActivity` foreground and `HeadlessBrowserSession` are existing host paths.

## Boundary
These are URL/navigation decisions, *not* a byte-level WebView file read authorization policy. Device-level tests and hostile-file hardening are separately required. [Implementation](impl.md).

## t-record native recorder Bridge API (foreground BrowserActivity only)

JavaScript bridge object name: `window.RikkaRecorder`. Methods are `start():void`, `stop():void`, `play(id:String):void`, `stopPlayback():void`; native calls are asynchronous. The page subscribes with `window.addEventListener("rikka-recorder", event => ...event.detail)`. Event types: `recording_started`, `recording_saved`, `playing`, `playback_finished`, `playback_stopped`, `error`. A saved event includes `id`, `fileName`, `absolutePath`, `bytes`; failure events include `code`.

Only an existing HTML page under `context.filesDir/webview` is allowed to initiate operations. Native per-start confirmation and RECORD_AUDIO permission are required. All files are created inside app private `context.filesDir/recordings`, not a shared gallery or downloadable public URL. The bridge is registered only in visible BrowserActivity, not HeadlessBrowserSession or the generic Compose WebView. It uses a fixed recording ID format to prevent arbitrary file path injection.

Security boundary: on top-level URL and path; not a safe sandbox against untrusted iframe JS. See [t-record design](../../dev-design.md) and [review](../../topics/review-t-record.md).

## Agent tool discovery
`browser_open` model-facing tool description explicitly advertises this recorder Bridge (start/stop/play/stopPlayback) and callback event schema so an Agent receiving a request to generate a recorder card can produce the correct JS. It is still the Agent's responsibility to write the HTML into an allowed local folder and invoke browser_open. The browser-open tool does not itself write files or start audio automatically.
