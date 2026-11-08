package com.banter.android.bfmd

/**
 * BFMD (Better Formatted Markdown) — Banter chat text formatting.
 * Port of crates/banter/src/bfmd.rs / docs/bfmd.md. Not CommonMark.
 */

const val DEFAULT_SIZE_PT = 16f
const val DEFAULT_EMOTE_PX = 22f
const val EMOTE_PX_MIN = 16f
const val EMOTE_PX_MAX = 128f

data class Rgba(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    companion object {
        fun rgb(r: Int, g: Int, b: Int) = Rgba(r, g, b, 255)
    }

    fun toArgb(): Int = (a shl 24) or (r shl 16) or (g shl 8) or b
}

enum class Align { Left, Center, Right, Justify }

data class Style(
    var size: Float? = null,
    var emoteSize: Float? = null,
    var color: Rgba? = null,
    var background: Rgba? = null,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var strike: Boolean = false,
    var font: String? = null,
    var align: Align? = null,
    var heading: Int? = null,
    var hr: Boolean = false
) {
    fun copyStyle() = copy()
    fun isDefault() = this == Style()
    fun sizeOrDefault() = size ?: DEFAULT_SIZE_PT
}

data class Run(val text: String, val style: Style)

data class Diagnostic(val message: String, val index: Int)

data class ParseResult(
    val runs: List<Run>,
    val diagnostics: List<Diagnostic> = emptyList(),
    val version: Int = 1
)

private sealed class ParsedCommand {
    data class Set(val name: String, val value: Value?) : ParsedCommand()
    data class Close(val name: String) : ParsedCommand()
    object CloseAll : ParsedCommand()
}

private sealed class Value {
    data class Number(val n: Float) : Value()
    data class Bool(val b: Boolean) : Value()
    data class Hex(val c: Rgba) : Value()
    data class Ident(val s: String) : Value()
    data class Quoted(val s: String) : Value()
}

private val KNOWN_NAMES = setOf("v", "s", "e", "c", "bg", "b", "i", "u", "k", "f", "a", "h", "hr")

fun parse(source: String): ParseResult {
    val src = source.removePrefix("\uFEFF")
    val bytes = src.toByteArray(Charsets.UTF_8)
    // Work on String indices via UTF-16 for simplicity with Kotlin String
    return parseUtf16(src)
}

private fun parseUtf16(source: String): ParseResult {
    var i = 0
    var style = Style()
    val runs = mutableListOf<Run>()
    val text = StringBuilder()
    val diagnostics = mutableListOf<Diagnostic>()
    var version = 1
    var firstGroup = true

    fun flush() {
        if (text.isEmpty()) return
        runs += Run(text.toString(), style.copyStyle())
        text.clear()
    }

    while (i < source.length) {
        val c = source[i]
        if (c == '\\') {
            val (ch, next, diag) = parseTextEscape(source, i)
            diag?.let { diagnostics += it }
            text.append(ch)
            i = next
            continue
        }
        if (c == '<' && groupStartsAt(source, i)) {
            flush()
            val parsed = parseGroup(source, i, diagnostics)
            if (parsed != null) {
                val (next, cmds) = parsed
                if (firstGroup) {
                    for (cmd in cmds) {
                        if (cmd is ParsedCommand.Set && cmd.name == "v") {
                            val n = (cmd.value as? Value.Number)?.n
                            if (n != null) {
                                version = n.toInt()
                                if (version != 1) {
                                    diagnostics += Diagnostic("unknown BFMD version $version", i)
                                }
                            }
                        }
                    }
                    firstGroup = false
                } else {
                    firstGroup = false
                }
                applyCommands(cmds, style, diagnostics, i)
                i = next
            } else {
                diagnostics += Diagnostic("unclosed BFMD group", i)
                text.append('<')
                i += 1
            }
            continue
        }
        text.append(c)
        i += 1
        firstGroup = false
    }
    flush()
    return ParseResult(runs, diagnostics, version)
}

fun plainText(source: String): String =
    parse(source).runs.joinToString("") { it.text }

private fun groupStartsAt(source: String, i: Int): Boolean {
    if (i >= source.length || source[i] != '<') return false
    var j = i + 1
    while (j < source.length && (source[j] == ' ' || source[j] == '\t')) j++
    return j < source.length && source[j] == '{'
}

private fun parseTextEscape(
    source: String,
    i: Int
): Triple<Char, Int, Diagnostic?> {
    if (i + 1 >= source.length) return Triple('\\', i + 1, null)
    val next = source[i + 1]
    return when (next) {
        'n' -> Triple('\n', i + 2, null)
        '<', '{', '>', '}', '\\' -> Triple(next, i + 2, null)
        else -> Triple(next, i + 2, Diagnostic("unknown escape \\$next", i))
    }
}

private fun parseGroup(
    source: String,
    start: Int,
    diagnostics: MutableList<Diagnostic>
): Pair<Int, List<ParsedCommand>>? {
    var i = start + 1
    while (i < source.length && (source[i] == ' ' || source[i] == '\t')) i++
    val cmds = mutableListOf<ParsedCommand>()
    while (true) {
        while (i < source.length && (source[i] == ' ' || source[i] == '\t' || source[i] == ',')) i++
        if (i >= source.length) return null
        if (source[i] == '>') return (i + 1) to cmds
        if (source[i] == '<' && groupStartsAt(source, i)) {
            diagnostics += Diagnostic("nested BFMD group", i)
            return i to cmds
        }
        if (source[i] != '{') {
            diagnostics += Diagnostic("expected command in BFMD group", i)
            while (i < source.length && source[i] != '>' && !(source[i] == '<' && groupStartsAt(source, i))) i++
            if (i < source.length && source[i] == '>') return (i + 1) to cmds
            if (i < source.length) return i to cmds
            return null
        }
        val cmdResult = parseCommand(source, i, diagnostics)
        if (cmdResult != null) {
            cmds += cmdResult.second
            i = cmdResult.first
        } else {
            // advance past broken command
            i++
            while (i < source.length && source[i] != '}' && source[i] != '>') i++
            if (i < source.length && source[i] == '}') i++
        }
    }
}

private fun parseCommand(
    source: String,
    start: Int,
    diagnostics: MutableList<Diagnostic>
): Pair<Int, ParsedCommand>? {
    var i = start + 1
    if (i >= source.length) {
        diagnostics += Diagnostic("unclosed command", start)
        return null
    }
    if (source[i] == '}') {
        return (i + 1) to ParsedCommand.Set("", null)
    }
    if (source[i] == '!') {
        i++
        if (i < source.length && (source[i] == '}' || source[i] == '*')) {
            if (source[i] == '*') i++
            if (i < source.length && source[i] == '}') {
                return (i + 1) to ParsedCommand.CloseAll
            }
        }
        val nameStart = i
        while (i < source.length && isNameChar(source[i], i == nameStart)) i++
        if (nameStart == i) {
            diagnostics += Diagnostic("bad close command", start)
            return null
        }
        val name = source.substring(nameStart, i)
        if (i < source.length && source[i] == '}') {
            return (i + 1) to ParsedCommand.Close(name)
        }
        diagnostics += Diagnostic("unclosed close command", start)
        return null
    }
    val nameStart = i
    while (i < source.length && isNameChar(source[i], i == nameStart)) i++
    if (nameStart == i) {
        diagnostics += Diagnostic("bad set command", start)
        return null
    }
    val name = source.substring(nameStart, i)
    val value = if (i < source.length && (source[i] == ':' || source[i] == '=')) {
        i++
        val vr = parseValue(source, i, diagnostics) ?: return null
        i = vr.first
        vr.second
    } else null
    if (i < source.length && source[i] == '}') {
        return (i + 1) to ParsedCommand.Set(name, value)
    }
    diagnostics += Diagnostic("unclosed set command", start)
    return null
}

private fun isNameChar(c: Char, first: Boolean): Boolean =
    if (first) c in 'a'..'z' else c in 'a'..'z' || c in '0'..'9'

private fun parseValue(
    source: String,
    start: Int,
    diagnostics: MutableList<Diagnostic>
): Pair<Int, Value>? {
    var i = start
    if (i >= source.length) return null
    if (source[i] == '"') {
        i++
        val sb = StringBuilder()
        while (i < source.length) {
            if (source[i] == '\\' && i + 1 < source.length && (source[i + 1] == '"' || source[i + 1] == '\\')) {
                sb.append(source[i + 1]); i += 2; continue
            }
            if (source[i] == '"') return (i + 1) to Value.Quoted(sb.toString())
            sb.append(source[i]); i++
        }
        diagnostics += Diagnostic("unclosed quoted value", start)
        return null
    }
    if (source[i] == '#' || (source[i] == '0' && i + 1 < source.length && source[i + 1].lowercaseChar() == 'x')) {
        return parseHex(source, i, diagnostics)
    }
    if (source[i].isDigit()) {
        var j = i
        while (j < source.length && source[j].isDigit()) j++
        if (j < source.length && source[j] == '.') {
            j++
            while (j < source.length && source[j].isDigit()) j++
        }
        val n = source.substring(i, j).toFloatOrNull()
        return if (n != null) j to Value.Number(n) else {
            diagnostics += Diagnostic("bad number", i); null
        }
    }
    if (source[i].isLetterOrDigit() || source[i] == '_' || source[i] == '-') {
        var j = i
        while (j < source.length && (source[j].isLetterOrDigit() || source[j] == '_' || source[j] == '-')) j++
        val raw = source.substring(i, j)
        parseBool(raw)?.let { return j to Value.Bool(it) }
        return j to Value.Ident(raw)
    }
    diagnostics += Diagnostic("bad value", i)
    return null
}

private fun parseBool(raw: String): Boolean? = when (raw.lowercase()) {
    "t", "true", "1" -> true
    "f", "false", "0" -> false
    else -> null
}

private fun parseHex(
    source: String,
    start: Int,
    diagnostics: MutableList<Diagnostic>
): Pair<Int, Value>? {
    var i = start
    i = if (source[i] == '#') i + 1 else i + 2
    var n = 0
    while (i + n < source.length) {
        val ch = source[i + n].lowercaseChar()
        if (ch.isDigit() || ch in 'a'..'f') n++ else break
    }
    if (n != 6 && n != 8) {
        diagnostics += Diagnostic("bad hex color", start)
        return null
    }
    val hex = source.substring(i, i + n)
    val rgba = if (n == 6) {
        val v = hex.toLong(16)
        Rgba.rgb(((v shr 16) and 0xff).toInt(), ((v shr 8) and 0xff).toInt(), (v and 0xff).toInt())
    } else {
        val v = hex.toLong(16)
        Rgba(
            ((v shr 24) and 0xff).toInt(),
            ((v shr 16) and 0xff).toInt(),
            ((v shr 8) and 0xff).toInt(),
            (v and 0xff).toInt()
        )
    }
    return (i + n) to Value.Hex(rgba)
}

private fun applyCommands(
    cmds: List<ParsedCommand>,
    style: Style,
    diagnostics: MutableList<Diagnostic>,
    index: Int
) {
    for (cmd in cmds) {
        when (cmd) {
            ParsedCommand.CloseAll -> {
                style.size = null; style.emoteSize = null; style.color = null
                style.background = null; style.bold = false; style.italic = false
                style.underline = false; style.strike = false; style.font = null
                style.align = null; style.heading = null; style.hr = false
            }
            is ParsedCommand.Close -> closeSlot(style, cmd.name)
            is ParsedCommand.Set -> {
                if (cmd.name.isEmpty()) continue
                if (cmd.name !in KNOWN_NAMES) {
                    diagnostics += Diagnostic("unknown BFMD command {${cmd.name}}", index)
                    continue
                }
                if (!applySet(style, cmd.name, cmd.value)) {
                    diagnostics += Diagnostic("bad BFMD value for {${cmd.name}}", index)
                }
            }
        }
    }
}

private fun closeSlot(style: Style, name: String) {
    when (name) {
        "s" -> style.size = null
        "e" -> style.emoteSize = null
        "c" -> style.color = null
        "bg" -> style.background = null
        "b" -> style.bold = false
        "i" -> style.italic = false
        "u" -> style.underline = false
        "k" -> style.strike = false
        "f" -> style.font = null
        "a" -> style.align = null
        "h" -> style.heading = null
        "hr" -> style.hr = false
    }
}

private fun applySet(style: Style, name: String, value: Value?): Boolean {
    return when (name) {
        "v" -> true
        "s" -> {
            val n = (value as? Value.Number)?.n ?: return false
            if (n <= 0f) return false
            style.size = n; true
        }
        "e" -> {
            val n = (value as? Value.Number)?.n ?: return false
            if (n <= 0f) return false
            style.emoteSize = n.coerceIn(EMOTE_PX_MIN, EMOTE_PX_MAX); true
        }
        "c" -> {
            style.color = (value as? Value.Hex)?.c ?: return false; true
        }
        "bg" -> {
            style.background = (value as? Value.Hex)?.c ?: return false; true
        }
        "b", "i", "u", "k", "hr" -> {
            val on = when (value) {
                null -> true
                is Value.Bool -> value.b
                is Value.Number -> value.n != 0f
                is Value.Ident -> parseBool(value.s) ?: true
                else -> return false
            }
            when (name) {
                "b" -> style.bold = on
                "i" -> style.italic = on
                "u" -> style.underline = on
                "k" -> style.strike = on
                "hr" -> style.hr = on
            }
            true
        }
        "f" -> {
            style.font = when (value) {
                is Value.Ident -> value.s
                is Value.Quoted -> value.s
                else -> return false
            }
            true
        }
        "a" -> {
            val s = (value as? Value.Ident)?.s ?: return false
            style.align = when (s) {
                "l" -> Align.Left
                "c" -> Align.Center
                "r" -> Align.Right
                "j" -> Align.Justify
                else -> return false
            }
            true
        }
        "h" -> {
            val level = when (value) {
                is Value.Number -> value.n.toInt()
                is Value.Ident -> value.s.toIntOrNull() ?: return false
                else -> return false
            }
            if (level !in 1..6) return false
            style.heading = level
            true
        }
        else -> false
    }
}
