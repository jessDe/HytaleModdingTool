package dev.lp4.hytalemoddingtool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.intellij.openapi.project.Project
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.intellij.openapi.vfs.VirtualFile
import dev.lp4.hytalemoddingtool.ui.model.HytaleUiComponent
import dev.lp4.hytalemoddingtool.ui.parser.HytaleUiParser
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text

import androidx.compose.runtime.collectAsState
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import dev.lp4.hytalemoddingtool.ui.model.HytaleUiAnchor
import dev.lp4.hytalemoddingtool.ui.model.HytaleUiColor

import dev.lp4.hytalemoddingtool.ui.model.HytaleUiFile
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.border

@Composable
fun HytaleUiVisualizer(project: Project, file: VirtualFile, caretOffsetFlow: StateFlow<Int>? = null) {
    val document = remember(file) { FileDocumentManager.getInstance().getDocument(file) }
    
    val caretOffset by (caretOffsetFlow ?: remember { MutableStateFlow(0) }).collectAsState()
    
    val navigateToCode: (Int) -> Unit = remember(project, file) {
        { offset ->
            OpenFileDescriptor(project, file, offset).navigate(true)
        }
    }

    val contentFlow = remember(document) {
        callbackFlow {
            val listener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    trySend(event.document.text)
                }
            }
            document?.addDocumentListener(listener)
            awaitClose { document?.removeDocumentListener(listener) }
        }.onStart {
            emit(document?.text ?: file.contentsToByteArray().decodeToString())
        }
    }

    var uiFile by remember { mutableStateOf<HytaleUiFile?>(null) }
    var rawContent by remember { mutableStateOf("") }

    LaunchedEffect(contentFlow) {
        contentFlow
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest { content ->
                rawContent = content
                val parsed = withContext(Dispatchers.Default) {
                    try {
                        HytaleUiParser(content).parse()
                    } catch (e: Exception) {
                        null
                    }
                }
                uiFile = parsed
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hytale UI Visualizer",
            color = Color.White,
            fontSize = 20.sp
        )
        Text(
            text = "Previewing: ${file.name}",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(orientation = Orientation.Horizontal, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        uiFile?.rootComponent?.let { root ->
            Text("Layout Preview", color = Color.LightGray)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                LayoutPreview(root, caretOffset, navigateToCode)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Representation of a UI component tree
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Component Tree", color = Color.LightGray)
            
            uiFile?.let { fileModel ->
                fileModel.rootComponent?.let { root ->
                    UiComponentView(root, 0, caretOffset, navigateToCode)
                } ?: Text("No root component found", color = Color.Yellow)
            } ?: if (rawContent.isEmpty()) {
                Text("Empty file", color = Color.Gray)
            } else {
                Text("Parsing...", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Raw Content Snippet", color = Color.Gray, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            Text(
                text = highlightHytaleUi(if (rawContent.length > 1000) rawContent.take(1000) + "..." else rawContent),
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun UiComponentView(component: HytaleUiComponent, depth: Int = 0, caretOffset: Int = 0, onComponentClick: (Int) -> Unit) {
    val isActive = caretOffset in component.startOffset..component.endOffset
    val isDeepestActive = isActive && component.children.none { caretOffset in it.startOffset..it.endOffset }

    Column(
        modifier = Modifier
            .padding(start = if (depth > 0) 12.dp else 0.dp)
            .clickable { onComponentClick(component.startOffset) }
            .background(if (isDeepestActive) Color(0xFF4A9EFF).copy(alpha = 0.2f) else Color.Transparent)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .background(if (component.children.isNotEmpty()) Color(0xFF4A9EFF) else Color(0xFF96A9BE))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(component.type, color = Color(0xFFFFD700), fontSize = 13.sp)
            component.id?.let { id ->
                Spacer(modifier = Modifier.width(4.dp))
                Text("#$id", color = Color(0xFF4FC3F7), fontSize = 13.sp)
            }
        }
        
        // Show properties if any
        if (component.properties.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 18.dp)) {
                component.properties.forEach { (key, value) ->
                    Row {
                        Text("$key: ", color = Color(0xFFA9B7C6), fontSize = 11.sp)
                        Text(formatValue(value), color = Color(0xFF6A8759), fontSize = 11.sp)
                    }
                }
            }
        }

        component.children.forEach { child ->
            UiComponentView(child, depth + 1, caretOffset, onComponentClick)
        }
    }
}

fun formatValue(value: Any?): String {
    return when (value) {
        is HytaleUiAnchor -> "Anchor(W:${value.width}, H:${value.height}, FW:${value.flexWeight})"
        is HytaleUiColor -> "Color(${value.hex}, A:${value.alpha})"
        is Map<*, *> -> "(" + value.entries.joinToString(", ") { "${it.key}: ${formatValue(it.value)}" } + ")"
        is List<*> -> "[" + value.joinToString(", ") { formatValue(it) } + "]"
        else -> value.toString()
    }
}

@Composable
fun LayoutPreview(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            RenderComponent(component, caretOffset, onComponentClick)
        }
    }
}

@Composable
fun RenderLabel(
    text: String,
    style: Map<*, *>?,
    modifier: Modifier
) {
    // Unwrap style if it's wrapped in a constructor map like Style(...) or LabelStyle(...)
    var effectiveStyle = style
    if (effectiveStyle != null && effectiveStyle.size == 1 && !effectiveStyle.containsKey("HorizontalAlignment") && !effectiveStyle.containsKey("@HorizontalAlignment")) {
        val firstValue = effectiveStyle.values.first()
        if (firstValue is Map<*, *>) {
            effectiveStyle = firstValue
        }
    }

    val hAlign = (effectiveStyle?.get("HorizontalAlignment") ?: effectiveStyle?.get("@HorizontalAlignment"))?.toString() ?: "Left"
    val vAlign = (effectiveStyle?.get("VerticalAlignment") ?: effectiveStyle?.get("@VerticalAlignment"))?.toString() ?: "Top"
    val fontSize = (effectiveStyle?.get("FontSize") ?: effectiveStyle?.get("@FontSize"))?.toString()?.toIntOrNull() ?: 12
    val textColorValue = effectiveStyle?.get("TextColor") ?: effectiveStyle?.get("@TextColor")
    val textColor = textColorValue as? HytaleUiColor
    val isBold = (effectiveStyle?.get("RenderBold") ?: effectiveStyle?.get("@RenderBold"))?.toString()?.toBoolean() ?: false
    val isUppercase = (effectiveStyle?.get("RenderUppercase") ?: effectiveStyle?.get("@RenderUppercase"))?.toString()?.toBoolean() ?: false
    val letterSpacing = (effectiveStyle?.get("LetterSpacing") ?: effectiveStyle?.get("@LetterSpacing"))?.toString()?.toFloatOrNull() ?: 0f

    val alignment = when {
        (hAlign == "Center" || hAlign == "Centre") && vAlign == "Center" -> Alignment.Center
        (hAlign == "Center" || hAlign == "Centre") && vAlign == "Top" -> Alignment.TopCenter
        (hAlign == "Center" || hAlign == "Centre") && vAlign == "Bottom" -> Alignment.BottomCenter
        hAlign == "Right" && vAlign == "Center" -> Alignment.CenterEnd
        hAlign == "Right" && vAlign == "Top" -> Alignment.TopEnd
        hAlign == "Right" && vAlign == "Bottom" -> Alignment.BottomEnd
        hAlign == "Left" && vAlign == "Center" -> Alignment.CenterStart
        hAlign == "Left" && vAlign == "Bottom" -> Alignment.BottomStart
        else -> Alignment.TopStart
    }

    val finalTextColor = if (textColor != null) {
        parseHexColor(textColor.hex).copy(alpha = textColor.alpha)
    } else if (textColorValue is Map<*, *>) {
        val hex = textColorValue["hex"]?.toString() ?: textColorValue["Hex"]?.toString()
        val alpha = textColorValue["alpha"]?.toString()?.toFloatOrNull() ?: textColorValue["Alpha"]?.toString()?.toFloatOrNull() ?: 1.0f
        if (hex != null) parseHexColor(hex).copy(alpha = alpha) else Color.White
    } else {
        Color.White
    }

    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        Text(
            text = if (isUppercase) text.uppercase() else text,
            color = finalTextColor,
            fontSize = fontSize.sp,
            textAlign = when (hAlign) {
                "Center", "Centre" -> TextAlign.Center
                "Right" -> TextAlign.End
                else -> TextAlign.Start
            },
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = letterSpacing.sp,
            modifier = Modifier.align(alignment)
        )
    }
}

@Composable
fun ColumnScope.RenderComponent(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit) {
    val isActive = caretOffset in component.startOffset..component.endOffset
    val isDeepestActive = isActive && component.children.none { caretOffset in it.startOffset..it.endOffset }

    val anchor = (component.properties["Anchor"] ?: component.properties["@Anchor"]) as? HytaleUiAnchor
    val flexWeight = (component.properties["FlexWeight"] ?: component.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f
    
    val paddingMap = (component.properties["Padding"] ?: component.properties["@Padding"])
    val paddingFull = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Full"] ?: paddingMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> paddingMap.full ?: 0
        else -> 0
    }
    val paddingTop = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Top"] ?: paddingMap["@Top"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingBottom = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Bottom"] ?: paddingMap["@Bottom"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingLeft = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Left"] ?: paddingMap["@Left"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingRight = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Right"] ?: paddingMap["@Right"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }

    val marginMap = (component.properties["Margin"] ?: component.properties["@Margin"])
    val marginFull = when (marginMap) {
        is Map<*, *> -> (marginMap["Full"] ?: marginMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> marginMap.full ?: 0
        else -> 0
    }
    val marginTop = when (marginMap) {
        is Map<*, *> -> (marginMap["Top"] ?: marginMap["@Top"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginBottom = when (marginMap) {
        is Map<*, *> -> (marginMap["Bottom"] ?: marginMap["@Bottom"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginLeft = when (marginMap) {
        is Map<*, *> -> (marginMap["Left"] ?: marginMap["@Left"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginRight = when (marginMap) {
        is Map<*, *> -> (marginMap["Right"] ?: marginMap["@Right"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }

    val baseModifier = run {
        var m: Modifier = Modifier.clickable { onComponentClick(component.startOffset) }

        if (isDeepestActive) {
            m = m.border(1.dp, Color.Cyan)
        }
        
        if (marginLeft > 0 || marginTop > 0 || marginRight > 0 || marginBottom > 0) {
            m = m.padding(
                start = marginLeft.dp,
                top = marginTop.dp,
                end = marginRight.dp,
                bottom = marginBottom.dp
            )
        }

        if (anchor != null) {
            if (anchor.width != null) {
                m = m.width(anchor.width.dp)
            } else if (flexWeight <= 0f) {
                m = m.fillMaxWidth()
            }
            if (anchor.height != null) m = m.height(anchor.height.dp)
        } else if (flexWeight <= 0f) {
            m = m.fillMaxWidth()
        }
        
        val bgValue = component.properties["Background"] ?: component.properties["@Background"]
        val bg = bgValue as? HytaleUiColor
        if (bg != null) {
            val color = parseHexColor(bg.hex).copy(alpha = bg.alpha)
            m = m.background(color)
        } else if (bgValue is Map<*, *>) {
            val hex = bgValue["hex"]?.toString() ?: bgValue["Hex"]?.toString()
            if (hex != null) {
                val alpha = bgValue["alpha"]?.toString()?.toFloatOrNull() ?: bgValue["Alpha"]?.toString()?.toFloatOrNull() ?: 1.0f
                m = m.background(parseHexColor(hex).copy(alpha = alpha))
            }
        } else if (component.type == "Group" && component.children.isEmpty() && anchor?.width != null && anchor.height != null) {
             m = m.background(Color.Gray.copy(alpha = 0.1f))
        }

        if (paddingLeft > 0 || paddingTop > 0 || paddingRight > 0 || paddingBottom > 0) {
            m = m.padding(
                start = paddingLeft.dp,
                top = paddingTop.dp,
                end = paddingRight.dp,
                bottom = paddingBottom.dp
            )
        }
        m
    }

    val modifier = if (flexWeight > 0) baseModifier.weight(flexWeight) else baseModifier
    val layoutMode = (component.properties["LayoutMode"] ?: component.properties["@LayoutMode"])?.toString() ?: "Top"

    when (component.type) {
        "Group" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center" || layoutMode == "Centre") {
                Row(
                    modifier = modifier,
                    horizontalArrangement = when (layoutMode) {
                        "Left" -> Arrangement.Start
                        "Right" -> Arrangement.End
                        else -> Arrangement.Center
                    },
                    verticalAlignment = when (layoutMode) {
                        "Center", "Centre" -> Alignment.CenterVertically
                        "Bottom" -> Alignment.Bottom
                        else -> Alignment.Top
                    }
                ) {
                    component.children.forEach { RenderComponentInRow(it, caretOffset, onComponentClick) }
                }
            } else {
                Column(
                    modifier = modifier,
                    verticalArrangement = when (layoutMode) {
                        "Bottom" -> Arrangement.Bottom
                        "Center", "Centre" -> Arrangement.Center
                        else -> Arrangement.Top
                    },
                    horizontalAlignment = when (layoutMode) {
                        "Center", "Centre" -> Alignment.CenterHorizontally
                        "Right" -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick) }
                }
            }
        }
        "Label" -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            val style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        "TextButton" -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            var style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>

            // Unwrap style if it's wrapped in a constructor map like TextButtonStyle(...)
            if (style != null && style.size == 1 && !style.containsKey("Default") && !style.containsKey("@Default")) {
                val firstValue = style.values.first()
                if (firstValue is Map<*, *>) {
                    style = firstValue
                }
            }
            
            // If the style is a TextButtonStyle, it will have a "Default" key (and "Hovered", "Pressed")
            // Or it might just be the content of "Default" if it was resolved from a global style.
            val textButtonStyle = if (style?.containsKey("Default") == true) {
                style["Default"] as? Map<*, *>
            } else if (style?.containsKey("@Default") == true) {
                style["@Default"] as? Map<*, *>
            } else {
                style
            }
            
            val bgValue = textButtonStyle?.get("Background") ?: textButtonStyle?.get("@Background")
            val bg = bgValue as? HytaleUiColor
            val labelStyle = (textButtonStyle?.get("LabelStyle") ?: textButtonStyle?.get("@LabelStyle")) as? Map<*, *> ?: textButtonStyle

            val btnModifier = modifier.background(
                if (bg != null) {
                    parseHexColor(bg.hex).copy(alpha = bg.alpha)
                } else if (bgValue is Map<*, *>) {
                    val hex = bgValue["hex"]?.toString() ?: bgValue["Hex"]?.toString()
                    val alpha = bgValue["alpha"]?.toString()?.toFloatOrNull() ?: bgValue["Alpha"]?.toString()?.toFloatOrNull() ?: 1.0f
                    if (hex != null) parseHexColor(hex).copy(alpha = alpha) else Color(0xFF2B3542)
                } else {
                    Color(0xFF2B3542)
                }
            )
            RenderLabel(text, labelStyle, btnModifier)
        }
        "CheckBox", "CheckBoxWithLabel" -> {
            val text = (component.properties["@Text"] ?: component.properties["Text"])?.toString() ?: ""
            val checked = (component.properties["@Checked"] ?: component.properties["Checked"])?.toString()?.toBoolean() ?: false
            
            val hAlign = component.properties["HorizontalAlignment"]?.toString() ?: "Left"

            Row(
                modifier = modifier, 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (hAlign) {
                    "Center" -> Arrangement.Center
                    "Right" -> Arrangement.End
                    else -> Arrangement.Start
                }
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .background(Color(0xFF2B3542))
                        .padding(2.dp)
                ) {
                    if (checked) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4A9EFF)))
                    }
                }
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = text, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        "TextField" -> {
            val placeholder = component.properties["PlaceholderText"]?.toString() ?: ""
            Box(
                modifier = modifier.background(Color(0xFF0A1119)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = placeholder, color = Color.Gray, fontSize = 13.sp)
            }
        }
        "NumberField" -> {
            val value = component.properties["Value"]?.toString() ?: "0"
            Box(
                modifier = modifier.background(Color(0xFF0A1119)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(text = value, color = Color.White, fontSize = 13.sp)
            }
        }
        else -> {
            Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.3f))) {
                Column {
                    Text(component.type, fontSize = 8.sp, color = Color.LightGray)
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick) }
                }
            }
        }
    }
}

@Composable
fun RowScope.RenderComponentInRow(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit) {
    val isActive = caretOffset in component.startOffset..component.endOffset
    val isDeepestActive = isActive && component.children.none { caretOffset in it.startOffset..it.endOffset }

    val anchor = (component.properties["Anchor"] ?: component.properties["@Anchor"]) as? HytaleUiAnchor
    val flexWeight = (component.properties["FlexWeight"] ?: component.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f
    
    val paddingMap = (component.properties["Padding"] ?: component.properties["@Padding"])
    val paddingFull = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Full"] ?: paddingMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> paddingMap.full ?: 0
        else -> 0
    }
    val paddingTop = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Top"] ?: paddingMap["@Top"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingBottom = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Bottom"] ?: paddingMap["@Bottom"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingLeft = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Left"] ?: paddingMap["@Left"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }
    val paddingRight = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Right"] ?: paddingMap["@Right"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingFull
        else -> paddingFull
    }

    val marginMap = (component.properties["Margin"] ?: component.properties["@Margin"])
    val marginFull = when (marginMap) {
        is Map<*, *> -> (marginMap["Full"] ?: marginMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> marginMap.full ?: 0
        else -> 0
    }
    val marginTop = when (marginMap) {
        is Map<*, *> -> (marginMap["Top"] ?: marginMap["@Top"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginBottom = when (marginMap) {
        is Map<*, *> -> (marginMap["Bottom"] ?: marginMap["@Bottom"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginLeft = when (marginMap) {
        is Map<*, *> -> (marginMap["Left"] ?: marginMap["@Left"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }
    val marginRight = when (marginMap) {
        is Map<*, *> -> (marginMap["Right"] ?: marginMap["@Right"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginFull
        else -> marginFull
    }

    val baseModifier = run {
        var m: Modifier = Modifier.clickable { onComponentClick(component.startOffset) }

        if (isDeepestActive) {
            m = m.border(1.dp, Color.Cyan)
        }
        
        if (marginLeft > 0 || marginTop > 0 || marginRight > 0 || marginBottom > 0) {
            m = m.padding(
                start = marginLeft.dp,
                top = marginTop.dp,
                end = marginRight.dp,
                bottom = marginBottom.dp
            )
        }

        if (anchor != null) {
            if (anchor.width != null) m = m.width(anchor.width.dp)
            if (anchor.height != null) {
                m = m.height(anchor.height.dp)
            } else if (flexWeight <= 0f) {
                m = m.fillMaxHeight()
            }
        } else if (flexWeight <= 0f) {
            m = m.fillMaxHeight()
        }
        
        val bgValue = component.properties["Background"] ?: component.properties["@Background"]
        val bg = bgValue as? HytaleUiColor
        if (bg != null) {
            val color = parseHexColor(bg.hex).copy(alpha = bg.alpha)
            m = m.background(color)
        } else if (bgValue is Map<*, *>) {
            val hex = bgValue["hex"]?.toString() ?: bgValue["Hex"]?.toString()
            if (hex != null) {
                val alpha = bgValue["alpha"]?.toString()?.toFloatOrNull() ?: bgValue["Alpha"]?.toString()?.toFloatOrNull() ?: 1.0f
                m = m.background(parseHexColor(hex).copy(alpha = alpha))
            }
        } else if (component.type == "Group" && component.children.isEmpty() && anchor?.width != null && anchor.height != null) {
             m = m.background(Color.Gray.copy(alpha = 0.1f))
        }

        if (paddingLeft > 0 || paddingTop > 0 || paddingRight > 0 || paddingBottom > 0) {
            m = m.padding(
                start = paddingLeft.dp,
                top = paddingTop.dp,
                end = paddingRight.dp,
                bottom = paddingBottom.dp
            )
        }
        m
    }

    val modifier = if (flexWeight > 0) baseModifier.weight(flexWeight) else baseModifier
    val layoutMode = (component.properties["LayoutMode"] ?: component.properties["@LayoutMode"])?.toString() ?: "Top"

    when (component.type) {
        "Group" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center" || layoutMode == "Centre") {
                Row(
                    modifier = modifier,
                    horizontalArrangement = when (layoutMode) {
                        "Left" -> Arrangement.Start
                        "Right" -> Arrangement.End
                        else -> Arrangement.Center
                    },
                    verticalAlignment = when (layoutMode) {
                        "Center", "Centre" -> Alignment.CenterVertically
                        "Bottom" -> Alignment.Bottom
                        else -> Alignment.Top
                    }
                ) {
                    component.children.forEach { RenderComponentInRow(it, caretOffset, onComponentClick) }
                }
            } else {
                Column(
                    modifier = modifier,
                    verticalArrangement = when (layoutMode) {
                        "Bottom" -> Arrangement.Bottom
                        "Center", "Centre" -> Arrangement.Center
                        else -> Arrangement.Top
                    },
                    horizontalAlignment = when (layoutMode) {
                        "Center", "Centre" -> Alignment.CenterHorizontally
                        "Right" -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick) }
                }
            }
        }
        "Label" -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            val style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        "TextButton" -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            var style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>

            // Unwrap style if it's wrapped in a constructor map like TextButtonStyle(...)
            if (style != null && style.size == 1 && !style.containsKey("Default") && !style.containsKey("@Default")) {
                val firstValue = style.values.first()
                if (firstValue is Map<*, *>) {
                    style = firstValue
                }
            }
            
            // If the style is a TextButtonStyle, it will have a "Default" key (and "Hovered", "Pressed")
            // Or it might just be the content of "Default" if it was resolved from a global style.
            val textButtonStyle = if (style?.containsKey("Default") == true) {
                style["Default"] as? Map<*, *>
            } else if (style?.containsKey("@Default") == true) {
                style["@Default"] as? Map<*, *>
            } else {
                style
            }
            
            val bgValue = textButtonStyle?.get("Background") ?: textButtonStyle?.get("@Background")
            val bg = bgValue as? HytaleUiColor
            val labelStyle = (textButtonStyle?.get("LabelStyle") ?: textButtonStyle?.get("@LabelStyle")) as? Map<*, *> ?: textButtonStyle

            val btnModifier = modifier.background(
                if (bg != null) {
                    parseHexColor(bg.hex).copy(alpha = bg.alpha)
                } else if (bgValue is Map<*, *>) {
                    val hex = bgValue["hex"]?.toString() ?: bgValue["Hex"]?.toString()
                    val alpha = bgValue["alpha"]?.toString()?.toFloatOrNull() ?: bgValue["Alpha"]?.toString()?.toFloatOrNull() ?: 1.0f
                    if (hex != null) parseHexColor(hex).copy(alpha = alpha) else Color(0xFF2B3542)
                } else {
                    Color(0xFF2B3542)
                }
            )
            RenderLabel(text, labelStyle, btnModifier)
        }
        "CheckBox", "CheckBoxWithLabel" -> {
            val text = (component.properties["@Text"] ?: component.properties["Text"])?.toString() ?: ""
            val checked = (component.properties["@Checked"] ?: component.properties["Checked"])?.toString()?.toBoolean() ?: false
            
            val hAlign = component.properties["HorizontalAlignment"]?.toString() ?: "Left"

            Row(
                modifier = modifier, 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (hAlign) {
                    "Center" -> Arrangement.Center
                    "Right" -> Arrangement.End
                    else -> Arrangement.Start
                }
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .background(Color(0xFF2B3542))
                        .padding(2.dp)
                ) {
                    if (checked) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4A9EFF)))
                    }
                }
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = text, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        "TextField" -> {
            val placeholder = component.properties["PlaceholderText"]?.toString() ?: ""
            Box(
                modifier = modifier.background(Color(0xFF0A1119)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = placeholder, color = Color.Gray, fontSize = 13.sp)
            }
        }
        "NumberField" -> {
            val value = component.properties["Value"]?.toString() ?: "0"
            Box(
                modifier = modifier.background(Color(0xFF0A1119)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(text = value, color = Color.White, fontSize = 13.sp)
            }
        }
        else -> {
            Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.3f))) {
                Column {
                    Text(component.type, fontSize = 8.sp, color = Color.LightGray)
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick) }
                }
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length == 3) {
            val r = cleanHex.substring(0, 1).repeat(2).toInt(16)
            val g = cleanHex.substring(1, 2).repeat(2).toInt(16)
            val b = cleanHex.substring(2, 3).repeat(2).toInt(16)
            return Color(r, g, b)
        }
        val longVal = cleanHex.toLong(16)
        if (cleanHex.length <= 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        Color.DarkGray
    }
}

fun highlightHytaleUi(content: String): AnnotatedString {
    return buildAnnotatedString {
        val typeColor = Color(0xFF4A9EFF)
        val propertyColor = Color(0xFF9CDCFE)
        val valueColor = Color(0xFFCE9178)
        val commentColor = Color(0xFF6A9955)
        val idColor = Color(0xFFDCDCAA)

        var i = 0
        while (i < content.length) {
            val char = content[i]
            when {
                content.startsWith("//", i) -> {
                    val end = content.indexOf("\n", i).let { if (it == -1) content.length else it }
                    withStyle(SpanStyle(color = commentColor)) {
                        append(content.substring(i, end))
                    }
                    i = end
                }
                char == '"' -> {
                    val end = content.indexOf('"', i + 1).let { if (it == -1) content.length else it + 1 }
                    withStyle(SpanStyle(color = valueColor)) {
                        append(content.substring(i, end))
                    }
                    i = end
                }
                char == '#' -> {
                    var end = i + 1
                    while (end < content.length && (content[end].isLetterOrDigit() || content[end] == '_')) end++
                    withStyle(SpanStyle(color = idColor)) {
                        append(content.substring(i, end))
                    }
                    i = end
                }
                char.isLetter() || char == '$' || char == '@' -> {
                    var end = i + 1
                    while (end < content.length && (content[end].isLetterOrDigit() || content[end] == '_' || content[end] == '.' || content[end] == '@')) end++
                    val word = content.substring(i, end)
                    
                    val nextChar = content.getOrNull(end)
                    val nextNextChar = if (end + 1 < content.length) content[end + 1] else null
                    
                    when {
                        word.startsWith("$") -> {
                            withStyle(SpanStyle(color = typeColor)) { append(word) }
                        }
                        word.startsWith("@") -> {
                            withStyle(SpanStyle(color = propertyColor)) { append(word) }
                        }
                        nextChar == ':' || nextChar == '=' || (nextChar?.isWhitespace() == true && (nextNextChar == ':' || nextNextChar == '=')) -> {
                            withStyle(SpanStyle(color = propertyColor)) { append(word) }
                        }
                        else -> {
                            withStyle(SpanStyle(color = typeColor)) { append(word) }
                        }
                    }
                    i = end
                }
                else -> {
                    append(char)
                    i++
                }
            }
        }
    }
}
