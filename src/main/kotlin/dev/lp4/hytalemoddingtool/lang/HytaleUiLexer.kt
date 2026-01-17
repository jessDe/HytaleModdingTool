package dev.lp4.hytalemoddingtool.lang

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerPosition
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class HytaleUiLexer : Lexer() {
    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentOffset: Int = 0
    private var tokenType: IElementType? = null
    private var tokenStart: Int = 0

    companion object {
        val WHITE_SPACE = IElementType("WHITE_SPACE", HytaleUiLanguage.INSTANCE)
        val COMMENT = IElementType("COMMENT", HytaleUiLanguage.INSTANCE)
        val IDENTIFIER = IElementType("IDENTIFIER", HytaleUiLanguage.INSTANCE)
        val STRING = IElementType("STRING", HytaleUiLanguage.INSTANCE)
        val NUMBER = IElementType("NUMBER", HytaleUiLanguage.INSTANCE)
        val LBRACE = IElementType("LBRACE", HytaleUiLanguage.INSTANCE)
        val RBRACE = IElementType("RBRACE", HytaleUiLanguage.INSTANCE)
        val COLON = IElementType("COLON", HytaleUiLanguage.INSTANCE)
        val EQUALS = IElementType("EQUALS", HytaleUiLanguage.INSTANCE)
        val SEMICOLON = IElementType("SEMICOLON", HytaleUiLanguage.INSTANCE)
        val COMMA = IElementType("COMMA", HytaleUiLanguage.INSTANCE)
        val LPAREN = IElementType("LPAREN", HytaleUiLanguage.INSTANCE)
        val RPAREN = IElementType("RPAREN", HytaleUiLanguage.INSTANCE)
        val HASH = IElementType("HASH", HytaleUiLanguage.INSTANCE)
        val TYPE = IElementType("TYPE", HytaleUiLanguage.INSTANCE)
        val ELEMENT = IElementType("ELEMENT", HytaleUiLanguage.INSTANCE)
        val PROPERTY = IElementType("PROPERTY", HytaleUiLanguage.INSTANCE)

        val WHITE_SPACES = TokenSet.create(WHITE_SPACE)
        val COMMENTS = TokenSet.create(COMMENT)
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentOffset = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = currentOffset

    override fun advance() {
        if (currentOffset >= endOffset) {
            tokenType = null
            return
        }

        tokenStart = currentOffset
        val c = buffer[currentOffset]

        when {
            c.isWhitespace() -> {
                while (currentOffset < endOffset && buffer[currentOffset].isWhitespace()) {
                    currentOffset++
                }
                tokenType = WHITE_SPACE
            }
            c == '/' && currentOffset + 1 < endOffset && buffer[currentOffset + 1] == '/' -> {
                while (currentOffset < endOffset && buffer[currentOffset] != '\n') {
                    currentOffset++
                }
                tokenType = COMMENT
            }
            c == '"' -> {
                currentOffset++
                while (currentOffset < endOffset && buffer[currentOffset] != '"') {
                    if (buffer[currentOffset] == '\\' && currentOffset + 1 < endOffset) {
                        currentOffset += 2
                    } else {
                        currentOffset++
                    }
                }
                if (currentOffset < endOffset) currentOffset++
                tokenType = STRING
            }
            c == '{' -> { currentOffset++; tokenType = LBRACE }
            c == '}' -> { currentOffset++; tokenType = RBRACE }
            c == ':' -> { currentOffset++; tokenType = COLON }
            c == '=' -> { currentOffset++; tokenType = EQUALS }
            c == ';' -> { currentOffset++; tokenType = SEMICOLON }
            c == ',' -> { currentOffset++; tokenType = COMMA }
            c == '(' -> { currentOffset++; tokenType = LPAREN }
            c == ')' -> { currentOffset++; tokenType = RPAREN }
            c == '#' -> {
                currentOffset++
                while (currentOffset < endOffset && buffer[currentOffset].isLetterOrDigit()) {
                    currentOffset++
                }
                tokenType = HASH
            }
            c.isLetter() || c == '$' || c == '@' -> {
                while (currentOffset < endOffset && (buffer[currentOffset].isLetterOrDigit() || buffer[currentOffset] == '_' || buffer[currentOffset] == '$' || buffer[currentOffset] == '@' || buffer[currentOffset] == '.' || buffer[currentOffset] == '-')) {
                    currentOffset++
                }
                val text = buffer.substring(tokenStart, currentOffset)
                tokenType = when {
                    text.startsWith("$") -> TYPE
                    text.startsWith("@") -> PROPERTY
                    text in setOf("Group", "Label", "TextButton", "CheckBox", "CheckBoxWithLabel", "TextField", "NumberField", "Image", "ScrollGroup", "VBox", "HBox", "Style", "TextButtonStyle", "LabelStyle") -> ELEMENT
                    buffer.getOrNull(currentOffset) == ':' -> PROPERTY
                    else -> IDENTIFIER
                }
            }
            c.isDigit() || (c == '-' && currentOffset + 1 < endOffset && buffer[currentOffset + 1].isDigit()) -> {
                if (c == '-') currentOffset++
                while (currentOffset < endOffset && (buffer[currentOffset].isDigit() || buffer[currentOffset] == '.')) {
                    currentOffset++
                }
                tokenType = NUMBER
            }
            else -> {
                currentOffset++
                tokenType = IDENTIFIER
            }
        }
    }

    override fun getCurrentPosition(): LexerPosition = object : LexerPosition {
        override fun getOffset(): Int = tokenStart
        override fun getState(): Int = 0
    }

    override fun restore(position: LexerPosition) {
        currentOffset = position.offset
        advance()
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset
}
