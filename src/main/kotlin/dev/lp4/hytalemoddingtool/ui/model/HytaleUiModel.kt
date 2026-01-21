package dev.lp4.hytalemoddingtool.ui.model

data class HytaleUiComponent(
    val type: String,
    val id: String? = null,
    val properties: Map<String, Any?> = emptyMap(),
    val children: List<HytaleUiComponent> = emptyList(),
    val startOffset: Int = 0,
    val endOffset: Int = 0
)

data class HytaleUiFile(
    val imports: List<String> = emptyList(),
    val styles: Map<String, Any?> = emptyMap(),
    val rootComponent: HytaleUiComponent? = null
)

data class HytaleUiAnchor(
    val width: Int? = null,
    val height: Int? = null,
    val full: Int? = null,
    val flexWeight: Float? = null,
    val top: Int? = null,
    val bottom: Int? = null,
    val left: Int? = null,
    val right: Int? = null
)

data class HytaleUiColor(
    val hex: String,
    val alpha: Float = 1.0f
)
