# Development knowledge index — ra / file-scheme

## For human fast reading:
**Start here only for navigation.** The browser file-scheme base is [t-file-scheme](tickets/t-file-scheme/t-file-scheme.md) on branch `file-scheme`. For the active recording ticket read [t-record state](tickets/t-record/state.md) before editing. A new Mac agent working on the file-scheme base MUST first read [ticket state](tickets/t-file-scheme/state.md) for the tested branch, Mac build and emulator results, and remaining checks, then [build-and-run](build-and-run.md). This index describes location and responsibility; it is not an implementation specification.

## Active ticket: t-record
- [t-record requirements](tickets/t-record/t-record.md): Agent-generated HTML recorder, native JS bridge, save and playback acceptance.
- [t-record state](tickets/t-record/state.md): completed source, outstanding device validation and handoff.
- [t-record code review](topics/review-t-record.md): implementation inspection and residual risks.
- [t-record browser module spec](modules/m-browser/spec.md): bridge API contract, in addition to file scheme.
- [t-record implementation](modules/m-browser/impl.md): source entrypoints and callback methods.

## Core development documents
| Path | Distinct purpose |
|---|---|
| [tickets/t-file-scheme/t-file-scheme.md](tickets/t-file-scheme/t-file-scheme.md) | User's objectives, acceptance tests, scope |
| [tickets/t-file-scheme/state.md](tickets/t-file-scheme/state.md) | Mac build and test results, emulator card demo, and remaining browser checks |
| [dev-spec.md](dev-spec.md) | What the feature must do, observable behavior |
| [dev-design.md](dev-design.md) | Architecture, trust boundaries, alternatives and unresolved security tradeoffs |
| [decisions.md](decisions.md) | Why particular approaches were chosen/rejected |
| [modules/m-browser/spec.md](modules/m-browser/spec.md) | Browser module's externally observable contract |
| [modules/m-browser/impl.md](modules/m-browser/impl.md) | Verified source functions, edits and call order |
| [modules/folder_layout.md](modules/folder_layout.md) | Source tree navigation and changed file map |
| [topics/review-file-scheme.md](topics/review-file-scheme.md) | Diff-based defect review, checks, risks and evidence |
| [build-and-run.md](build-and-run.md) | Mac local build, tests, emulator/device and failure diagnostics |

## External documentation and code
- [Root AGENTS.md](../AGENTS.md): short entry instructions and no-GitHub-Actions rule.
- [Root build_and_run.md](../build_and_run.md): compatibility pointer, not a duplicate of the canonical .dsu build document.
- [Project README.md](../README.md): upstream project overview.
- `app/src/main/java/me/rerere/rikkahub/browser/`: file policy and foreground/headless WebView hosts.
- `app/src/main/java/me/rerere/rikkahub/data/ai/tools/local/BrowserTools.kt`: model-facing `browser_open` tool.
- `app/src/main/java/me/rerere/rikkahub/ui/components/message/tools/BrowserToolUIs.kt`: chat card that reopens a `browser_open` page (t-record chat-reachability requirement).
- `app/src/main/java/me/rerere/rikkahub/data/files/SkillManager.kt`: actual skill-root discovery.
- `app/src/main/java/me/rerere/rikkahub/skills/js/RunJsTool.kt`: actual skill-authored WebView URLs.
- `app/src/test/java/me/rerere/rikkahub/browser/BrowserFileSchemePolicyTest.kt`: new pure path-policy tests.

No per-folder index files. No ontology sequence/pseudocode file was created because this ticket did not explicitly request one. Update this index whenever adding/removing .dsu documents.

Review evidence for `t-record` is in [review-t-record](topics/review-t-record.md) with a per-item verification matrix in [state](tickets/t-record/state.md). The 4-button HTML example is source at `app/src/main/assets/demo/t-record.html`; its static asset location is not the approved runtime browser file directory. See [build-and-run](build-and-run.md) for staging instructions.
