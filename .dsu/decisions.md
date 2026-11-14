# Decision register

## D-FILE-01 — retain file:// URL surface
**Accepted** direct `file://` navigation in Agent `browser_open` for local trusted HTML, because user explicitly requested local HTML + JS in WebView. Did not replace the URL with virtual HTTPS origin in this ticket.

## D-FILE-02 — restrict direct Agent tool navigation
**Accepted** `BrowserFileSchemePolicy.isAllowed` using real canonical file and root paths. Two staged directories are unrestricted *within those directories*, with a named-skill-tree web-asset extension allowlist. Prevents direct tool navigation to unrelated private files; no claim of sandboxed hostile HTML.

## D-FILE-03 — shared page-initiated navigation guard
**Accepted** a common policy for foreground and headless top-level file navigation. Removed the historical exception that allowed an unapproved file page to navigate to another unapproved private file. Preserves app-originated `loadUrl` behavior (not intercepted by `shouldOverrideUrlLoading`).

## D-FILE-04 — legacy WebView settings unchanged
**Accepted for compatibility, with explicit risk:** `allowFileAccess=true` and `allowFileAccessFromFileURLs=true` predate the ticket. This is a **security limitation**, not evidence that local JavaScript cannot read unrelated files. A hardened viewer for untrusted content is a **separate, currently unresolved design decision** requiring migration and device tests.

## D-FILE-05 — validation and Git workflow
**Accepted:** GitHub connector for commits, local Mac Gradle/adb for full verification, **no GitHub Actions**. Do not mark ticket done based on source review or policy-only JVM checks. See [review](topics/review-file-scheme.md) and [build instructions](build-and-run.md).

## D-RECORD-01 — Foreground local browser rather than chat inline
T-record reuses the working Agent `browser_open(file:///.../files/webview/...html)` path, with visible `BrowserActivity`. It does not pretend `SkillWebviewCard` is an inline WebView.

## D-RECORD-02 — Native recorder and playback bridge
`BrowserRecorderBridge` exposes only start/stop/play/stopPlayback, registers before page load, uses Android `MediaRecorder`/ `MediaPlayer`, records to app-owned persistent `files/recordings`, and reports structured JS CustomEvents. It does not grant unrestricted `file://` access to recording files.

## D-RECORD-03 — Require native per-recording consent
Recording starts only following user confirmation and Android RECORD_AUDIO permission. Automatic background recording without a foreground page was rejected for this ticket. WebView interface is accessible to frames, so trusted HTML is mandatory; this ticket does not certify hostile HTML sandbox isolation.

## D-RECORD-04 — DSU handoff and build validation
Use the existing `file-scheme` branch, new `t-record` ticket, a single `.dsu/index.md`, Gradle and device verification on Mac, and no GitHub Actions.
