package dev.lp4.hytalemoddingtool.lang

import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import java.awt.Color

class HytaleUiColorProvider : ElementColorProvider {
    override fun getColorFrom(element: PsiElement): Color? {
        if (element is LeafPsiElement && element.elementType == HytaleUiLexer.HASH) {
            val text = element.text
            return parseHexColor(text)
        }
        return null
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        if (element is LeafPsiElement && element.elementType == HytaleUiLexer.HASH) {
            val newHex = String.format("#%02X%02X%02X", color.red, color.green, color.blue)
            val newNode = LeafPsiElement(HytaleUiLexer.HASH, newHex)
            element.replace(newNode)
        }
    }

    private fun parseHexColor(hex: String): Color? {
        return try {
            val cleanHex = hex.removePrefix("#")
            when (cleanHex.length) {
                3 -> {
                    val r = cleanHex.substring(0, 1).repeat(2).toInt(16)
                    val g = cleanHex.substring(1, 2).repeat(2).toInt(16)
                    val b = cleanHex.substring(2, 3).repeat(2).toInt(16)
                    Color(r, g, b)
                }
                6 -> {
                    val r = cleanHex.substring(0, 2).toInt(16)
                    val g = cleanHex.substring(2, 4).toInt(16)
                    val b = cleanHex.substring(4, 6).toInt(16)
                    Color(r, g, b)
                }
                8 -> {
                    // ARGB or RGBA? Hytale UI colors seem to use alpha as a separate parameter sometimes, 
                    // but if it's 8 chars, let's assume RRGGBBAA or AARRGGBB.
                    // Based on HytaleUiParser.kt: readColor() just reads letters and digits after #.
                    // Let's support 8-digit hex just in case.
                    val r = cleanHex.substring(0, 2).toInt(16)
                    val g = cleanHex.substring(2, 4).toInt(16)
                    val b = cleanHex.substring(4, 6).toInt(16)
                    val a = cleanHex.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
