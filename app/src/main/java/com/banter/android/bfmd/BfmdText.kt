package com.banter.android.bfmd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.ui.theme.BanterColors

private sealed class ParagraphBlock {
    data class Text(val text: AnnotatedString, val align: Align?) : ParagraphBlock()
    data object Rule : ParagraphBlock()
}

@Composable
fun BfmdText(
    source: String,
    modifier: Modifier = Modifier,
    defaultColor: Color = BanterColors.TextPrimary,
    defaultSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 22.sp
) {
    val blocks = remember(source, defaultColor, defaultSize) {
        buildParagraphs(source, defaultColor, defaultSize)
    }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is ParagraphBlock.Rule -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(BanterColors.Divider)
                    )
                }
                is ParagraphBlock.Text -> {
                    if (block.text.isNotEmpty()) {
                        val align = when (block.align) {
                            Align.Center -> TextAlign.Center
                            Align.Right -> TextAlign.End
                            Align.Justify -> TextAlign.Justify
                            else -> TextAlign.Start
                        }
                        BasicText(
                            text = block.text,
                            style = TextStyle(
                                color = defaultColor,
                                fontSize = defaultSize,
                                lineHeight = lineHeight,
                                textAlign = align
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

private fun buildParagraphs(
    source: String,
    defaultColor: Color,
    defaultSize: TextUnit
): List<ParagraphBlock> {
    val result = parse(source)
    val out = mutableListOf<ParagraphBlock>()
    var builder = AnnotatedString.Builder()
    var paragraphAlign: Align? = null

    fun flush() {
        val built = builder.toAnnotatedString()
        if (built.isNotEmpty()) {
            out += ParagraphBlock.Text(built, paragraphAlign)
        }
        builder = AnnotatedString.Builder()
        paragraphAlign = null
    }

    for (run in result.runs) {
        if (run.style.hr) {
            flush()
            out += ParagraphBlock.Rule
        }
        val parts = run.text.split('\n')
        parts.forEachIndexed { idx, part ->
            if (part.isNotEmpty()) {
                paragraphAlign = run.style.align ?: paragraphAlign
                builder.withStyle(spanStyleFor(run.style, defaultColor, defaultSize)) {
                    append(part)
                }
            }
            if (idx < parts.lastIndex) flush()
        }
    }
    flush()
    return out
}

private fun spanStyleFor(style: Style, defaultColor: Color, defaultSize: TextUnit): SpanStyle {
    val sizeSp = (style.size ?: defaultSize.value).sp
    val weight = when {
        style.bold || (style.heading != null && style.heading!! <= 3) -> FontWeight.Bold
        style.heading != null -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }
    val decorations = buildList {
        if (style.underline) add(TextDecoration.Underline)
        if (style.strike) add(TextDecoration.LineThrough)
    }
    return SpanStyle(
        color = style.color?.let { Color(it.toArgb()) } ?: defaultColor,
        background = style.background?.let { Color(it.toArgb()) } ?: Color.Transparent,
        fontSize = sizeSp,
        fontWeight = weight,
        fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when (decorations.size) {
            0 -> TextDecoration.None
            1 -> decorations[0]
            else -> TextDecoration.combine(decorations)
        },
        fontFamily = if (style.font != null) FontFamily.Monospace else null
    )
}

fun bfmdToAnnotated(
    source: String,
    defaultColor: Color = BanterColors.TextPrimary,
    defaultSize: TextUnit = 15.sp
): AnnotatedString {
    val result = parse(source)
    return buildAnnotatedString {
        for (run in result.runs) {
            withStyle(spanStyleFor(run.style, defaultColor, defaultSize)) {
                append(run.text)
            }
        }
    }
}
