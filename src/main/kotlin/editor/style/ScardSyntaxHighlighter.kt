package editor.style

import org.fxmisc.richtext.CodeArea
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

    fun applyHighlighting(codeArea: CodeArea) {
        codeArea.textProperty().addListener { _, _, _ ->
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.text))
        }
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher = PATTERN.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastEnd = 0

        while (matcher.find()) {
            val styleClass = when {
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("KEYWORD") != null -> "keyword"
                matcher.group("STRING") != null -> "string"
                matcher.group("NUMBER") != null -> "number"
                else -> null
            }

            spansBuilder.add(emptyList(), matcher.start() - lastEnd)
            spansBuilder.add(listOfNotNull(styleClass), matcher.end() - matcher.start())
            lastEnd = matcher.end()
        }

        spansBuilder.add(emptyList(), text.length - lastEnd)
        return spansBuilder.create()
    }
}
