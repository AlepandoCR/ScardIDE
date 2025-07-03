# ScardIDE

ScardIDE is an Integrated Development Environment (IDE) tailored for creating and editing `.scard` files. These files are used to define properties for game entities, likely Non-Player Characters (NPCs), based on common template fields such as `id`, `rarity`, `npcName`, `npcSpeed`, `npcPassing`, and `npcShooting`.

## Features

*   **Dedicated `.scard` File Editor**: Focused environment for `.scard` file manipulation.
*   **Syntax Highlighting**: Visual differentiation of syntax elements in `.scard` files for better readability.
*   **Real-time Validation**: Instant feedback on the validity of `.scard` file syntax and structure, with error reporting.
*   **Auto-Completion**: Intelligent suggestions for keywords and values (e.g., rarity levels) to speed up development.
*   **Auto-Save**: Automatically saves your work to prevent data loss.
*   **Project View**: Navigate your project's file structure with an integrated file tree.
*   **File Management**: Standard operations like New File, Open File, Open Folder, Save, and Save As.
*   **Template System**: Start new `.scard` files quickly using a predefined template.
*   **Status Bar**: Displays useful information like cursor position (line, column) and validation status.
*   **Built with JavaFX**: Modern and responsive user interface.

## Getting Started

### Prerequisites

ScardIDE is built using Java and JavaFX. The installer should bundle the necessary runtime.

### Installation

Installers for ScardIDE (e.g., `.msi` for Windows) are available in the **[Releases](https://github.com/AlepandoCR/ScardIDE/releases)** section of this GitHub repository. Download the latest release and run the installer.

You can also open `.scard` files directly with ScardIDE after installation, or launch ScardIDE and use the "Open" or "Open Folder" options.

## What are `.scard` files?

`.scard` files (described as "Scard Script File") are text-based files used by ScardIDE. They appear to define attributes for game elements. An example structure from the default template includes:

```
id = "unique_card_id"
# common, rare, epic, legendary, mythic
rarity = "common"
npcName = "NPC Name"
npcSpeed = 1.0f
npcPassing = 10
npcShooting = 10
# Add other fields as needed
```

## Building from Source (Optional)

This project uses Gradle. To build it from source:
1.  Clone the repository.
2.  Ensure you have a compatible JDK installed (Java 22 as per `build.gradle.kts`).
3.  Navigate to the project root and run `./gradlew build` (or `gradlew.bat build` on Windows).
    The application can be run using `./gradlew run`.

---

Developed by AlepandoCR.
