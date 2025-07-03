package dev.alepando.editor.filetree

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextInputDialog
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseButton
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ProjectView(private val onFileSelected: (Path) -> Unit) {

    val view = TreeView<Path>()
    private var currentDirectoryItem: TreeItem<Path>? = null

    init {
        view.setCellFactory { _ ->
            val cell = object : TreeCell<Path>() {
                override fun updateItem(item: Path?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) {
                        null
                    } else {
                        item.name
                    }
                }
            }

            val contextMenu = ContextMenu()
            val newScardFileItem = MenuItem("New .scard file")
            newScardFileItem.setOnAction {
                val selectedPath = view.selectionModel.selectedItem?.value
                if (selectedPath != null && selectedPath.isDirectory()) {
                    val dialog = TextInputDialog()
                    dialog.title = "New SCARD File"
                    dialog.headerText = "Enter name for the new .scard file (without extension):"
                    dialog.contentText = "Name:"
                    dialog.showAndWait().ifPresent { name ->
                        if (name.isNotBlank()) {
                            val newFilePath = selectedPath.resolve("$name.scard")
                            try {
                                Files.writeString(newFilePath, "", StandardOpenOption.CREATE_NEW)
                                val selectedTreeItem = view.selectionModel.selectedItem
                                selectedTreeItem?.let { parentItem ->
                                    parentItem.children.clear()
                                    populateTree(selectedPath, parentItem)
                                    parentItem.isExpanded = true
                                }
                            } catch (e: Exception) {
                                println("Error creating file: ${e.message}")
                            }
                        }
                    }
                }
            }
            contextMenu.items.add(newScardFileItem)

            cell.contextMenu = contextMenu
            cell.setOnContextMenuRequested {
                val treeItem = cell.treeItem
                if (treeItem?.value == null || !treeItem.value.isDirectory()) {
                    contextMenu.hide()
                } else {
                    view.selectionModel.select(treeItem)
                }
            }
            cell
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
