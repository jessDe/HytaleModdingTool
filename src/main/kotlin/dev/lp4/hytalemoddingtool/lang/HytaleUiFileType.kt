package dev.lp4.hytalemoddingtool.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class HytaleUiFileType : LanguageFileType(HytaleUiLanguage.INSTANCE) {
    companion object {
        val INSTANCE = HytaleUiFileType()
    }

    override fun getName(): String = "Hytale UI"

    override fun getDescription(): String = "Hytale UI file"

    override fun getDefaultExtension(): String = "ui"

    override fun getIcon(): Icon = AllIcons.FileTypes.UiForm
}
