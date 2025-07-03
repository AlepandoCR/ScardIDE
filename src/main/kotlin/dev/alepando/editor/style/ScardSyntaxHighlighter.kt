package dev.alepando.editor.style

import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

object ScardSyntaxHighlighter {

    private val KEYWORDS = listOf(
        "id", "rarity", "npcName", "npcSpeed", "npcPassing", "npcShooting"
    )

    private val PATTERN = Pattern.compile(
        "(?<COMMENT>#.*)|" +
                "(?<KEYWORD>\\b(${KEYWORDS.joinToString("|")})\\b)|" +
                "(?<STRING>\"[^\"]*\")|" +
                "(?<NUMBER>\\b\\d+(\\.\\d+)?f?\\b)"
    )

    fun computeStyles(text: String, errors: List<ScardError>): StyleSpans<Collection<String>> {
        if (text.isEmpty()) {
            return StyleSpansBuilder<Collection<String>>().add(emptyList(), 0).create()
        }

        val finalSpansBuilder = StyleSpansBuilder<Collection<String>>()
        val errorLineIndices = errors.mapNotNull { if (it.line >= 0) it.line else null }.toSet()
        val lines = text.lines()
        var currentPos = 0

        for (lineIndex in lines.indices) {
            val lineText = lines[lineIndex]
            val lineEndPos = currentPos + lineText.length

            val baseLineStyles = mutableListOf<String>()
            if (lineIndex in errorLineIndices) {
                baseLineStyles.add("error")
            }

            val lineMatcher = PATTERN.matcher(lineText)
            var lastMatchEndInLine = 0
            while (lineMatcher.find()) {
                if (lineMatcher.start() > lastMatchEndInLine) {
                    finalSpansBuilder.add(ArrayList(baseLineStyles), lineMatcher.start() - lastMatchEndInLine)
                }
                val syntaxStyle = when {
                    lineMatcher.group("COMMENT") != null -> "comment"
                    lineMatcher.group("KEYWORD") != null -> "keyword"
                    lineMatcher.group("STRING") != null -> "string"
                    lineMatcher.group("NUMBER") != null -> "number"
                    else -> null
                }
                val combinedStyles = ArrayList(baseLineStyles)
                if (syntaxStyle != null) {
                    combinedStyles.add(syntaxStyle)
                }
                finalSpansBuilder.add(combinedStyles, lineMatcher.end() - lineMatcher.start())
                lastMatchEndInLine = lineMatcher.end()
            }

            if (lineText.length > lastMatchEndInLine) {
                finalSpansBuilder.add(ArrayList(baseLineStyles), lineText.length - lastMatchEndInLine)
            }

            currentPos = lineEndPos
            if (lineIndex < lines.size - 1) {
                finalSpansBuilder.add(ArrayList(baseLineStyles), 1)
                currentPos++
            }
        }


        return finalSpansBuilder.create()
    }
}
