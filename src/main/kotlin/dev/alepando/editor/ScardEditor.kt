package dev.alepando.editor

import dev.alepando.editor.style.ScardSyntaxHighlighter
import dev.alepando.editor.style.ScardValidator
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ScardEditor : Application() {

    private val statusLabel = Label().apply {
        id = "statusLabel"
    }

    private val validationExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        val thread = Thread(runnable)
        thread.isDaemon = true
        thread
    }
    private var scheduledValidationTask: ScheduledFuture<*>? = null
    private val validationDelayMs = 300L

    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea()
        codeArea.styleClass.add("code-area")
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea).apply {
        }


        val scrollPane = VirtualizedScrollPane(codeArea)

        val menuBar = MenuBar()
        menuBar.styleClass.add("menu-bar")
        val fileMenu = Menu("Archivo")

        val openItem = MenuItem("Abrir .scard")
        val saveItem = MenuItem("Guardar")
        val loadTemplateItem = MenuItem("Cargar Plantilla")

        openFiles(openItem, primaryStage, codeArea)
        saveFile(saveItem, primaryStage, codeArea)
        loadTemplate(loadTemplateItem, codeArea)

        fileMenu.items.addAll(openItem, saveItem, loadTemplateItem)
        menuBar.menus.add(fileMenu)

        val root = BorderPane()
        root.styleClass.add("root")
        root.top = menuBar
        root.center = scrollPane
        root.bottom = statusLabel

        val scene = Scene(root, 800.0, 600.0)

        scene.stylesheets.add(this::class.java.getResource("/styles.css")!!.toExternalForm())

        primaryStage.title = "Scard IDE"
        primaryStage.scene = scene
        primaryStage.show()

        setupLiveValidation(codeArea)
        performValidation(codeArea.text, codeArea)
    }

    private fun performValidation(text: String, codeArea: CodeArea) {
        val errors = ScardValidator.validate(text)
        Platform.runLater {
            statusLabel.styleClass.removeAll("status-valid", "status-error")
            if (errors.isEmpty()) {
                statusLabel.text = "Archivo válido"
                statusLabel.styleClass.add("status-valid")
            } else {
                val msgs =
                    errors.joinToString("; ") { error ->
                        if (error.line >= 0) "Linea ${error.line + 1}: ${error.message}"
                        else error.message
                    }
                statusLabel.text = "Errores: $msgs"
                statusLabel.styleClass.add("status-error")
            }
            val styleSpans = ScardSyntaxHighlighter.computeStyles(text, errors)
            codeArea.setStyleSpans(0, styleSpans)
        }
    }

    private fun setupLiveValidation(codeArea: CodeArea) {
        codeArea.textProperty().addListener { _, _, newText ->
            scheduledValidationTask?.cancel(false)
            scheduledValidationTask = validationExecutor.schedule({
                performValidation(newText, codeArea)
            }, validationDelayMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun stop() {
        super.stop()
        validationExecutor.shutdownNow()
    }

    private fun saveFile(
        saveItem: MenuItem,
        primaryStage: Stage,
        codeArea: CodeArea
    ) {
        saveItem.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("SCARD Files", "*.scard"))
            val file = fileChooser.showSaveDialog(primaryStage)
            file?.writeText(codeArea.text)
        }
    }

    private fun openFiles(
        openItem: MenuItem,
        primaryStage: Stage,
        codeArea: CodeArea
    ) {
        openItem.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("SCARD Files", "*.scard"))
            val file = fileChooser.showOpenDialog(primaryStage)
            file?.let {
                val content = it.readText()
                codeArea.replaceText(content)
                performValidation(content, codeArea)
            }
        }
    }

    private fun loadTemplate(loadTemplateItem: MenuItem, codeArea: CodeArea) {
        val template = """
            # Template for Scard file
            id = "unique_card_id"
            # common, rare, epic, legendary
            rarity = "common" 
            npcName = "NPC Name"
            npcSpeed = 1.0f
            npcPassing = 10
            npcShooting = 10
            # Add other fields as needed
            """.trimIndent()

        loadTemplateItem.setOnAction {
            codeArea.replaceText(template)
            performValidation(template, codeArea)
        }
    }
}
