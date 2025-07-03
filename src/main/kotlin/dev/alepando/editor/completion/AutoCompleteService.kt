package dev.alepando.editor.completion

class AutoCompleteService {

    private val scardKeywords = listOf(
        "id", "rarity", "npcName", "npcSpeed", "npcPassing", "npcShooting"
    )

    val rarityValues = listOf(
        "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"
    )

    /**
     * Analyzes the current line text and caret position to provide auto-completion suggestions.
     *
     * @param lineText The text of the current line where the caret is.
     * @param caretInLine The position of the caret within that line.
     * @return A list of suggestion strings. Empty if no relevant suggestions are found.
     */
    fun getSuggestions(lineText: String, caretInLine: Int): List<String> {
        if (caretInLine < 0 || caretInLine > lineText.length) {
            return emptyList()
        }

        val textBeforeCaret = lineText.substring(0, caretInLine)


        val rarityValueRegex = """^\s*rarity\s*=\s*"([^"]*)?$""".toRegex()
        val rarityMatch = rarityValueRegex.find(textBeforeCaret.trimEnd())
        val regex = Regex("^rarity\\s*=\\s*", RegexOption.IGNORE_CASE)
        
        if (rarityMatch != null) {
            val assignmentPart = textBeforeCaret.substringBeforeLast('"', "").trimStart()
            if (regex.containsMatchIn(assignmentPart)) {
                val currentTypedValue = textBeforeCaret.substringAfterLast('"')
                return rarityValues.filter { it.startsWith(currentTypedValue, ignoreCase = true) }
            }
        }


        val wordStartPos = textBeforeCaret.lastIndexOfAny(charArrayOf(' ', '=', '\t')).let { if (it == -1) 0 else it + 1 }
        val currentWord = textBeforeCaret.substring(wordStartPos)

        if (currentWord.isNotBlank()) {
            return scardKeywords.filter { it.startsWith(currentWord, ignoreCase = true) }
        } else {
            if (textBeforeCaret.trim().isEmpty() || textBeforeCaret.trimEnd().endsWith("=")) {
                return scardKeywords
            }
        }
        
        return emptyList()
    }
}
