# Strict Git diff audit — t-file-scheme

## For human fast reading:
Review basis: GitHub Compare `master...file-scheme` on 2026-11-13, initial base `a88f5854b4d2e2706ea2ae8304a7d66a3f9f2bbc`. Compare enumerated **18 changed files** before this review's new commit: 5 Kotlin files, 11 .dsu files, `AGENTS.md` and root `build_and_run.md`. Existing branch was 6 commits ahead/0 behind. The exact final head is in [state](../tickets/t-file-scheme/state.md). **Status: source reviewed and corrected; 10 standalone JVM policy assertions passed, Android Gradle/device smoke tests not run.**

## Method
- Used GitHub compare for the complete base/head changed-file set, fetched current Kotlin source and every .dsu text, checked which source was modified vs reference-only.
- Cross-checked `BrowserTools.browserOpenTool`, both WebViewClients, shared `configureWebViewForRikka`, `SkillManager.getSkillsDir`, `RunJsTool.resolveSkillWebviewUrl`, JUnit test source and build script.
- Ensured ticket/spec/design/module spec/module impl/decisions/review/index/build/state describe the *same* code behavior and avoid unverified claims.

## Findings and disposition
| Severity | Finding and evidence | Disposition |
|---|---|---|
| HIGH | `blockPageFileNavigation` previously returned false when current page was a non-allowed file URL, permitting navigation to other non-allowed private files. | FIXED: no exemption; deny any file destination not approved once origin is file |
| MEDIUM | A URL like `file:relative.html` has a scheme but not an absolute filesystem path. | FIXED: require URI path beginning with `/` |
| MEDIUM | Skill extension filter considered only basename; `.private/secret.html` could be directly opened. | FIXED: reject any hidden component below skill root |
| MEDIUM | The prior docs incorrectly claimed both "disallow query/fragment" and "support query/fragment". | FIXED: canonical file resolution uses URI path while query/fragment stay in navigation URL |
| MEDIUM | `topics/review-file-scheme.md` contained literal `\
7.`/ `\
8.` instead of actual Markdown lines. | FIXED: rebuilt review doc with table and real paragraphs |
| HIGH / OPEN | Existing `BrowserWebViewConfig.kt` enables `allowFileAccessFromFileURLs=true`. Tool and top-level navigation allowlists do not control all local JS fetch/XHR or subresources. A hostile allowed HTML asset may read other private files. | OPEN: no safe-untrusted-file guarantee; requires separate hardening design / device tests |
| MEDIUM / OPEN | No actual Android Gradle compile, full test suite, foreground or headless device render evidence in this execution. | OPEN: Mac agent must run verification and fix compilation or behavioral failures |
| MEDIUM / OPEN | SkillWebviewCard bypasses model tool as intended. Its unrestricted app-originated file URL path must be explicitly considered in threat model. | OPEN: preserve legacy behavior; separately audit malicious skill content |

## Functional checks to complete on Mac
- `./gradlew :app:testDebugUnitTest --tests '*BrowserFileSchemePolicyTest'` and debug assemble.
- Install on emulator/device, run permitted staged HTML with JS/CSS, browser tools and click; verify root path, URL query/hash, HTTP regression and error envelopes.
- Verify JS skill `files/skills/<skill-name>/ui.html` with assets and reject `.env`, `SKILL.md`, hidden subdirectories.
- Attempt remote→file, allowed→secret, legacy-unapproved→secret navigations; confirm both foreground and headless behavior.
- Explicitly test whether a page with file-origin JS can fetch an app-private file; do not silently label it secure if fetch succeeds.

## Evidence classification
- **Verified**: GitHub compare, changed-file mapping, source code inspection, documentation comparison and GitHub commit writes.
- **Local JVM compilation and deterministic checks (this audit)**: a mirror of the final pure `BrowserFileSchemePolicy` implementation, excluding comments, was compiled with installed `kotlinc` and executed under `java -jar`. All **10 assertions passed**: staged HTML, query/hash, private file rejection, relative URI rejection, named skill HTML, hidden child directory rejection, remote→file block, unapproved→unapproved block, unapproved→approved allow, approved→private block. This was a manually mirrored policy harness, **not** a Git checkout nor the repository JUnit tests. The earlier seven-assertion policy exercise is superseded.
- **Not verified**: repository JUnit Gradle task, Android app build, emulator/device runtime, headless screenshot pathway or hostile file JS isolation. The GitHub connector provided the true source of the current branch and diff; there was no local clone against which to execute `git diff --check`.

## Recommendation to Mac agent
Start with [ticket state](../tickets/t-file-scheme/state.md), run [build and run](../build-and-run.md), then decide with the user whether hostile HTML should be in-scope. If it is, the existing WebView origin configuration requires a substantive isolation change, not a simple white-list patch.
