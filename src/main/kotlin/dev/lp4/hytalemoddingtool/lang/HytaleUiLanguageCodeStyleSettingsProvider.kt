package dev.lp4.hytalemoddingtool.lang

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class HytaleUiLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = HytaleUiLanguage.INSTANCE

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun getDefaultCommonSettings(): CommonCodeStyleSettings {
        val defaultSettings = CommonCodeStyleSettings(language)
        val indentOptions = defaultSettings.initIndentOptions()
        indentOptions.INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 2
        return defaultSettings
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showAllStandardOptions()
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String? {
        return """
            Group {
              Padding: (Full: 16)
              Label {
                Text: "Hello Hytale"
                Style: LabelStyle(
                  FontSize: 24
                )
              }
            }
        """.trimIndent()
    }
}
