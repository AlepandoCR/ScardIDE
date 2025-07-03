package dev.alepando

import dev.alepando.editor.ScardEditor
import javafx.application.Application


fun main(args: Array<String>) {
    ScardEditor.openedFilePath = args.firstOrNull() // <-- Guardamos el archivo recibido
    Application.launch(ScardEditor::class.java)
}