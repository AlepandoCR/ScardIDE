module ScardIDE.main {
    requires kotlin.stdlib;
    requires org.fxmisc.richtext;
    requires javafx.controls;
    requires org.fxmisc.flowless;

    exports dev.alepando;
    exports dev.alepando.editor;
    exports dev.alepando.editor.filetree;
    exports dev.alepando.editor.completion;
}