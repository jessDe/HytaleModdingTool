package dev.lp4.hytalemoddingtool.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.impl.source.tree.LeafPsiElement

class HytaleUiParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(HytaleUiLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?): Lexer = HytaleUiLexer()

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = HytaleUiLexer.WHITE_SPACES

    override fun getCommentTokens(): TokenSet = HytaleUiLexer.COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode?): PsiElement = LeafPsiElement(node?.elementType!!, node.text)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = HytaleUiPsiFile(viewProvider)
}

class HytaleUiPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, HytaleUiLanguage.INSTANCE) {
    override fun getFileType(): com.intellij.openapi.fileTypes.FileType = HytaleUiFileType.INSTANCE
}
