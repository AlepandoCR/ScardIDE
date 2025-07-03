package editor.style

import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

object ScardSyntaxHighlighter {

    // Keywords que queremos destacar
    private val KEYWORDS = listOf(
        "id", "rarity", "npcName", "npcSpeed", "npcPassing", "npcShooting"
    )

    // Regex para los tokens
    private val PATTERN = Pattern.compile(
        "(?<COMMENT>#.*)|" +
                "(?<KEYWORD>\\b(${KEYWORDS.joinToString("|")})\\b)|" +
                "(?<STRING>\"[^\"]*\")|" +
                "(?<NUMBER>\\b\\d+(\\.\\d+)?f?\\b)"
    )

    // This listener will be managed by ScardEditor to coordinate with validation
    fun computeStyles(text: String, errors: List<ScardError>): StyleSpans<Collection<String>> {
        val matcher = PATTERN.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastEnd = 0

        // Apply syntax highlighting
        while (matcher.find()) {
            val styleClass = when {
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("KEYWORD") != null -> "keyword"
                matcher.group("STRING") != null -> "string"
                matcher.group("NUMBER") != null -> "number"
                else -> null // Should not happen with the current regex
            }
            spansBuilder.add(emptyList(), matcher.start() - lastEnd)
            spansBuilder.add(listOfNotNull(styleClass), matcher.end() - matcher.start())
            lastEnd = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastEnd)

        val finalSpansBuilder = StyleSpansBuilder<Collection<String>>()
        var pos = 0
        val lines = text.lines()
        val errorLineIndices = errors.mapNotNull { if (it.line >= 0) it.line else null }.toSet()

        for (i in lines.indices) {
            val lineStart = pos
            val lineEnd = pos + lines[i].length
            val stylesForLine = mutableListOf<String>()

            if (i in errorLineIndices) {
                stylesForLine.add("error")
            }


            val lineMatcher = PATTERN.matcher(lines[i])
            var lineLastEnd = 0
            while(lineMatcher.find()) {
                // Apply styles for segment before match
                if (lineMatcher.start() > lineLastEnd) {
                    finalSpansBuilder.add(ArrayList(stylesForLine), lineMatcher.start() - lineLastEnd)
                }
                // Determine syntax style for the match
                val syntaxStyle = when {
                    lineMatcher.group("COMMENT") != null -> "comment"
                    lineMatcher.group("KEYWORD") != null -> "keyword"
                    lineMatcher.group("STRING") != null -> "string"
                    lineMatcher.group("NUMBER") != null -> "number"
                    else -> null
                }
                val combinedStyles = ArrayList(stylesForLine)
                if (syntaxStyle != null) {
                    combinedStyles.add(syntaxStyle)
                }
                finalSpansBuilder.add(combinedStyles, lineMatcher.end() - lineMatcher.start())
                lineLastEnd = lineMatcher.end()
            }
            // Apply styles for segment after last match in line
            if (lines[i].length > lineLastEnd) {
                finalSpansBuilder.add(ArrayList(stylesForLine), lines[i].length - lineLastEnd)
            }

            pos = lineEnd
            if (pos < text.length) { // Account for newline character
                finalSpansBuilder.add(ArrayList(stylesForLine), 1) // Style for the newline char itself
                pos++
            }
        }
        return finalSpansBuilder.create()
    }
}
