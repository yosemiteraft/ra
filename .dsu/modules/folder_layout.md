# Folder layout — existing and modified source

No new Android source directories were introduced. The ticket adds two Kotlin files under existing folders and modifies three existing Kotlin sources.

| File | Change status | Role |
|---|---|---|
| [BrowserFileSchemePolicy.kt](../../app/src/main/java/me/rerere/rikkahub/browser/BrowserFileSchemePolicy.kt) | new | pure local URI/permission path checks |
| [BrowserFileSchemePolicyTest.kt](../../app/src/test/java/me/rerere/rikkahub/browser/BrowserFileSchemePolicyTest.kt) | new | JVM policy regression tests |
| [BrowserTools.kt](../../app/src/main/java/me/rerere/rikkahub/data/ai/tools/local/BrowserTools.kt) | modified | Agent `browser_open` entry check |
| [BrowserView.kt](../../app/src/main/java/me/rerere/rikkahub/browser/BrowserView.kt) | modified | foreground top-level file navigation |
| [HeadlessBrowserSession.kt](../../app/src/main/java/me/rerere/rikkahub/browser/HeadlessBrowserSession.kt) | modified | headless top-level file navigation |

Reference only (not changed): [BrowserWebViewConfig.kt](../../app/src/main/java/me/rerere/rikkahub/browser/BrowserWebViewConfig.kt), [SkillManager.kt](../../app/src/main/java/me/rerere/rikkahub/data/files/SkillManager.kt), [RunJsTool.kt](../../app/src/main/java/me/rerere/rikkahub/skills/js/RunJsTool.kt), [SkillWebviewCard.kt](../../app/src/main/java/me/rerere/rikkahub/ui/components/message/SkillWebviewCard.kt).

Documentation lives under `.dsu/`, indexed only at [index.md](../index.md). The per-ticket folder contains exactly `t-file-scheme.md` and `state.md`. Build documentation canonical location is `.dsu/build-and-run.md`, with root `build_and_run.md` pointing there.

## t-record additions

- `app/src/main/java/me/rerere/rikkahub/browser/BrowserRecorderBridge.kt`: native microphone and audio playback interface
- `app/src/main/java/me/rerere/rikkahub/browser/BrowserRecorderPagePolicy.kt`: local-card origin and ID policy
- `app/src/main/java/me/rerere/rikkahub/browser/BrowserActivity.kt`: bridge registration, permission and lifecycle additions
- `app/src/main/java/me/rerere/rikkahub/browser/BrowserView.kt`: initial load follows bridge registration
- `app/src/test/java/me/rerere/rikkahub/browser/BrowserRecorderPagePolicyTest.kt`: path safety tests
- `app/src/main/assets/demo/t-record.html`: sample standalone card
- `.dsu/tickets/t-record/t-record.md` + `state.md`: ticket scope and verified progress
- `.dsu/topics/review-t-record.md`: audit

- `app/src/main/res/xml/backup_rules.xml` + `data_extraction_rules.xml`: explicit exclusion for private microphone recordings from cloud backup/device transfer.
