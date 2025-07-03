package dev.alepando.editor.filetree

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseButton
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ProjectView(private val onFileSelected: (Path) -> Unit) {

    val view = TreeView<Path>()

    init {
        view.setCellFactory { _ ->
            object : javafx.scene.control.TreeCell<Path>() {
                override fun updateItem(item: Path?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) {
                        null
                    } else {
                        item.name
                    }
                }
            }
        }

        view.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                val selectedItem = view.selectionModel.selectedItem
                if (selectedItem?.value != null && !selectedItem.value.isDirectory()) {
                    onFileSelected(selectedItem.value)
                }
            }
        }
    }

    fun loadDirectory(directoryPath: Path) {
        if (!directoryPath.isDirectory()) {
            println("Error: Provided path is not a directory or does not exist: $directoryPath")
            view.root = TreeItem(Path.of("Error: Not a directory"))
            return
        }

        val rootItem = TreeItem(directoryPath)
        rootItem.isExpanded = true
        populateTree(directoryPath, rootItem)
        view.root = rootItem
    }

    private fun populateTree(directory: Path, parentItem: TreeItem<Path>) {
        try {
            Files.list(directory).forEach { path ->
                val item = TreeItem(path)
                parentItem.children.add(item)
                if (path.isDirectory()) {
                    populateTree(path, item)
                }
            }
        } catch (e: Exception) {
            val errorItem = TreeItem(Path.of("Error reading: ${directory.name}"))
            parentItem.children.add(errorItem)
            e.printStackTrace()
        }
    }
}
