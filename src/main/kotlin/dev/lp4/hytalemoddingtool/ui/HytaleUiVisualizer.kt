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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.foundation.Image
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

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
                LayoutPreview(root, caretOffset, navigateToCode, project, file)
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
        is HytaleUiAnchor -> {
            val parts = mutableListOf<String>()
            value.width?.let { parts.add("W:$it") }
            value.height?.let { parts.add("H:$it") }
            value.full?.let { parts.add("F:$it") }
            value.flexWeight?.let { parts.add("FW:$it") }
            value.top?.let { parts.add("T:$it") }
            value.bottom?.let { parts.add("B:$it") }
            value.left?.let { parts.add("L:$it") }
            value.right?.let { parts.add("R:$it") }
            "Anchor(${parts.joinToString(", ")})"
        }
        is HytaleUiColor -> "Color(${value.hex}, A:${value.alpha})"
        is Map<*, *> -> "(" + value.entries.joinToString(", ") { "${it.key}: ${formatValue(it.value)}" } + ")"
        is List<*> -> "[" + value.joinToString(", ") { formatValue(it) } + "]"
        else -> value.toString()
    }
}

@Composable
fun LayoutPreview(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit, project: Project, file: VirtualFile) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        RenderComponent(component, caretOffset, onComponentClick, project, file)
    }
}

@Composable
fun RenderLabel(
    text: String,
    style: Map<*, *>?,
    modifier: Modifier
) {
    // Check if the text is a localized string key and format it for the preview
    val displayText = if (text.startsWith("%")) {
        text.substringAfterLast(".").replaceFirstChar { it.uppercase() }
    } else {
        text
    }

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
            text = if (isUppercase) displayText.uppercase() else displayText,
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
fun RenderComponent(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit, project: Project, file: VirtualFile) {
    RenderComponentWithModifier(component, caretOffset, onComponentClick, Modifier, project, file)
}

@Composable
fun RenderComponentWithModifier(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit, inheritedModifier: Modifier, project: Project, file: VirtualFile, isInLayout: Boolean = false) {
    Box(modifier = Modifier) {
        RenderComponentInternal(component, caretOffset, onComponentClick, inheritedModifier, project, file, isInLayout)
    }
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.RenderComponentInternal(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit, inheritedModifier: Modifier, project: Project, file: VirtualFile, isInLayout: Boolean = false) {
    val isActive = caretOffset in component.startOffset..component.endOffset
    val isDeepestActive = isActive && component.children.none { caretOffset in it.startOffset..it.endOffset }

    val anchor = (component.properties["Anchor"] ?: component.properties["@Anchor"]) as? HytaleUiAnchor
    
    var componentModifier: Modifier = Modifier
    anchor?.let {
        // Handle Alignment in Box containers (like the root container)
        val alignment = when {
            it.left != null && it.top != null -> Alignment.TopStart
            it.right != null && it.top != null -> Alignment.TopEnd
            it.left != null && it.bottom != null -> Alignment.BottomStart
            it.right != null && it.bottom != null -> Alignment.BottomEnd
            it.left != null -> Alignment.CenterStart
            it.right != null -> Alignment.CenterEnd
            it.top != null -> Alignment.TopCenter
            it.bottom != null -> Alignment.BottomCenter
            else -> null
        }
        
        if (alignment != null) {
            componentModifier = componentModifier.then(Modifier.align(alignment))
        }

        // Apply Anchor offsets as padding BEFORE size so they act as margins relative to the alignment
        if (!isInLayout) {
            if (it.top != null) componentModifier = componentModifier.padding(top = it.top.dp)
            if (it.bottom != null) componentModifier = componentModifier.padding(bottom = it.bottom.dp)
            if (it.left != null) componentModifier = componentModifier.padding(start = it.left.dp)
            if (it.right != null) componentModifier = componentModifier.padding(end = it.right.dp)
        }

        if (it.width != null) componentModifier = componentModifier.width(it.width.dp)
        
        if (it.height != null) componentModifier = componentModifier.height(it.height.dp)
    }

    // Process Margin (outer padding)
    val marginMap = (component.properties["Margin"] ?: component.properties["@Margin"])
    val marginFull = when (marginMap) {
        is Map<*, *> -> (marginMap["Full"] ?: marginMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> marginFullFromAnchor(marginMap)
        is Number -> marginMap.toInt()
        else -> 0
    }
    val marginHorizontal = when (marginMap) {
        is Map<*, *> -> (marginMap["Horizontal"] ?: marginMap["@Horizontal"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginHorizontalFromAnchor(marginMap, marginFull)
        else -> marginFull
    }
    val marginVertical = when (marginMap) {
        is Map<*, *> -> (marginMap["Vertical"] ?: marginMap["@Vertical"])?.toString()?.toIntOrNull() ?: marginFull
        is HytaleUiAnchor -> marginVerticalFromAnchor(marginMap, marginFull)
        else -> marginFull
    }
    val marginTop = when (marginMap) {
        is Map<*, *> -> (marginMap["Top"] ?: marginMap["@Top"])?.toString()?.toIntOrNull() ?: marginVertical
        is HytaleUiAnchor -> marginMap.top ?: marginVertical
        else -> marginVertical
    }
    val marginBottom = when (marginMap) {
        is Map<*, *> -> (marginMap["Bottom"] ?: marginMap["@Bottom"])?.toString()?.toIntOrNull() ?: marginVertical
        is HytaleUiAnchor -> marginMap.bottom ?: marginVertical
        else -> marginVertical
    }
    val marginLeft = when (marginMap) {
        is Map<*, *> -> (marginMap["Left"] ?: marginMap["@Left"])?.toString()?.toIntOrNull() ?: marginHorizontal
        is HytaleUiAnchor -> marginMap.left ?: marginHorizontal
        else -> marginHorizontal
    }
    val marginRight = when (marginMap) {
        is Map<*, *> -> (marginMap["Right"] ?: marginMap["@Right"])?.toString()?.toIntOrNull() ?: marginHorizontal
        is HytaleUiAnchor -> marginMap.right ?: marginHorizontal
        else -> marginHorizontal
    }

    if (marginLeft > 0 || marginTop > 0 || marginRight > 0 || marginBottom > 0) {
        componentModifier = componentModifier.padding(
            start = marginLeft.dp,
            top = marginTop.dp,
            end = marginRight.dp,
            bottom = marginBottom.dp
        )
    }

    // Process Padding (inner padding)
    val paddingMap = (component.properties["Padding"] ?: component.properties["@Padding"])
    val paddingFull = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Full"] ?: paddingMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> paddingFullFromAnchor(paddingMap)
        is Number -> paddingMap.toInt()
        else -> 0
    }
    val paddingHorizontal = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Horizontal"] ?: paddingMap["@Horizontal"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingHorizontalFromAnchor(paddingMap, paddingFull)
        else -> paddingFull
    }
    val paddingVertical = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Vertical"] ?: paddingMap["@Vertical"])?.toString()?.toIntOrNull() ?: paddingFull
        is HytaleUiAnchor -> paddingVerticalFromAnchor(paddingMap, paddingFull)
        else -> paddingFull
    }
    val paddingTop = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Top"] ?: paddingMap["@Top"])?.toString()?.toIntOrNull() ?: paddingVertical
        is HytaleUiAnchor -> paddingMap.top ?: paddingVertical
        else -> paddingVertical
    }
    val paddingBottom = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Bottom"] ?: paddingMap["@Bottom"])?.toString()?.toIntOrNull() ?: paddingVertical
        is HytaleUiAnchor -> paddingMap.bottom ?: paddingVertical
        else -> paddingVertical
    }
    val paddingLeft = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Left"] ?: paddingMap["@Left"])?.toString()?.toIntOrNull() ?: paddingHorizontal
        is HytaleUiAnchor -> paddingMap.left ?: paddingHorizontal
        else -> paddingHorizontal
    }
    val paddingRight = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Right"] ?: paddingMap["@Right"])?.toString()?.toIntOrNull() ?: paddingHorizontal
        is HytaleUiAnchor -> paddingMap.right ?: paddingHorizontal
        else -> paddingHorizontal
    }

    val baseModifier = run {
        var m: Modifier = inheritedModifier.then(componentModifier).clickable { onComponentClick(component.startOffset) }

        if (isDeepestActive) {
            m = m.border(1.dp, Color.Cyan)
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

    val modifier = baseModifier
    val layoutMode = (component.properties["LayoutMode"] ?: component.properties["@LayoutMode"])?.toString() ?: "Top"
    val flexWeight = (component.properties["FlexWeight"] ?: component.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f

    val bgValue2 = component.properties["Background"] ?: component.properties["@Background"]
    val texturePath = when (bgValue2) {
        is String -> bgValue2
        is Map<*, *> -> (bgValue2["TexturePath"] ?: bgValue2["@TexturePath"] ?: bgValue2["Texture"] ?: bgValue2["@Texture"])?.toString()
        else -> null
    }

    if (texturePath != null) {
        RenderImage(texturePath, project, file, Modifier.matchParentSize())
    }

    val labelTypes = listOf(
        "Label", "CenteredTitleLabel", "HotkeyLabel", "LabelAffix", "PanelTitle", 
        "RowLabel", "StatNameLabel", "StatNameValueLabel", "TitleLabel"
    )
    
    val buttonTypes = listOf(
        "Button", "ActionButton", "BackButton", "ColumnButton", "DestructiveTextButton", 
        "PrimaryButton", "PrimaryTextButton", "SecondaryButton", "SecondaryTextButton", 
        "SmallSecondaryTextButton", "TabButton", "TagTextButton", "TertiaryTextButton", 
        "ToggleButton", "ToolButton", "TextButton", "MenuItem"
    )

    val contentAlignment = run {
        val hAlign = when {
            anchor?.left != null -> Alignment.Start
            anchor?.right != null -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
        val vAlign = when {
            anchor?.top != null -> Alignment.Top
            anchor?.bottom != null -> Alignment.Bottom
            else -> Alignment.CenterVertically
        }
        
        val alignment = when {
            anchor?.left != null && anchor.top != null -> Alignment.TopStart
            anchor?.right != null && anchor.top != null -> Alignment.TopEnd
            anchor?.left != null && anchor.bottom != null -> Alignment.BottomStart
            anchor?.right != null && anchor.bottom != null -> Alignment.BottomEnd
            anchor?.left != null -> Alignment.CenterStart
            anchor?.right != null -> Alignment.CenterEnd
            anchor?.top != null -> Alignment.TopCenter
            anchor?.bottom != null -> Alignment.BottomCenter
            else -> Alignment.Center
        }
        alignment
    }

    when {
        component.type == "Group" || component.type == "Container" || component.type == "Content" || 
        component.type == "DecoratedContainer" || component.type == "Overlay" || component.type == "Page" || 
        component.type == "Pages" || component.type == "Panel" || component.type == "SectionContainer" || 
        component.type == "Wrapper" || component.type == "ActionButtonContainer" || component.type == "Row" ||
        component.type == "RowHintContainer" || component.type == "RowLabelContainer" || component.type == "Title" ||
        component.type == "HeaderSearch" || component.type == "Legend" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center" || layoutMode == "Centre" || component.type == "Row") {
                Row(
                    modifier = modifier,
                    horizontalArrangement = when (layoutMode) {
                        "Left" -> Arrangement.Start
                        "Right" -> Arrangement.End
                        "Center", "Centre" -> Arrangement.Center
                        else -> Arrangement.Start
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    component.children.forEach { child ->
                        val childAnchor = (child.properties["Anchor"] ?: child.properties["@Anchor"]) as? HytaleUiAnchor
                        val childFlexWeight = (child.properties["FlexWeight"] ?: child.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: childAnchor?.flexWeight ?: 0f
                        
                        var childModifier: Modifier = if (childFlexWeight > 0) Modifier.weight(childFlexWeight) else Modifier
                        
                        // Handle spacing/offsets via padding in Row/Column
                        if (childAnchor != null) {
                            childModifier = childModifier.padding(
                                start = (childAnchor.left ?: 0).dp,
                                top = (childAnchor.top ?: 0).dp,
                                end = (childAnchor.right ?: 0).dp,
                                bottom = (childAnchor.bottom ?: 0).dp
                            )
                        }

                        if (childAnchor?.height == null && childAnchor?.full == null) {
                            childModifier = childModifier.fillMaxHeight()
                        }
                        
                        RenderComponentWithModifier(
                            child, caretOffset, onComponentClick, 
                            childModifier,
                            project, file,
                            isInLayout = true
                        )
                    }
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
                    component.children.forEach { child ->
                        val childAnchor = (child.properties["Anchor"] ?: child.properties["@Anchor"]) as? HytaleUiAnchor
                        val childFlexWeight = (child.properties["FlexWeight"] ?: child.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: childAnchor?.flexWeight ?: 0f
                        
                        var childModifier: Modifier = if (childFlexWeight > 0) Modifier.weight(childFlexWeight) else Modifier
                        
                        // Handle spacing/offsets via padding in Row/Column
                        if (childAnchor != null) {
                            childModifier = childModifier.padding(
                                start = (childAnchor.left ?: 0).dp,
                                top = (childAnchor.top ?: 0).dp,
                                end = (childAnchor.right ?: 0).dp,
                                bottom = (childAnchor.bottom ?: 0).dp
                            )
                        }

                        if (childAnchor?.width == null && childAnchor?.full == null) {
                            childModifier = childModifier.fillMaxWidth()
                        }

                        RenderComponentWithModifier(
                            child, caretOffset, onComponentClick, 
                            childModifier,
                            project, file,
                            isInLayout = true
                        )
                    }
                }
            }
        }
        component.type in labelTypes -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            val style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        component.type in buttonTypes -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            var style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>

            if (style != null && style.size == 1 && !style.containsKey("Default") && !style.containsKey("@Default")) {
                val firstValue = style.values.first()
                if (firstValue is Map<*, *>) {
                    style = firstValue
                }
            }
            
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
            
            val iconValue = component.properties["Icon"] ?: component.properties["@Icon"]
            val iconAnchor = (component.properties["IconAnchor"] ?: component.properties["@IconAnchor"]) as? HytaleUiAnchor
            var iconModifier: Modifier = Modifier.size(16.dp)
            if (iconAnchor != null) {
                if (iconAnchor.width != null) iconModifier = iconModifier.width(iconAnchor.width.dp)
                if (iconAnchor.height != null) iconModifier = iconModifier.height(iconAnchor.height.dp)
            }

            if (iconValue != null) {
                Row(modifier = btnModifier, verticalAlignment = Alignment.CenterVertically) {
                    Box(iconModifier.background(Color.White.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(4.dp))
                    RenderLabel(text, labelStyle, Modifier)
                }
            } else {
                RenderLabel(text, labelStyle, btnModifier)
            }
        }
        component.type == "CheckBox" || component.type == "CheckBoxWithLabel" || component.type == "LabeledCheckBox" -> {
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
                        .size(20.dp)
                        .border(1.dp, Color.Gray)
                        .background(if (checked) Color.Cyan else Color.Transparent)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RenderLabel(text, null, Modifier)
                }
            }
        }
        component.type == "TextField" || component.type == "NumberField" || component.type == "MultilineTextField" || component.type == "CompactTextField" -> {
            val placeholder = (component.properties["PlaceholderText"] ?: component.properties["@PlaceholderText"])?.toString() ?: ""
            Box(
                modifier = modifier
                    .height(30.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color.Gray),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = placeholder,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        component.type == "ProgressBar" || component.type == "CircularProgressBar" -> {
            val value = (component.properties["Value"] ?: component.properties["@Value"])?.toString()?.toFloatOrNull() ?: 0.5f
            Box(
                modifier = modifier
                    .height(20.dp)
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .background(Color.Green)
                )
            }
        }
        component.type == "Slider" || component.type == "SliderNumberField" || component.type == "FloatSliderNumberField" -> {
            Box(
                modifier = modifier
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                 Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                 Box(Modifier.size(12.dp).background(Color.LightGray).align(Alignment.Center))
            }
        }
        component.type == "Image" || component.type == "AssetImage" || component.type == "BackgroundImage" || component.type == "Icon" || component.type == "Sprite" -> {
            val textureValue = component.properties["TexturePath"] ?: component.properties["@TexturePath"] ?: 
                               component.properties["Texture"] ?: component.properties["@Texture"] ?:
                               component.properties["Image"] ?: component.properties["@Image"] ?:
                               component.properties["Icon"] ?: component.properties["@Icon"]
            
            Box(
                modifier = modifier
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(component.type, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    if (textureValue != null) {
                        val fileName = textureValue.toString().substringAfterLast("/")
                        Text(fileName, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    }
                }
            }
        }
        component.type == "Divider" || component.type == "Separator" || component.type == "ContentSeparator" || component.type == "VerticalSeparator" || component.type == "Sep" || component.type == "ActionButtonSeparator" || component.type == "VerticalActionButtonSeparator" || component.type == "PanelSeparatorFancy" -> {
             if (component.type.contains("Vertical")) {
                 Divider(Orientation.Vertical, modifier = modifier.fillMaxHeight())
             } else {
                 Divider(Orientation.Horizontal, modifier = modifier.fillMaxWidth())
             }
        }
        else -> {
            // Fallback for unknown types - render as a group-like container
            val iconValue = component.properties["Icon"] ?: component.properties["@Icon"]
            if (iconValue != null) {
                Box(modifier = modifier.background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Box(Modifier.size(16.dp).background(Color.White.copy(alpha = 0.3f)))
                         component.children.forEach { RenderComponent(it, caretOffset, onComponentClick, project, file) }
                    }
                }
            } else {
                Box(modifier = modifier, contentAlignment = contentAlignment) {
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick, project, file) }
                }
            }
        }
    }
}

@Composable
fun RowScope.RenderComponentInRow(component: HytaleUiComponent, caretOffset: Int, onComponentClick: (Int) -> Unit, project: Project, file: VirtualFile) {
    val isActive = caretOffset in component.startOffset..component.endOffset
    val isDeepestActive = isActive && component.children.none { caretOffset in it.startOffset..it.endOffset }

    val anchor = (component.properties["Anchor"] ?: component.properties["@Anchor"]) as? HytaleUiAnchor
    val flexWeight = (component.properties["FlexWeight"] ?: component.properties["@FlexWeight"])?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f
    
    val paddingMap = (component.properties["Padding"] ?: component.properties["@Padding"])
    val paddingFull = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Full"] ?: paddingMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> paddingMap.full ?: 0
        is Number -> paddingMap.toInt()
        else -> 0
    }
    val paddingHorizontal = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Horizontal"] ?: paddingMap["@Horizontal"])?.toString()?.toIntOrNull() ?: paddingFull
        else -> paddingFull
    }
    val paddingVertical = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Vertical"] ?: paddingMap["@Vertical"])?.toString()?.toIntOrNull() ?: paddingFull
        else -> paddingFull
    }
    val paddingTop = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Top"] ?: paddingMap["@Top"])?.toString()?.toIntOrNull() ?: paddingVertical
        is HytaleUiAnchor -> paddingVertical
        else -> paddingVertical
    }
    val paddingBottom = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Bottom"] ?: paddingMap["@Bottom"])?.toString()?.toIntOrNull() ?: paddingVertical
        is HytaleUiAnchor -> paddingVertical
        else -> paddingVertical
    }
    val paddingLeft = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Left"] ?: paddingMap["@Left"])?.toString()?.toIntOrNull() ?: paddingHorizontal
        is HytaleUiAnchor -> paddingHorizontal
        else -> paddingHorizontal
    }
    val paddingRight = when (paddingMap) {
        is Map<*, *> -> (paddingMap["Right"] ?: paddingMap["@Right"])?.toString()?.toIntOrNull() ?: paddingHorizontal
        is HytaleUiAnchor -> paddingHorizontal
        else -> paddingHorizontal
    }

    val marginMap = (component.properties["Margin"] ?: component.properties["@Margin"])
    val marginFull = when (marginMap) {
        is Map<*, *> -> (marginMap["Full"] ?: marginMap["@Full"])?.toString()?.toIntOrNull() ?: 0
        is HytaleUiAnchor -> marginMap.full ?: 0
        is Number -> marginMap.toInt()
        else -> 0
    }
    val marginHorizontal = when (marginMap) {
        is Map<*, *> -> (marginMap["Horizontal"] ?: marginMap["@Horizontal"])?.toString()?.toIntOrNull() ?: marginFull
        else -> marginFull
    }
    val marginVertical = when (marginMap) {
        is Map<*, *> -> (marginMap["Vertical"] ?: marginMap["@Vertical"])?.toString()?.toIntOrNull() ?: marginFull
        else -> marginFull
    }
    val marginTop = when (marginMap) {
        is Map<*, *> -> (marginMap["Top"] ?: marginMap["@Top"])?.toString()?.toIntOrNull() ?: marginVertical
        is HytaleUiAnchor -> marginVertical
        else -> marginVertical
    }
    val marginBottom = when (marginMap) {
        is Map<*, *> -> (marginMap["Bottom"] ?: marginMap["@Bottom"])?.toString()?.toIntOrNull() ?: marginVertical
        is HytaleUiAnchor -> marginVertical
        else -> marginVertical
    }
    val marginLeft = when (marginMap) {
        is Map<*, *> -> (marginMap["Left"] ?: marginMap["@Left"])?.toString()?.toIntOrNull() ?: marginHorizontal
        is HytaleUiAnchor -> marginHorizontal
        else -> marginHorizontal
    }
    val marginRight = when (marginMap) {
        is Map<*, *> -> (marginMap["Right"] ?: marginMap["@Right"])?.toString()?.toIntOrNull() ?: marginHorizontal
        is HytaleUiAnchor -> marginHorizontal
        else -> marginHorizontal
    }

    val baseModifier = run {
        var m: Modifier = Modifier.clickable { onComponentClick(component.startOffset) }

        if (isDeepestActive) {
            m = m.border(1.dp, Color.Cyan)
        }
        
        anchor?.let {
            // Apply Anchor offsets as padding BEFORE size so they act as margins relative to the alignment
            // These will be correctly positioned by the parent Box with contentAlignment
            if (it.top != null) m = m.padding(top = it.top.dp)
            if (it.bottom != null) m = m.padding(bottom = it.bottom.dp)
            if (it.left != null) m = m.padding(start = it.left.dp)
            if (it.right != null) m = m.padding(end = it.right.dp)

            if (it.width != null) m = m.width(it.width.dp)
            if (it.height != null) m = m.height(it.height.dp)
        }

        if (marginLeft > 0 || marginTop > 0 || marginRight > 0 || marginBottom > 0) {
            m = m.padding(
                start = marginLeft.dp,
                top = marginTop.dp,
                end = marginRight.dp,
                bottom = marginBottom.dp
            )
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
            } else {
                // Check for image background in map
                val texture = bgValue["TexturePath"]?.toString() ?: bgValue["@TexturePath"] ?: bgValue["Texture"]?.toString() ?: bgValue["@Texture"]
                if (texture != null) {
                    m = m.background(Color.Gray.copy(alpha = 0.2f))
                }
            }
        } else if (bgValue is String) {
            // Background is an image path
            m = m.background(Color.Gray.copy(alpha = 0.2f))
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

    val labelTypes = listOf(
        "Label", "CenteredTitleLabel", "HotkeyLabel", "LabelAffix", "PanelTitle", 
        "RowLabel", "StatNameLabel", "StatNameValueLabel", "TitleLabel"
    )
    
    val buttonTypes = listOf(
        "Button", "ActionButton", "BackButton", "ColumnButton", "DestructiveTextButton", 
        "PrimaryButton", "PrimaryTextButton", "SecondaryButton", "SecondaryTextButton", 
        "SmallSecondaryTextButton", "TabButton", "TagTextButton", "TertiaryTextButton", 
        "ToggleButton", "ToolButton", "TextButton", "MenuItem"
    )

    when {
        component.type == "Group" || component.type == "Container" || component.type == "Content" || 
        component.type == "DecoratedContainer" || component.type == "Overlay" || component.type == "Page" || 
        component.type == "Pages" || component.type == "Panel" || component.type == "SectionContainer" || 
        component.type == "Wrapper" || component.type == "ActionButtonContainer" || component.type == "Row" ||
        component.type == "RowHintContainer" || component.type == "RowLabelContainer" || component.type == "Title" ||
        component.type == "HeaderSearch" || component.type == "Legend" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center" || layoutMode == "Centre" || component.type == "Row") {
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
                    component.children.forEach { RenderComponentInRow(it, caretOffset, onComponentClick, project, file) }
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
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick, project, file) }
                }
            }
        }
        component.type in labelTypes -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            val style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        component.type in buttonTypes -> {
            val text = (component.properties["Text"] ?: component.properties["@Text"])?.toString() ?: ""
            var style = (component.properties["Style"] ?: component.properties["@Style"]) as? Map<*, *>

            if (style != null && style.size == 1 && !style.containsKey("Default") && !style.containsKey("@Default")) {
                val firstValue = style.values.first()
                if (firstValue is Map<*, *>) {
                    style = firstValue
                }
            }
            
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
            
            val iconValue = component.properties["Icon"] ?: component.properties["@Icon"]
            val iconAnchor = (component.properties["IconAnchor"] ?: component.properties["@IconAnchor"]) as? HytaleUiAnchor
            var iconModifier: Modifier = Modifier.size(16.dp)
            if (iconAnchor != null) {
                if (iconAnchor.width != null) iconModifier = iconModifier.width(iconAnchor.width.dp)
                if (iconAnchor.height != null) iconModifier = iconModifier.height(iconAnchor.height.dp)
            }

            if (iconValue != null) {
                Row(modifier = btnModifier, verticalAlignment = Alignment.CenterVertically) {
                    Box(iconModifier.background(Color.White.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(4.dp))
                    RenderLabel(text, labelStyle, Modifier)
                }
            } else {
                RenderLabel(text, labelStyle, btnModifier)
            }
        }
        component.type == "CheckBox" || component.type == "CheckBoxWithLabel" || component.type == "LabeledCheckBox" -> {
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
                        .size(20.dp)
                        .border(1.dp, Color.Gray)
                        .background(if (checked) Color.Cyan else Color.Transparent)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RenderLabel(text, null, Modifier)
                }
            }
        }
        component.type == "TextField" || component.type == "NumberField" || component.type == "MultilineTextField" || component.type == "CompactTextField" -> {
            val placeholder = (component.properties["PlaceholderText"] ?: component.properties["@PlaceholderText"])?.toString() ?: ""
            Box(
                modifier = modifier
                    .height(30.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color.Gray),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = placeholder,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        component.type == "ProgressBar" || component.type == "CircularProgressBar" -> {
            val value = (component.properties["Value"] ?: component.properties["@Value"])?.toString()?.toFloatOrNull() ?: 0.5f
            Box(
                modifier = modifier
                    .height(20.dp)
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .background(Color.Green)
                )
            }
        }
        component.type == "Slider" || component.type == "SliderNumberField" || component.type == "FloatSliderNumberField" -> {
            Box(
                modifier = modifier
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                 Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                 Box(Modifier.size(12.dp).background(Color.LightGray).align(Alignment.Center))
            }
        }
        component.type == "Image" || component.type == "AssetImage" || component.type == "BackgroundImage" || component.type == "Icon" || component.type == "Sprite" -> {
            val textureValue = component.properties["TexturePath"] ?: component.properties["@TexturePath"] ?: 
                               component.properties["Texture"] ?: component.properties["@Texture"] ?:
                               component.properties["Image"] ?: component.properties["@Image"] ?:
                               component.properties["Icon"] ?: component.properties["@Icon"]
            
            Box(
                modifier = modifier
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(component.type, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    if (textureValue != null) {
                        val fileName = textureValue.toString().substringAfterLast("/")
                        Text(fileName, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    }
                }
            }
        }
        component.type == "CheckBox" || component.type == "CheckBoxWithLabel" || component.type == "LabeledCheckBox" -> {
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
                        .size(20.dp)
                        .border(1.dp, Color.Gray)
                        .background(if (checked) Color.Cyan else Color.Transparent)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RenderLabel(text, null, Modifier)
                }
            }
        }
        component.type == "TextField" || component.type == "NumberField" || component.type == "MultilineTextField" || component.type == "CompactTextField" -> {
            val placeholder = (component.properties["PlaceholderText"] ?: component.properties["@PlaceholderText"])?.toString() ?: ""
            Box(
                modifier = modifier
                    .height(30.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color.Gray),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = placeholder,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        component.type == "ProgressBar" || component.type == "CircularProgressBar" -> {
            val value = (component.properties["Value"] ?: component.properties["@Value"])?.toString()?.toFloatOrNull() ?: 0.5f
            Box(
                modifier = modifier
                    .height(20.dp)
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .background(Color.Green)
                )
            }
        }
        component.type == "Slider" || component.type == "SliderNumberField" || component.type == "FloatSliderNumberField" -> {
            Box(
                modifier = modifier
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                 Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                 Box(Modifier.size(12.dp).background(Color.LightGray).align(Alignment.Center))
            }
        }
        component.type == "Image" || component.type == "AssetImage" || component.type == "BackgroundImage" || component.type == "Icon" || component.type == "Sprite" -> {
            val textureValue = component.properties["TexturePath"] ?: component.properties["@TexturePath"] ?: 
                               component.properties["Texture"] ?: component.properties["@Texture"] ?:
                               component.properties["Image"] ?: component.properties["@Image"] ?:
                               component.properties["Icon"] ?: component.properties["@Icon"]
            
            Box(
                modifier = modifier
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(component.type, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    if (textureValue != null) {
                        val fileName = textureValue.toString().substringAfterLast("/")
                        Text(fileName, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    }
                }
            }
        }
        component.type == "Divider" || component.type == "Separator" || component.type == "ContentSeparator" || component.type == "VerticalSeparator" || component.type == "Sep" || component.type == "ActionButtonSeparator" || component.type == "VerticalActionButtonSeparator" || component.type == "PanelSeparatorFancy" -> {
             if (component.type.contains("Vertical")) {
                 Divider(Orientation.Vertical, modifier = modifier.fillMaxHeight())
             } else {
                 Divider(Orientation.Horizontal, modifier = modifier.fillMaxWidth())
             }
        }
        else -> {
            // Fallback for unknown types - render as a group-like container
            val iconValue = component.properties["Icon"] ?: component.properties["@Icon"]
            if (iconValue != null) {
                Box(modifier = modifier.background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Box(Modifier.size(16.dp).background(Color.White.copy(alpha = 0.3f)))
                         component.children.forEach { RenderComponent(it, caretOffset, onComponentClick, project, file) }
                    }
                }
            } else {
                Column(modifier = modifier) {
                    component.children.forEach { RenderComponent(it, caretOffset, onComponentClick, project, file) }
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

// Helper functions for Padding/Margin extraction from HytaleUiAnchor
private fun paddingFullFromAnchor(anchor: HytaleUiAnchor): Int = anchor.full ?: 0
private fun paddingHorizontalFromAnchor(anchor: HytaleUiAnchor, fallback: Int): Int = anchor.left ?: anchor.right ?: fallback
private fun paddingVerticalFromAnchor(anchor: HytaleUiAnchor, fallback: Int): Int = anchor.top ?: anchor.bottom ?: fallback

private fun marginFullFromAnchor(anchor: HytaleUiAnchor): Int = anchor.full ?: 0
private fun marginHorizontalFromAnchor(anchor: HytaleUiAnchor, fallback: Int): Int = anchor.left ?: anchor.right ?: fallback
private fun marginVerticalFromAnchor(anchor: HytaleUiAnchor, fallback: Int): Int = anchor.top ?: anchor.bottom ?: fallback

@Composable
fun RenderImage(texturePath: String, project: Project, currentFile: VirtualFile, modifier: Modifier, componentType: String = "Image") {
    val imageFile = remember(texturePath) {
        fun findFile(path: String): VirtualFile? {
            // Try relative to current file
            val parent = currentFile.parent
            var file = parent?.findFileByRelativePath(path)

            // Try relative to project root
            if (file == null) {
                val projectDir = project.guessProjectDir()
                file = projectDir?.findFileByRelativePath(path)
            }

            // Try direct path
            if (file == null) {
                val javaFile = File(path)
                if (javaFile.exists()) {
                    file = VfsUtil.findFileByIoFile(javaFile, true)
                }
            }
            return file
        }

        var file = findFile(texturePath)

        // Try @2x fallback if not found
        if (file == null) {
            val lastDotIndex = texturePath.lastIndexOf('.')
            val pathAt2x = if (lastDotIndex != -1) {
                texturePath.substring(0, lastDotIndex) + "@2x" + texturePath.substring(lastDotIndex)
            } else {
                texturePath + "@2x"
            }
            file = findFile(pathAt2x)
        }

        file
    }

    if (imageFile != null) {
        val bitmap = remember(imageFile) {
            try {
                imageFile.inputStream.use { loadImageBitmap(it) }
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = texturePath,
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback if bitmap loading fails
            Box(
                modifier = modifier.background(Color.Red.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Error loading: ${imageFile.name}", fontSize = 8.sp, color = Color.White)
            }
        }
    } else {
        // Placeholder for missing file
        Box(
            modifier = modifier.background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(componentType, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                val fileName = texturePath.substringAfterLast("/")
                Text(fileName, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
            }
        }
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
