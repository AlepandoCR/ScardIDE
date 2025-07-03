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
import dev.alepando.editor.completion.AutoCompleteService
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ContextMenu
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.isDirectory

class ScardEditor : Application() {

    private lateinit var codeArea: CodeArea // Make it a class member
    private lateinit var projectView: ProjectView // Add ProjectView member
    private var currentFile: File? = null

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

    private val autoSaveExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        val thread = Thread(runnable)
        thread.isDaemon = true
        thread
    }
    private var scheduledAutoSaveTask: ScheduledFuture<*>? = null
    private val autoSaveDelayMs = 1000L // 1 second for auto-save

    private lateinit var autoCompleteService: AutoCompleteService
    private lateinit var suggestionsPopup: ContextMenu

    override fun start(primaryStage: Stage) {
        codeArea = CodeArea()
        autoCompleteService = AutoCompleteService()
        suggestionsPopup = ContextMenu()
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
        val exitItem = MenuItem("Salir")
        exitItem.accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)


        projectView = ProjectView { filePath ->
            // Attempt to save the currently open file before switching
            currentFile?.let { fileToSaveBeforeSwitching ->
                // Check if codeArea has text and the file exists.
                // A more sophisticated "dirty" check could be implemented,
                // but a direct write is simpler given auto-save.
                if (codeArea.text.isNotEmpty() && fileToSaveBeforeSwitching.exists()) {
                    try {
                        fileToSaveBeforeSwitching.writeText(codeArea.text)
                        // Optionally, update status briefly, though it might be too quick to notice
                        // statusLabel.text = "Auto-saved ${fileToSaveBeforeSwitching.name} before switch."
                    } catch (e: Exception) {
                        System.err.println("Error saving ${fileToSaveBeforeSwitching.name} before switching: ${e.message}")
                        // Decide if you want to inform the user via statusLabel
                        // statusLabel.text = "Error saving current file: ${e.message}"
                        // statusLabel.styleClass.setAll("status-error")
                    }
                }
            }

            // Proceed to load the new file
            try {
                val file = filePath.toFile()
                val content = file.readText()
                codeArea.replaceText(content)
                currentFile = file // Update currentFile
                performValidation(content, codeArea)
                primaryStage.title = "Scard IDE - ${file.name}"
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
        newFile(newItem, primaryStage, codeArea, projectView)
        loadTemplate(loadTemplateItem, codeArea)
        openDirectory(openDirectoryItem, primaryStage, projectView)
        exitApplication(exitItem)


        fileMenu.items.addAll(newItem, openItem, openDirectoryItem, saveItem, saveAsItem, loadTemplateItem, exitItem)

        val plantillaMenu = Menu("Plantilla")

        val cargarPlantillaFromPlantillaMenu = MenuItem("Cargar Plantilla")
        loadTemplate(cargarPlantillaFromPlantillaMenu, codeArea)
        plantillaMenu.items.add(cargarPlantillaFromPlantillaMenu)
        cargarPlantillaFromPlantillaMenu.accelerator = KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN)


        menuBar.menus.add(fileMenu)
        menuBar.menus.add(plantillaMenu)

        val root = BorderPane()
        root.styleClass.add("root")
        root.top = menuBar
        root.center = scrollPane
        root.left = projectView.view
        root.bottom = statusLabel

        val scene = Scene(root, 1024.0, 768.0)

        val stylesheet = this::class.java.getResource("/styles.css")
            ?: throw IllegalStateException("styles.css not found")
        scene.stylesheets.add(stylesheet.toExternalForm())

        primaryStage.title = "Scard IDE - Sin título" // Default title for new/empty editor
        currentFile = null // Explicitly null for a new session without a file
        primaryStage.scene = scene
        primaryStage.show()

        setupLiveValidation(codeArea)
        setupStatusBar(codeArea)
        setupAutoCompletion(codeArea)
        performValidation(codeArea.text, codeArea)

        openFromFile(primaryStage)
    }

    private fun openFromFile(primaryStage: Stage) {
        openedFilePath?.let { pathStr ->
            val file = File(pathStr)
            if (file.exists() && file.extension == "scard") {
                val content = file.readText()
                codeArea.replaceText(content)
                currentFile = file // Update currentFile
                performValidation(content, codeArea)
                primaryStage.title = "Scard IDE - ${file.name}"

                if (projectView.view.root == null || projectView.view.root.children.isEmpty()) {
                    file.parentFile?.toPath()?.let { parentDir ->
                        projectView.loadDirectory(parentDir)
                    }
                }
            }
        }
    }

    private fun setupAutoCompletion(codeArea: CodeArea) {
        codeArea.textProperty().addListener { _, _, newText ->
            handleAutoCompletion(newText)
        }
        codeArea.caretPositionProperty().addListener { _, _, _ ->
            handleAutoCompletion(codeArea.text)
        }
    }

    private fun handleAutoCompletion(text: String) {
        if (text.isEmpty()) {
            suggestionsPopup.hide()
            return
        }

        val currentParagraph = codeArea.currentParagraph
        val lineText = codeArea.getParagraph(currentParagraph).text
        val caretInLine = codeArea.caretColumn

        val suggestions = autoCompleteService.getSuggestions(lineText, caretInLine)

        if (suggestions.isNotEmpty()) {
            suggestionsPopup.items.clear()
            suggestions.forEach { suggestion ->
                val menuItem = MenuItem(suggestion)
                menuItem.setOnAction {
                    val currentParagraphIndex = codeArea.currentParagraph
                    val lineStartGlobal = codeArea.getAbsolutePosition(currentParagraphIndex, 0)
                    val textBeforeCaretInLine = textBeforeCaret(lineText, caretInLine)

                    var textToInsert = suggestion
                    var replacementStartInLine: Int

                    // Check if we are in the context of completing a rarity value
                    val rarityAssignmentRegex = """^\s*rarity\s*=\s*""".toRegex(RegexOption.IGNORE_CASE)
                    val rarityValuePrefixRegex = """(rarity\s*=\s*")([^"]*)""".toRegex(RegexOption.IGNORE_CASE)

                    rarityValuePrefixRegex.find(textBeforeCaretInLine)

                    if (autoCompleteService.rarityValues.contains(suggestion) && // Check if the suggestion is a known rarity
                        (rarityAssignmentRegex.containsMatchIn(textBeforeCaretInLine) || lineText.trimStart()
                            .startsWith("rarity", ignoreCase = true))
                    ) {

                        val currentTextUpToCaret = lineText.substring(0, caretInLine)
                        val equalsIndex = currentTextUpToCaret.lastIndexOf('=')
                        val quoteIndex = currentTextUpToCaret.lastIndexOf('"')

                        replacementStartInLine = if (quoteIndex > equalsIndex) {
                            quoteIndex + 1 // Start replacing after the opening quote
                        } else {
                            // User hasn't typed a quote yet, find where the value should start
                            (equalsIndex + 1).let { idx ->
                                currentTextUpToCaret.substring(idx).indexOfFirst { !it.isWhitespace() } + idx
                            }
                        }

                        textToInsert = "\"$suggestion\""

                        if (quoteIndex == -1 && equalsIndex != -1) {
                            replacementStartInLine = currentTextUpToCaret.indexOf(
                                '=',
                                startIndex = currentTextUpToCaret.toLowerCase().lastIndexOf("rarity")
                            ) + 1
                            while (replacementStartInLine < currentTextUpToCaret.length && currentTextUpToCaret[replacementStartInLine].isWhitespace()) {
                                replacementStartInLine++
                            }
                        } else if (quoteIndex > equalsIndex) {

                        }



                        val lineSuffix = lineText.substring(caretInLine)
                        if (lineSuffix.startsWith("\"")) {
                            // remove our added closing quote if one already exists right after caret
                            textToInsert = "\"$suggestion"
                        }


                    } else {
                        replacementStartInLine = textBeforeCaretInLine.lastIndexOfAny(charArrayOf(' ', '=', '\t'))
                            .let { if (it == -1) 0 else it + 1 }
                    }

                    val replaceStartGlobal = lineStartGlobal + replacementStartInLine
                    val replaceEndGlobal = lineStartGlobal + caretInLine

                    codeArea.replaceText(replaceStartGlobal, replaceEndGlobal, textToInsert)
                    codeArea.moveTo(replaceStartGlobal + textToInsert.length)
                    suggestionsPopup.hide()
                }
                suggestionsPopup.items.add(menuItem)
            }


            val caretBounds = codeArea.caretBounds
            if (caretBounds.isPresent) {
                val bounds = caretBounds.get()
                suggestionsPopup.show(codeArea, bounds.maxX, bounds.maxY)
            } else {
                suggestionsPopup.show(codeArea.scene.window)
            }
        } else {
            suggestionsPopup.hide()
        }
    }

    private fun textBeforeCaret(lineText: String, caretInLine: Int): String {
        return if (caretInLine > 0 && caretInLine <= lineText.length) {
            lineText.substring(0, caretInLine)
        } else {
            ""
        }
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
            if (text != codeArea.text) {
                return@runLater
            }

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

            if (styleSpans.length() == codeArea.length) {
                codeArea.setStyleSpans(0, styleSpans)
            } else {
                System.err.println(
                    "ScardEditor: StyleSpans length (${styleSpans.length()}) " +
                            "mismatches CodeArea length (${codeArea.length}) " +
                            "even after text content check. Text analyzed: \"$text\""
                )
            }
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
            // Debounce validation
            scheduledValidationTask?.cancel(false)
            scheduledValidationTask = validationExecutor.schedule({
                performValidation(newText, codeArea)
            }, validationDelayMs, TimeUnit.MILLISECONDS)

            // Debounce auto-save
            scheduledAutoSaveTask?.cancel(false)
            scheduledAutoSaveTask = autoSaveExecutor.schedule({
                Platform.runLater { // Ensure UI updates are on the JavaFX application thread
                    currentFile?.let { fileToSave ->
                        if (fileToSave.exists() && fileToSave.isFile) {
                            try {
                                fileToSave.writeText(newText)
                                // Update status bar - keep existing validation message if present
                                val currentStatus = statusLabel.text
                                val statusParts = currentStatus.split("|", limit = 2)
                                val validationMsg = if (statusParts.size > 1) statusParts[1].trim() else ""
                                val lineColInfo = if (statusParts.isNotEmpty()) statusParts[0].trim() else "Línea: ?, Col: ?"
                                statusLabel.text = "$lineColInfo | Auto-guardado. $validationMsg"
                                // Temporarily change style, then revert or let validation override
                                statusLabel.styleClass.removeAll("status-error", "status-valid") // Clear previous specific styles
                                // No specific style for auto-save message, let validation/error dictate color
                            } catch (e: Exception) {
                                e.printStackTrace()
                                statusLabel.text = "Error en auto-guardado: ${e.message}"
                                statusLabel.styleClass.setAll("status-error")
                            }
                        }
                    }
                }
            }, autoSaveDelayMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun stop() {
        super.stop()
        validationExecutor.shutdownNow()
        autoSaveExecutor.shutdownNow()
    }

    private fun saveFile(
        saveItem: MenuItem,
        primaryStage: Stage,
        codeArea: CodeArea
    ) {
        saveItem.setOnAction {
            val fileToSave = currentFile
            if (fileToSave != null) {
                try {
                    fileToSave.writeText(codeArea.text)
                    primaryStage.title = "Scard IDE - ${fileToSave.name}" // Ensure title is correct
                    statusLabel.text = "Archivo guardado: ${fileToSave.name}"
                    statusLabel.styleClass.setAll("status-valid")
                } catch (e: Exception) {
                    e.printStackTrace()
                    statusLabel.text = "Error al guardar archivo: ${e.message}"
                    statusLabel.styleClass.setAll("status-error")
                    // Optionally, trigger Save As dialog here as a fallback
                    // showSaveAsDialog(primaryStage, codeArea)
                }
            } else {
                // currentFile is null, so behave like "Save As"
                showSaveAsDialog(primaryStage, codeArea, isSaveAs = false)
            }
        }
    }

    private fun showSaveAsDialog(primaryStage: Stage, codeArea: CodeArea, isSaveAs: Boolean) {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("SCARD Files", "*.scard"))
        fileChooser.title = if (isSaveAs) "Guardar Como..." else "Guardar Archivo"

        // Suggest current file name if available for "Save As"
        currentFile?.let {
            fileChooser.initialFileName = it.name
            // Set initial directory to the parent directory of the current file
            it.parentFile?.let { parentDir ->
                if (parentDir.exists() && parentDir.isDirectory) {
                    fileChooser.initialDirectory = parentDir
                }
            }
        }

        val file = fileChooser.showSaveDialog(primaryStage)
        file?.let {
            try {
                it.writeText(codeArea.text)
                currentFile = it // Update currentFile
                primaryStage.title = "Scard IDE - ${it.name}"
                statusLabel.text = "Archivo guardado como: ${it.name}"
                statusLabel.styleClass.setAll("status-valid")
            } catch (e: Exception) {
                e.printStackTrace()
                statusLabel.text = "Error al guardar archivo: ${e.message}"
                statusLabel.styleClass.setAll("status-error")
            }
        }
    }


    private fun saveAsFile(
        saveAsItemMenuItem: MenuItem, // Renamed parameter to avoid conflict
        primaryStage: Stage,
        codeArea: CodeArea
    ) {
        saveAsItemMenuItem.setOnAction { // Use the renamed parameter
            showSaveAsDialog(primaryStage, codeArea, isSaveAs = true)
        }
    }

    private fun newFile(
        newItem: MenuItem,
        primaryStage: Stage,
        codeArea: CodeArea,
        projectView: ProjectView
    ) {
        newItem.setOnAction {
            val projectRootNode = projectView.view.root
            val projectRootDir = projectRootNode?.value

            if (projectRootDir != null && projectRootDir.isDirectory() && projectRootDir.toString() != "Error: Not a directory") {
                // A directory is open
                val dialog = TextInputDialog()
                dialog.title = "Nuevo Archivo SCARD"
                dialog.headerText = "Introduce el nombre para el nuevo archivo .scard (sin extensión):"
                dialog.contentText = "Nombre:"
                dialog.showAndWait().ifPresent { name ->
                    if (name.isNotBlank()) {
                        val newFileName = "$name.scard"
                        val newFilePath = projectRootDir.resolve(newFileName)
                        try {
                            Files.writeString(newFilePath, "", StandardOpenOption.CREATE_NEW)
                            codeArea.replaceText("")
                            currentFile = newFilePath.toFile()
                            performValidation("", codeArea)
                            statusLabel.text = "Nuevo archivo creado: $newFileName"
                            statusLabel.styleClass.removeAll("status-valid", "status-error")
                            projectView.loadDirectory(projectRootDir)


                            primaryStage.title = "Scard IDE - $newFileName"


                        } catch (e: Exception) {

                            currentFile = null
                            statusLabel.text = "Error al crear archivo: ${e.message}"
                            statusLabel.styleClass.setAll("status-error")
                            e.printStackTrace()
                        }
                    }
                }
            } else {

                codeArea.replaceText("")
                currentFile = null
                performValidation("", codeArea)
                statusLabel.text = "Nuevo archivo (guardar para asignar ubicación)"
                statusLabel.styleClass.removeAll("status-valid", "status-error")
                primaryStage.title = "Scard IDE - Sin título" // Reset title
            }
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
                currentFile = it // Update currentFile
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
            currentFile = null // Template is not saved to a file yet
            (codeArea.scene.window as? Stage)?.title = "Scard IDE - Sin título (Plantilla)"
            performValidation(template, codeArea)
            statusLabel.text = "Plantilla cargada. Guardar para crear archivo."
            statusLabel.styleClass.removeAll("status-valid", "status-error")
        }
    }

    companion object {
        var openedFilePath: String? = null
    }

}
