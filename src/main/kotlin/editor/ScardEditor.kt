package editor

import editor.style.ScardError
import editor.style.ScardSyntaxHighlighter
import editor.style.ScardValidator
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
// import org.fxmisc.richtext.model.StyleSpansBuilder // No longer directly used here
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ScardEditor : Application() {

    private val statusLabel = Label().apply {
        id = "statusLabel" // For specific CSS styling if needed
    }

    private val validationExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        val thread = Thread(runnable)
        thread.isDaemon = true // Allow JVM to exit even if this thread is running
        thread
    }
    private var scheduledValidationTask: ScheduledFuture<*>? = null
    private val validationDelayMs = 300L // Configurable delay

    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea()
        codeArea.styleClass.add("code-area") // Apply .code-area style
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea).apply {
            // Potentially customize line number format or style here if needed
            // For now, the .lineno class in CSS should target it.
        }


        // Syntax highlighting will now be handled by the validationThreat/text listener
        // ScardSyntaxHighlighter.applyHighlighting(codeArea) // Remove this line


        val scrollPane = VirtualizedScrollPane(codeArea)

        val menuBar = MenuBar()
        menuBar.styleClass.add("menu-bar") // Apply .menu-bar style
        val fileMenu = Menu("Archivo")

        val openItem = MenuItem("Abrir .scard")
        val saveItem = MenuItem("Guardar")

        openFiles(openItem, primaryStage, codeArea)

        saveFile(saveItem, primaryStage, codeArea)

        fileMenu.items.addAll(openItem, saveItem)
        menuBar.menus.add(fileMenu)

        val root = BorderPane()
        root.styleClass.add("root") // Apply .root style
        root.top = menuBar
        root.center = scrollPane
        root.bottom = statusLabel // Add statusLabel to the layout

        val scene = Scene(root, 800.0, 600.0)

        // Cargamos css si usás
        scene.stylesheets.add(this::class.java.getResource("/styles.css")!!.toExternalForm())

        primaryStage.title = "Scard IDE"
        primaryStage.scene = scene
        primaryStage.show()

        setupLiveValidation(codeArea)
        // Perform an initial validation for any existing text when the editor loads
        performValidation(codeArea.text, codeArea)
    }

    private fun performValidation(text: String, codeArea: CodeArea) {
        // This function can be called initially or by the scheduled task
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
            scheduledValidationTask?.cancel(false) // Cancel previous task
            scheduledValidationTask = validationExecutor.schedule({
                // Perform validation and UI update
                // The validation itself (ScardValidator.validate) happens in this scheduled thread.
                // UI updates (setStyleSpans, statusLabel) must be on the JavaFX Application Thread.
                performValidation(newText, codeArea)
            }, validationDelayMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun stop() {
        super.stop()
        validationExecutor.shutdownNow() // Shut down the executor when the app closes
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
                // Immediately validate and style the newly opened file's content
                performValidation(content, codeArea)
            }
        }
    }
}
