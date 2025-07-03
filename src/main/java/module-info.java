module ScardIDE.main {
    requires kotlin.stdlib;
    requires org.fxmisc.richtext;
    requires javafx.controls;
    requires org.fxmisc.flowless;

    exports dev.alepando.editor.style;
    exports dev.alepando.editor.filetree;
    exports dev.alepando.editor.completion;
    exports dev.alepando.editor;
}