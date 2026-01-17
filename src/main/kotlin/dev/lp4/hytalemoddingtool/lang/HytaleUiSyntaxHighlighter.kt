package dev.lp4.hytalemoddingtool.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class HytaleUiSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val TYPE = createTextAttributesKey("HYTALE_UI_TYPE", DefaultLanguageHighlighterColors.KEYWORD)
        val ELEMENT = createTextAttributesKey("HYTALE_UI_ELEMENT", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val PROPERTY = createTextAttributesKey("HYTALE_UI_PROPERTY", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val STRING = createTextAttributesKey("HYTALE_UI_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = createTextAttributesKey("HYTALE_UI_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT = createTextAttributesKey("HYTALE_UI_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BRACES = createTextAttributesKey("HYTALE_UI_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val COLON = createTextAttributesKey("HYTALE_UI_COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val ID = createTextAttributesKey("HYTALE_UI_ID", DefaultLanguageHighlighterColors.METADATA)
        val BAD_CHARACTER = createTextAttributesKey("HYTALE_UI_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val TYPE_KEYS = arrayOf(TYPE)
        private val ELEMENT_KEYS = arrayOf(ELEMENT)
        private val PROPERTY_KEYS = arrayOf(PROPERTY)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BRACES_KEYS = arrayOf(BRACES)
        private val COLON_KEYS = arrayOf(COLON)
        private val ID_KEYS = arrayOf(ID)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = HytaleUiLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            HytaleUiLexer.TYPE -> TYPE_KEYS
            HytaleUiLexer.ELEMENT -> ELEMENT_KEYS
            HytaleUiLexer.PROPERTY -> PROPERTY_KEYS
            HytaleUiLexer.STRING -> STRING_KEYS
            HytaleUiLexer.NUMBER -> NUMBER_KEYS
            HytaleUiLexer.COMMENT -> COMMENT_KEYS
            HytaleUiLexer.LBRACE, HytaleUiLexer.RBRACE -> BRACES_KEYS
            HytaleUiLexer.COLON, HytaleUiLexer.EQUALS -> COLON_KEYS
            HytaleUiLexer.HASH -> ID_KEYS
            else -> EMPTY_KEYS
        }
    }
}
