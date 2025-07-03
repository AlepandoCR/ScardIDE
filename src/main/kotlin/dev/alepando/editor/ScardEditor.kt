package dev.alepando.editor

import dev.alepando.editor.filetree.ProjectView
import dev.alepando.editor.style.ScardSyntaxHighlighter
import dev.alepando.editor.style.ScardValidator
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ScardEditor : Application() {

    private lateinit var codeArea: CodeArea // Make it a class member
    private lateinit var projectView: ProjectView // Add ProjectView member

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
        codeArea = CodeArea()
        codeArea.styleClass.add("code-area")
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea).apply {
        }


        val scrollPane = VirtualizedScrollPane(codeArea)

        val menuBar = MenuBar()
        menuBar.styleClass.add("menu-bar")
        val fileMenu = Menu("Archivo")

        val openItem = MenuItem("Abrir .scard")
        openItem.accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN)
        val saveItem = MenuItem("Guardar")
        saveItem.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        val saveAsItem = MenuItem("Guardar Como...")
        saveAsItem.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
        val newItem = MenuItem("Nuevo Archivo")
        newItem.accelerator = KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN)
        val loadTemplateItem = MenuItem("Cargar Plantilla")
        val openDirectoryItem = MenuItem("Abrir Carpeta")
        // No common shortcut for load template or open directory, so we'll omit it for now
        val exitItem = MenuItem("Salir")
        exitItem.accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN) // Or KeyCode.F4, KeyCombination.ALT_DOWN


        // ProjectView Initialization
        projectView = ProjectView { filePath ->
            // This lambda is called when a file is selected in ProjectView
            try {
                val content = filePath.toFile().readText()
                codeArea.replaceText(content)
                performValidation(content, codeArea)
                primaryStage.title = "Scard IDE - ${filePath.fileName}"
            } catch (e: Exception) {
                // Handle file reading errors, e.g., show an alert
                e.printStackTrace()
                statusLabel.text = "Error al abrir el archivo: ${filePath.fileName}"
                statusLabel.styleClass.setAll("status-error")
            }
        }

        openFiles(openItem, primaryStage, codeArea)
        saveFile(saveItem, primaryStage, codeArea)
        saveAsFile(saveAsItem, primaryStage, codeArea)
        newFile(newItem, codeArea)
        loadTemplate(loadTemplateItem, codeArea)
        openDirectory(openDirectoryItem, primaryStage, projectView)
        exitApplication(exitItem)


        fileMenu.items.addAll(newItem, openItem, openDirectoryItem, saveItem, saveAsItem, loadTemplateItem, exitItem)
        menuBar.menus.add(fileMenu)

        val root = BorderPane()
        root.styleClass.add("root")
        root.top = menuBar
        root.center = scrollPane
        root.left = projectView.view
        root.bottom = statusLabel

        val scene = Scene(root, 1024.0, 768.0) // Increased default size

        val stylesheet = this::class.java.getResource("/styles.css")
            ?: throw IllegalStateException("styles.css not found")
        scene.stylesheets.add(stylesheet.toExternalForm())

        primaryStage.title = "Scard IDE"
        primaryStage.scene = scene
        primaryStage.show()

        setupLiveValidation(codeArea)
        setupStatusBar(codeArea) // Add this call
        performValidation(codeArea.text, codeArea) // Initial validation and status update
    }

    private fun updateStatusText(codeArea: CodeArea, validationStatus: String, isValid: Boolean?) {
        val currentParagraph = codeArea.currentParagraph
        val caretColumn = codeArea.caretColumn
        val lineText = "Linea: ${currentParagraph + 1}"
        val columnText = "Col: ${caretColumn + 1}"
        val statusText = "$lineText, $columnText | $validationStatus"

        statusLabel.text = statusText
        statusLabel.styleClass.removeAll("status-valid", "status-error")
        isValid?.let {
            if (it) statusLabel.styleClass.add("status-valid") else statusLabel.styleClass.add("status-error")
        }
    }


    private fun performValidation(text: String, codeArea: CodeArea) {
        val errors = ScardValidator.validate(text)
        Platform.runLater {
            val validationStatus: String
            val isValid: Boolean
            if (errors.isEmpty()) {
                validationStatus = "Archivo válido"
                isValid = true
            } else {
                val msgs =
                    errors.joinToString("; ") { error ->
                        if (error.line >= 0) "Linea ${error.line + 1}: ${error.message}"
                        else error.message
                    }
                validationStatus = "Errores: $msgs"
                isValid = false
            }
            updateStatusText(codeArea, validationStatus, isValid)
            val styleSpans = ScardSyntaxHighlighter.computeStyles(text, errors)
            codeArea.setStyleSpans(0, styleSpans)
        }
    }

    private fun setupStatusBar(codeArea: CodeArea) {
        codeArea.caretPositionProperty().addListener { _, _, _ ->
            val currentText = statusLabel.text
            val parts = currentText.split("|", limit = 2)
            val validationMessage = if (parts.size > 1) parts[1].trim() else "Validando..."
            val isValid = if (statusLabel.styleClass.contains("status-valid")) true
            else if (statusLabel.styleClass.contains("status-error")) false
            else null
            updateStatusText(codeArea, validationMessage, isValid)
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

    private fun saveAsFile(
        saveAsItem: MenuItem,
        primaryStage: Stage,
        codeArea: CodeArea
    ) {
        saveAsItem.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("SCARD Files", "*.scard"))
            val file = fileChooser.showSaveDialog(primaryStage)
            file?.writeText(codeArea.text)
        }
    }

    private fun newFile(
        newItem: MenuItem,
        codeArea: CodeArea
    ) {
        newItem.setOnAction {
            codeArea.replaceText("")
            performValidation("", codeArea)
            statusLabel.text = "Nuevo archivo"
            statusLabel.styleClass.removeAll("status-valid", "status-error")
        }
    }

    private fun exitApplication(exitItem: MenuItem) {
        exitItem.setOnAction {
            Platform.exit()
        }
    }

    private fun openDirectory(
        openDirectoryItem: MenuItem,
        primaryStage: Stage,
        projectView: ProjectView
    ) {
        openDirectoryItem.setOnAction {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Seleccionar Carpeta del Proyecto"
            val selectedDirectory: File? = directoryChooser.showDialog(primaryStage)

            selectedDirectory?.let {
                if (it.isDirectory) {
                    projectView.loadDirectory(it.toPath())
                    primaryStage.title = "Scard IDE - ${it.name}"
                } else {
                    statusLabel.text = "La selección no es una carpeta válida."
                    statusLabel.styleClass.setAll("status-error")
                }
            }
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
                primaryStage.title = "Scard IDE - ${it.name}"
                if (projectView.view.root == null || projectView.view.root.children.isEmpty()) {
                    it.parentFile?.toPath()?.let { parentDir -> projectView.loadDirectory(parentDir) }
                }
            }
        }
    }

    private fun loadTemplate(loadTemplateItem: MenuItem, codeArea: CodeArea) {
        val template = """
            # Template for Scard file
            id = "unique_card_id"
            # common, rare, epic, legendary, mythic
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
