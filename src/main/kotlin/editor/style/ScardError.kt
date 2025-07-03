package editor.style

import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpansBuilder

data class ScardError(val line: Int, val message: String)

object ScardValidator {

    fun validate(text: String): List<ScardError> {
        val errors = mutableListOf<ScardError>()
        val lines = text.lines()

        var hasId = false

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isBlank()) continue

            // Check for 'id' presence
            if (trimmed.startsWith("id")) {
                hasId = true
            }

            // Validate numeric fields
            if (trimmed.startsWith("npcSpeed")) {
                val parts = trimmed.split("=")
                if (parts.size == 2) {
                    val value = parts[1].trim()
                    val numStr = value.removeSuffix("f")
                    if (numStr.toDoubleOrNull() == null) {
                        errors.add(ScardError(index, "'npcSpeed' no es un número válido"))
                    }
                }
            }

            // Similar checks para npcPassing, npcShooting
            if (trimmed.startsWith("npcPassing") || trimmed.startsWith("npcShooting")) {
                val parts = trimmed.split("=")
                if (parts.size == 2) {
                    val value = parts[1].trim()
                    if (value.toIntOrNull() == null) {
                        errors.add(ScardError(index, "'${trimmed.substringBefore('=')}' no es un número entero válido"))
                    }
                }
            }
        }

        if (!hasId) {
            errors.add(ScardError(-1, "Falta el campo obligatorio 'id'"))
        }

        return errors
    }

    fun highlightErrors(codeArea: CodeArea, errors: List<ScardError>) {
        val errorLines = errors.mapNotNull { if (it.line >= 0) it.line else null }.toSet()
        val text = codeArea.text
        val lines = text.lines()
        val totalLength = text.length

        if (totalLength == 0) {

            codeArea.setStyleSpans(0, StyleSpansBuilder<Collection<String>>().create())
            return
        }

        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var currentPos = 0

        for ((i, line) in lines.withIndex()) {
            val isErrorLine = i in errorLines
            val lineLength = line.length
            val lengthWithNewLine = if (currentPos + lineLength < totalLength) lineLength + 1 else lineLength

            val style = if (isErrorLine) listOf("error") else emptyList()
            spansBuilder.add(style, lengthWithNewLine)

            currentPos += lengthWithNewLine
        }

        codeArea.setStyleSpans(0, spansBuilder.create())
    }


}
