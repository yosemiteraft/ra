package me.rerere.rikkahub.ui.components.message.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import me.rerere.common.http.jsonObjectOrNull
import me.rerere.rikkahub.R
import me.rerere.rikkahub.browser.BrowserActivity
import me.rerere.rikkahub.browser.BrowserToolDefaults
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull

/**
 * t-record requirement: after `browser_open` succeeds, the chat must keep a clickable
 * object that reopens the page. Without this card the browser is the only surface that
 * knows about the page; once the user leaves BrowserActivity the generated program (for
 * example the Agent-written recorder card) is unreachable from the conversation, and the
 * only way back is Settings → Browser → retype the file URL.
 *
 * This is deliberately an inline [ToolUIRenderer.Summary] card: the tool step content is
 * always rendered (the step starts expanded and its content is the Summary slot), so the
 * card stays visible in the chat transcript and survives app restarts because the URL is
 * read from the persisted tool output, not from [me.rerere.rikkahub.browser.BrowserController].
 *
 * Data source, in order:
 *  1. `output.current_url` — the final URL after redirects/load, written by
 *     `BrowserTools.browserOpenTool`.
 *  2. `input.url` — fallback for older persisted messages whose output text was truncated
 *     by `maybeTruncateToolOutput`.
 *
 * The button launches [BrowserActivity.intent], i.e. the same in-app browser the tools
 * drive, so the file-scheme trust policy still applies to the reopened URL.
 */
object BrowserOpenToolUI : ToolUIRenderer {

    override val toolName: String = BrowserToolDefaults.OPEN

    @Composable
    override fun title(context: ToolUIContext): String =
        stringResource(R.string.chat_message_tool_browser_open_title)

    private fun succeeded(context: ToolUIContext): Boolean =
        context.content?.jsonObjectOrNull
            ?.get("success")
            ?.jsonPrimitiveOrNull
            ?.booleanOrNull == true

    private fun landingUrl(context: ToolUIContext): String? =
        context.content.getStringContent("current_url")?.takeIf { it.isNotBlank() }
            ?: (context.arguments as? JsonObject)
                ?.get("url")
                ?.jsonPrimitiveOrNull
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }

    private fun landingTitle(context: ToolUIContext): String? =
        context.content.getStringContent("title")?.takeIf { it.isNotBlank() }

    override fun hasSummary(context: ToolUIContext): Boolean =
        succeeded(context) && landingUrl(context) != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        if (!hasSummary(context)) return
        val url = landingUrl(context) ?: return
        val pageTitle = landingTitle(context)
        val activityContext = LocalContext.current

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.chat_message_tool_browser_open_card_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = pageTitle
                        ?: stringResource(R.string.chat_message_tool_browser_open_card_untitled),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(onClick = {
                        // Reopen through the in-app browser so the same file-scheme policy,
                        // persistent profile and RikkaRecorder bridge apply.
                        runCatching {
                            activityContext.startActivity(BrowserActivity.intent(activityContext, url))
                        }
                    }) {
                        Text(stringResource(R.string.chat_message_tool_browser_open_card_open))
                    }
                }
            }
        }
    }
}
