package dev.lp4.hytalemoddingtool.ui

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jewel.bridge.JewelComposePanel
import kotlinx.coroutines.flow.MutableStateFlow
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class HytaleUiFileEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {

    private val _caretOffset = MutableStateFlow(0)
    val caretOffset: MutableStateFlow<Int> = _caretOffset

    private val panel = JewelComposePanel {
        HytaleUiVisualizer(project, file, caretOffset)
    }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent? = panel

    override fun getName(): String = "Hytale UI Visualizer"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {}

    override fun getFile(): VirtualFile = file
}
