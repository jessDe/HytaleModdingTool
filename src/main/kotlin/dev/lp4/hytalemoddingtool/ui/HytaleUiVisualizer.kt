package dev.lp4.hytalemoddingtool.ui

import androidx.compose.foundation.background
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

@Composable
fun HytaleUiVisualizer(file: VirtualFile) {
    val document = remember(file) { FileDocumentManager.getInstance().getDocument(file) }
    
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
                LayoutPreview(root)
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
                    UiComponentView(root, 0)
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
                text = if (rawContent.length > 1000) rawContent.take(1000) + "..." else rawContent,
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun UiComponentView(component: HytaleUiComponent, depth: Int = 0) {
    Column(modifier = Modifier.padding(start = if (depth > 0) 12.dp else 0.dp)) {
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
            UiComponentView(child, depth + 1)
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
fun LayoutPreview(component: HytaleUiComponent) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            RenderComponent(component)
        }
    }
}

@Composable
fun RenderLabel(
    text: String,
    style: Map<*, *>?,
    modifier: Modifier
) {
    val hAlign = style?.get("HorizontalAlignment")?.toString() ?: "Left"
    val vAlign = style?.get("VerticalAlignment")?.toString() ?: "Top"
    val fontSize = style?.get("FontSize")?.toString()?.toIntOrNull() ?: 12
    val textColor = style?.get("TextColor") as? HytaleUiColor
    val isBold = style?.get("RenderBold")?.toString()?.toBoolean() ?: false
    val isUppercase = style?.get("RenderUppercase")?.toString()?.toBoolean() ?: false
    val letterSpacing = style?.get("LetterSpacing")?.toString()?.toFloatOrNull() ?: 0f

    Box(
        modifier = modifier,
        contentAlignment = when {
            hAlign == "Center" && vAlign == "Center" -> Alignment.Center
            hAlign == "Center" && vAlign == "Top" -> Alignment.TopCenter
            hAlign == "Center" && vAlign == "Bottom" -> Alignment.BottomCenter
            hAlign == "Right" && vAlign == "Center" -> Alignment.CenterEnd
            hAlign == "Right" && vAlign == "Top" -> Alignment.TopEnd
            hAlign == "Right" && vAlign == "Bottom" -> Alignment.BottomEnd
            hAlign == "Left" && vAlign == "Center" -> Alignment.CenterStart
            hAlign == "Left" && vAlign == "Bottom" -> Alignment.BottomStart
            else -> Alignment.TopStart
        }
    ) {
        Text(
            text = if (isUppercase) text.uppercase() else text,
            color = if (textColor != null) parseHexColor(textColor.hex).copy(alpha = textColor.alpha) else Color.White,
            fontSize = fontSize.sp,
            textAlign = when (hAlign) {
                "Center" -> TextAlign.Center
                "Right" -> TextAlign.End
                else -> TextAlign.Start
            },
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = letterSpacing.sp
        )
    }
}

@Composable
fun ColumnScope.RenderComponent(component: HytaleUiComponent) {
    val anchor = component.properties["Anchor"] as? HytaleUiAnchor
    val flexWeight = component.properties["FlexWeight"]?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f
    
    val paddingMap = component.properties["Padding"] as? Map<*, *>
    val paddingFull = paddingMap?.get("Full")?.toString()?.toIntOrNull() ?: 0

    val baseModifier = run {
        var m: Modifier = Modifier
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
        
        val bg = component.properties["Background"] as? HytaleUiColor
        if (bg != null) {
            val color = parseHexColor(bg.hex).copy(alpha = bg.alpha)
            m = m.background(color)
        } else if (component.type == "Group" && component.children.isEmpty() && anchor?.width != null && anchor.height != null) {
             m = m.background(Color.Gray.copy(alpha = 0.1f))
        }

        if (paddingFull > 0) {
            m = m.padding(paddingFull.dp)
        }
        m
    }

    val modifier = if (flexWeight > 0) baseModifier.weight(flexWeight) else baseModifier
    val layoutMode = component.properties["LayoutMode"]?.toString() ?: "Top"

    when (component.type) {
        "Group" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center") {
                Row(
                    modifier = modifier,
                    horizontalArrangement = when (layoutMode) {
                        "Left" -> Arrangement.Start
                        "Right" -> Arrangement.End
                        else -> Arrangement.Center
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    component.children.forEach { RenderComponentInRow(it) }
                }
            } else {
                Column(
                    modifier = modifier,
                    verticalArrangement = when (layoutMode) {
                        "Bottom" -> Arrangement.Bottom
                        "Center" -> Arrangement.Center
                        else -> Arrangement.Top
                    },
                    horizontalAlignment = when (layoutMode) {
                        "Center" -> Alignment.CenterHorizontally
                        "Right" -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    component.children.forEach { RenderComponent(it) }
                }
            }
        }
        "Label" -> {
            val text = component.properties["Text"]?.toString() ?: ""
            val style = component.properties["Style"] as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        "TextButton" -> {
            val text = (component.properties["@Text"] ?: component.properties["Text"])?.toString() ?: ""
            var style = component.properties["Style"] as? Map<*, *>
            
            // Handle @StyleName reference which might be stored in the style map
            if (style == null) {
                 style = component.properties["@Style"] as? Map<*, *>
            }

            val defaultStyle = (style?.get("Default") as? Map<*, *>) ?: style
            
            val bg = defaultStyle?.get("Background") as? HytaleUiColor
            val labelStyle = defaultStyle?.get("LabelStyle") as? Map<*, *> ?: defaultStyle

            val btnModifier = modifier.background(
                if (bg != null) parseHexColor(bg.hex).copy(alpha = bg.alpha) else Color(0xFF2B3542)
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
                    component.children.forEach { RenderComponent(it) }
                }
            }
        }
    }
}

@Composable
fun RowScope.RenderComponentInRow(component: HytaleUiComponent) {
    val anchor = component.properties["Anchor"] as? HytaleUiAnchor
    val flexWeight = component.properties["FlexWeight"]?.toString()?.toFloatOrNull() ?: anchor?.flexWeight ?: 0f
    
    val paddingMap = component.properties["Padding"] as? Map<*, *>
    val paddingFull = paddingMap?.get("Full")?.toString()?.toIntOrNull() ?: 0

    val baseModifier = run {
        var m: Modifier = Modifier
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
        
        val bg = component.properties["Background"] as? HytaleUiColor
        if (bg != null) {
            val color = parseHexColor(bg.hex).copy(alpha = bg.alpha)
            m = m.background(color)
        } else if (component.type == "Group" && component.children.isEmpty() && anchor?.width != null && anchor.height != null) {
             m = m.background(Color.Gray.copy(alpha = 0.1f))
        }

        if (paddingFull > 0) {
            m = m.padding(paddingFull.dp)
        }
        m
    }

    val modifier = if (flexWeight > 0) baseModifier.weight(flexWeight) else baseModifier
    val layoutMode = component.properties["LayoutMode"]?.toString() ?: "Top"

    when (component.type) {
        "Group" -> {
            if (layoutMode == "Left" || layoutMode == "Right" || layoutMode == "Center") {
                Row(
                    modifier = modifier,
                    horizontalArrangement = when (layoutMode) {
                        "Left" -> Arrangement.Start
                        "Right" -> Arrangement.End
                        else -> Arrangement.Center
                    },
                    verticalAlignment = when (layoutMode) {
                        "Center" -> Alignment.CenterVertically
                        else -> Alignment.Top
                    }
                ) {
                    component.children.forEach { RenderComponentInRow(it) }
                }
            } else {
                Column(
                    modifier = modifier,
                    verticalArrangement = when (layoutMode) {
                        "Bottom" -> Arrangement.Bottom
                        "Center" -> Arrangement.Center
                        else -> Arrangement.Top
                    },
                    horizontalAlignment = when (layoutMode) {
                        "Center" -> Alignment.CenterHorizontally
                        "Right" -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    component.children.forEach { RenderComponent(it) }
                }
            }
        }
        "Label" -> {
            val text = component.properties["Text"]?.toString() ?: ""
            val style = component.properties["Style"] as? Map<*, *>
            RenderLabel(text, style, modifier)
        }
        "TextButton" -> {
            val text = component.properties["Text"]?.toString() ?: ""
            var style = component.properties["Style"] as? Map<*, *>
            
            if (style == null) {
                 style = component.properties["@Style"] as? Map<*, *>
            }

            val defaultStyle = (style?.get("Default") as? Map<*, *>) ?: style
            
            val bg = defaultStyle?.get("Background") as? HytaleUiColor
            val labelStyle = defaultStyle?.get("LabelStyle") as? Map<*, *> ?: defaultStyle

            val btnModifier = modifier.background(
                if (bg != null) parseHexColor(bg.hex).copy(alpha = bg.alpha) else Color(0xFF2B3542)
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
                    component.children.forEach { RenderComponent(it) }
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
