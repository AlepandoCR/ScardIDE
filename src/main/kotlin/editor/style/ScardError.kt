package editor.style


data class ScardError(val line: Int, val message: String)

object ScardValidator {

    private val VALID_RARITIES = setOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY")
    private val EXPECTED_FIELDS = setOf("id", "rarity", "npcName", "npcSpeed", "npcPassing", "npcShooting")

    fun validate(text: String): List<ScardError> {
        val errors = mutableListOf<ScardError>()
        val lines = text.lines()
        val definedFields = mutableSetOf<String>()

        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()

            if (trimmedLine.startsWith("#") || trimmedLine.isBlank()) {
                continue // Skip comments and blank lines
            }

            val parts = trimmedLine.split("=", limit = 2)
            if (parts.size != 2) {
                errors.add(ScardError(index, "Formato incorrecto. Se esperaba 'clave = valor'."))
                continue
            }

            val key = parts[0].trim()
            val value = parts[1].trim()

            if (key.isEmpty()) {
                errors.add(ScardError(index, "La clave no puede estar vacía."))
                continue
            }
            if (value.isEmpty()) {
                errors.add(ScardError(index, "El valor para '$key' no puede estar vacío."))
                continue
            }

            if (!EXPECTED_FIELDS.contains(key)) {
                errors.add(ScardError(index, "Clave desconocida: '$key'."))
            } else {
                definedFields.add(key)
            }

            when (key) {
                "id", "npcName" -> {
                    if (!value.startsWith("\"") || !value.endsWith("\"")) {
                        errors.add(ScardError(index, "El valor para '$key' debe estar entre comillas dobles."))
                    } else if (value.length < 2) {
                        errors.add(ScardError(index, "El valor para '$key' entre comillas no puede estar vacío."))
                    }
                }
                "rarity" -> {
                    val rarityValue = if (value.startsWith("\"") && value.endsWith("\"")) {
                        value.substring(1, value.length - 1)
                    } else {
                        value // Allow unquoted for now, or enforce quotes
                    }
                    if (!VALID_RARITIES.contains(rarityValue.uppercase())) {
                        errors.add(ScardError(index, "Valor de 'rarity' inválido: '$rarityValue'. Valores permitidos: ${VALID_RARITIES.joinToString()}."))
                    }
                    if (!value.startsWith("\"") || !value.endsWith("\"")) { // Enforce quotes for rarity as well for consistency
                        errors.add(ScardError(index, "El valor para 'rarity' debe estar entre comillas dobles (ej: \"COMMON\")."))
                    }
                }
                "npcSpeed" -> {
                    if (!value.endsWith("f")) {
                        errors.add(ScardError(index, "El valor para 'npcSpeed' debe terminar con 'f' (ej: 1.2f)."))
                    } else {
                        val numStr = value.removeSuffix("f")
                        if (numStr.toFloatOrNull() == null) {
                            errors.add(ScardError(index, "El valor para 'npcSpeed' ('$numStr') no es un número de punto flotante válido."))
                        }
                    }
                }
                "npcPassing", "npcShooting" -> {
                    if (value.toIntOrNull() == null) {
                        errors.add(ScardError(index, "El valor para '$key' ('$value') no es un número entero válido."))
                    }
                }
            }
        }

        if (!definedFields.contains("id")) {
            errors.add(ScardError(-1, "Falta el campo obligatorio 'id'. Todo .scard debe tener un 'id'."))
        }

        return errors
    }
}
