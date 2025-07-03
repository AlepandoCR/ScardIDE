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
import org.fxmisc.richtext.model.StyleSpansBuilder

class ScardEditor : Application() {

    private val statusLabel = Label()

    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea()
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)

        // Establecer resaltado aquí o en otra clase
        ScardSyntaxHighlighter.applyHighlighting(codeArea)


        val scrollPane = VirtualizedScrollPane(codeArea)

        val menuBar = MenuBar()
        val fileMenu = Menu("Archivo")

        val openItem = MenuItem("Abrir .scard")
        val saveItem = MenuItem("Guardar")

        openFiles(openItem, primaryStage, codeArea)

        saveFile(saveItem, primaryStage, codeArea)

        fileMenu.items.addAll(openItem, saveItem)
        menuBar.menus.add(fileMenu)

        val root = BorderPane()
        root.top = menuBar
        root.center = scrollPane

        val scene = Scene(root, 800.0, 600.0)

        // Cargamos css si usás
        scene.stylesheets.add(this::class.java.getResource("/styles.css")!!.toExternalForm())

        primaryStage.title = "Scard IDE"
        primaryStage.scene = scene
        primaryStage.show()

        validationThreat(codeArea)
    }

    private fun validationThreat(codeArea: CodeArea) {
        var validationThread: Thread? = null
        codeArea.textProperty().addListener { _, _, newText ->
            validationThread?.interrupt()
            validationThread = Thread {
                try {
                    Thread.sleep(300)
                    Platform.runLater {
                        val errors = ScardValidator.validate(newText)
                        if (errors.isEmpty()) {
                            statusLabel.text = "Archivo válido"
                            statusLabel.textFill = Color.GREEN
                        } else {
                            val msgs =
                                errors.joinToString("; ") { if (it.line >= 0) "Linea ${it.line + 1}: ${it.message}" else it.message }
                            statusLabel.text = "Errores: $msgs"
                            statusLabel.textFill = Color.RED
                        }

                        ScardValidator.highlightErrors(codeArea, errors)
                    }
                } catch (_: InterruptedException) {
                }
            }
            validationThread?.start()
        }
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
            }
        }
    }
}
