# Ticket t-file-scheme

## For human fast reading:
User requested: create branch `file-scheme` in `yosemiteraft/ra`, enable WebView `file://` for local HTML/JS with Agent tool access, create `build_and_run.md`, and commit changes. **No GitHub Actions.** Follow Library `structure.md`: this ticket folder contains only the ticket and [state](state.md).

## Requirements
1. Agent `browser_open` accepts a valid existing local HTML file URL and opens it via existing visible or headless WebView path.
2. Local HTML supports normal interactive DOM/JavaScript and sibling web resources on real Android device.
3. Existing http(s) / about behavior and browser tools keep working.
4. Direct model-driven private file navigation is restricted and negative tests are added.
5. All changes use `.dsu/index.md` for discovery; specification/design/implementation live in their canonical layers, not in ticket folder.
6. Provide reproducible Mac local build/install/smoke test instructions, review actual Git diff, and push commits to the specified branch.

## Acceptance matrix
| Item | Required evidence | Current status |
|---|---|---|
| Branch and commits | GitHub ref and compare to master | implemented; see state |
| Allowed/disallowed URI policy | Gradle JUnit test report | 5 passed on Mac |
| Foreground local HTML JS/CSS | Android device/emulator run | passed for the chat card and for sibling assets opened by the Agent; see state |
| Headless local HTML JS/CSS | configured headless run | pending |
| Private-file access boundary | model URL rejections; separate JS fetch security test | JUnit policy passed; untrusted JS sandbox **not** proven |
| Regression and docs | diff/check and source-to-doc consistency | branch diff check passed; full JVM suite has five Mac path failures outside the patch |
| No GitHub Actions | local commands only | respected |

## Scope note
This ticket enables the requested local URL workflow; it does **not** silently promise a safe execution environment for hostile HTML. Existing WebView file-origin JS settings require separate isolation work if hostile/untrusted pages are a requirement. See [development spec](../../dev-spec.md), [design](../../dev-design.md), [review](../../topics/review-file-scheme.md).
