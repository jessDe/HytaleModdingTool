package dev.lp4.hytalemoddingtool.ui.parser

import dev.lp4.hytalemoddingtool.ui.model.*

class HytaleUiParser(private val content: String) {
    private var pos = 0

    fun parse(): HytaleUiFile {
        val imports = mutableListOf<String>()
        val styles = mutableMapOf<String, Any?>()
        var rootComponent: HytaleUiComponent? = null

        val startTime = System.currentTimeMillis()
        while (pos < content.length) {
            if (System.currentTimeMillis() - startTime > 1000) {
                break
            }
            skipWhitespace()
            if (pos >= content.length) break

            when {
                content.startsWith("$", pos) -> {
                    val line = readUntil(";").trim()
                    if (line.contains("=")) {
                        imports.add(line)
                    }
                    consume(";")
                }
                content.startsWith("@", pos) -> {
                    val start = pos
                    val styleName = readIdentifier()
                    // Remove @ from styleName if present
                    val cleanStyleName = if (styleName.startsWith("@")) styleName.substring(1) else styleName
                    skipWhitespace()
                    if (consume("=")) {
                        val styleValue = parseValue(styles)
                        styles[cleanStyleName] = styleValue
                        skipWhitespace()
                        consume(";")
                    } else {
                        pos = start
                        val comp = parseComponent(styles)
                        if (rootComponent == null) {
                            rootComponent = comp
                        }
                    }
                }
                else -> {
                    val start = pos
                    val identifier = readIdentifier()
                    if (identifier.isEmpty() && pos < content.length) {
                        pos++
                    } else if (identifier.isNotEmpty()) {
                        pos = start
                        val comp = parseComponent(styles)
                        if (rootComponent == null) {
                            rootComponent = comp
                        }
                    }
                }
            }
        }

        return HytaleUiFile(imports, styles, rootComponent)
    }

    private fun parseComponent(globalStyles: Map<String, Any?>): HytaleUiComponent {
        val startOffset = pos
        var type = readIdentifier()
        skipWhitespace()
        
        // Handle $C.@CheckBoxWithLabel
        if (type.contains(".@")) {
            type = type.substringAfter(".@")
        } else if (type.startsWith("@")) {
            type = type.substring(1)
        }

        var id: String? = null
        if (consume("#")) {
            id = readIdentifier()
            skipWhitespace()
        }

        val properties = mutableMapOf<String, Any?>()
        val children = mutableListOf<HytaleUiComponent>()

        if (consume("{")) {
            val componentStartTime = System.currentTimeMillis()
            while (pos < content.length && !peek("}")) {
                if (System.currentTimeMillis() - componentStartTime > 500) break // Safety break
                skipWhitespace()
                if (peek("}")) break
                
                // Distinguish between property and child component
                val componentPropStartPos = pos
                val identifier = readIdentifier()
                skipWhitespace()
                
                if (identifier.isEmpty() && pos < content.length) {
                    pos++
                    continue
                }

                if (consume(":") || consume("=")) {
                    // Property or Assignment
                    val isAssignment = content[pos - 1] == '='
                    var value = parseValue(globalStyles)
                    
                    // Resolve variable references
                    value = resolveVariables(value, globalStyles)
                    
                    if (isAssignment) {
                        properties["@$identifier"] = value
                    } else {
                        properties[identifier] = value
                    }
                    skipWhitespace()
                    consume(";")
                } else {
                    // Child component
                    pos = componentPropStartPos
                    children.add(parseComponent(globalStyles))
                }
                skipWhitespace()
            }
            consume("}")
        }
        val endOffset = pos

        return HytaleUiComponent(type, id, properties, children, startOffset, endOffset)
    }

    private fun parseValue(globalStyles: Map<String, Any?> = emptyMap()): Any? {
        skipWhitespace()
        val start = pos
        return when {
            peek("(") -> {
                consume("(")
                val map = mutableMapOf<String, Any?>()
                val mapStartTime = System.currentTimeMillis()
                while (pos < content.length && !peek(")")) {
                    if (System.currentTimeMillis() - mapStartTime > 500) break // Increased safety break
                    skipWhitespace()
                    if (peek(")")) break
                    
                    var key = readIdentifier()
                    skipWhitespace()
                    
                    // Handle @Checked: true in map
                    if (key.startsWith("@")) {
                        // Keep the @ in key for mapping to properties later if needed, 
                        // but let's see how it's used.
                    }

                    if (consume(":") || consume("=")) {
                        var value = parseValue(globalStyles)
                        // Resolve variable references in maps
                        value = resolveVariables(value, globalStyles)
                        map[key] = value
                    } else if (peek("(")) {
                        // Handle constructor-like Style(...) as a value in a map
                        val valStr = readIdentifierOrValue()
                        val mapValue = parseValue(globalStyles)
                        if (mapValue is Map<*, *>) {
                            map[key] = resolveVariables(mapOf(valStr to mapValue), globalStyles)
                        } else {
                            map[key] = mapValue
                        }
                    } else if (key.isNotEmpty()) {
                        // Check if it's just a value (no key: prefix)
                        map["value_${map.size}"] = key
                    } else if (pos < content.length) {
                        pos++
                    }
                    
                    skipWhitespace()
                    if (consume(",")) {
                        skipWhitespace()
                    }
                    if (consume(";")) {
                        skipWhitespace()
                    }
                }
                consume(")")
                
                // Try to map to HytaleUiAnchor if it looks like one
                if (map.containsKey("Width") || map.containsKey("Height") || map.containsKey("Full") || map.containsKey("FlexWeight")) {
                    // Check if it's likely NOT an anchor (e.g., just Full without Width/Height in a context that might be Padding)
                    // But for now, we'll keep the conversion and let the visualizer handle it.
                    // However, we should only convert if it has Anchor-specific keys or is explicitly an Anchor() constructor.
                    HytaleUiAnchor(
                        width = map["Width"]?.toString()?.toIntOrNull() ?: map["@Width"]?.toString()?.toIntOrNull(),
                        height = map["Height"]?.toString()?.toIntOrNull() ?: map["@Height"]?.toString()?.toIntOrNull(),
                        full = map["Full"]?.toString()?.toIntOrNull() ?: map["@Full"]?.toString()?.toIntOrNull(),
                        flexWeight = map["FlexWeight"]?.toString()?.toFloatOrNull() ?: map["@FlexWeight"]?.toString()?.toFloatOrNull()
                    )
                } else {
                    map
                }
            }
            peek("\"") -> {
                consume("\"")
                val str = readUntil("\"")
                consume("\"")
                str
            }
            peek("#") -> {
                val hex = readColor()
                var alpha = 1.0f
                if (peek("(")) {
                    consume("(")
                    alpha = readUntil(")").toFloatOrNull() ?: 1.0f
                    consume(")")
                }
                HytaleUiColor(hex, alpha)
            }
            else -> {
                val valStr = readIdentifierOrValue()
                val result = when {
                    valStr == "true" -> true
                    valStr == "false" -> false
                    valStr.toIntOrNull() != null -> valStr.toInt()
                    else -> valStr
                }
                
                // Check for constructor-like call: Name(properties)
                skipWhitespace()
                if (peek("(")) {
                    val mapValue = parseValue(globalStyles)
                    if (mapValue is Map<*, *>) {
                        return resolveVariables(mapOf(valStr to mapValue), globalStyles)
                    }
                }
                result
            }
        }
    }

    private fun resolveVariables(value: Any?, globalStyles: Map<String, Any?>): Any? {
        return when (value) {
            is String -> {
                if (value.startsWith("@")) {
                    globalStyles[value.substring(1)] ?: value
                } else {
                    value
                }
            }
            is Map<*, *> -> {
                value.mapValues { resolveVariables(it.value, globalStyles) }
            }
            else -> value
        }
    }

    private fun skipWhitespace() {
        while (pos < content.length && content[pos].isWhitespace()) {
            pos++
        }
        // Skip comments (simplified)
        if (pos + 1 < content.length && content[pos] == '/' && content[pos+1] == '/') {
            readUntil("\n")
            skipWhitespace()
        }
    }

    private fun readIdentifier(): String {
        skipWhitespace()
        val start = pos
        while (pos < content.length && (content[pos].isLetterOrDigit() || content[pos] == '_' || content[pos] == '$' || content[pos] == '@' || content[pos] == '.' || content[pos] == '-')) {
            pos++
        }
        return content.substring(start, pos)
    }

    private fun readIdentifierOrValue(): String {
        skipWhitespace()
        val start = pos
        while (pos < content.length && !content[pos].isWhitespace() && content[pos] != '(' && content[pos] != ';' && content[pos] != ':' && content[pos] != '=' && content[pos] != ',' && content[pos] != ')' && content[pos] != '}') {
            pos++
        }
        if (start == pos && pos < content.length) {
            pos++ // Force advance
        }
        return content.substring(start, pos)
    }

    private fun readColor(): String {
        val start = pos
        pos++ // skip #
        while (pos < content.length && content[pos].isLetterOrDigit()) {
            pos++
        }
        return content.substring(start, pos)
    }

    private fun readUntil(stop: String): String {
        val start = pos
        val index = content.indexOf(stop, pos)
        return if (index != -1) {
            pos = index
            content.substring(start, index)
        } else {
            pos = content.length
            content.substring(start)
        }
    }

    private fun consume(expected: String): Boolean {
        skipWhitespace()
        if (content.startsWith(expected, pos)) {
            pos += expected.length
            return true
        }
        return false
    }

    private fun peek(expected: String): Boolean {
        skipWhitespace()
        return content.startsWith(expected, pos)
    }
}
