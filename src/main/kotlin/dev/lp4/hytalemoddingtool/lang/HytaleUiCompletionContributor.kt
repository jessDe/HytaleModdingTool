package dev.lp4.hytalemoddingtool.lang

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class HytaleUiCompletionContributor : CompletionContributor() {
    init {
        // Basic completion for component types
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val componentTypes = listOf(
                        "Group", "Label", "TextButton", "CheckBox", "CheckBoxWithLabel",
                        "TextField", "NumberField", "Image", "ScrollGroup", "VBox", "HBox"
                    )
                    componentTypes.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }

                    val commonProperties = listOf(
                        "Anchor", "Background", "Padding", "Margin", "LayoutMode", "FlexWeight",
                        "Text", "Style", "HorizontalAlignment", "VerticalAlignment", "FontSize",
                        "TextColor", "RenderBold", "RenderUppercase", "LetterSpacing", "Visible",
                        "Enabled", "Id", "Tooltip", "OnPress", "OnValueChange"
                    )
                    commonProperties.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                    
                    val commonValues = listOf("true", "false", "Center", "Left", "Right", "Top", "Bottom")
                    commonValues.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
        )
    }
}
