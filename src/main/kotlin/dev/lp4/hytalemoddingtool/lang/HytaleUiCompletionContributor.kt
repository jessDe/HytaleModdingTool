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
                        "ActionButton", "ActionButtonContainer", "ActionButtonSeparator", "AssetImage", "BackButton",
                        "BackgroundImage", "BlockSelector", "BlockSelectorStyle", "Button", "ButtonStyle",
                        "CenteredTitleLabel", "CharacterPreviewComponent", "CheckBox", "CheckBoxStyle",
                        "CircularProgressBar", "ColorPickerDropdownBoxStyle", "ColorPickerStyle", "ColumnButton",
                        "CompactTextField", "Container", "Content", "ContentSeparator", "DecoratedContainer",
                        "DefaultSpinner", "DestructiveTextButton", "Divider", "DoubleArrowKeyHotkeyRow", "DropdownBox",
                        "DropdownBoxStyle", "EditionCard", "FileDropdownBoxStyle", "FloatSliderNumberField", "Group",
                        "HeaderSearch", "HotkeyLabel", "HotkeyRow", "Icon", "InputFieldStyle", "ItemGrid",
                        "ItemGridStyle", "ItemPreviewComponent", "Label", "LabelAffix", "LabelStyle", "LabeledCheckBox",
                        "Legend", "MenuItem", "MultilineTextField", "NumberField", "OfflineOverlay", "Overlay", "Page",
                        "Pages", "Panel", "PanelSeparatorFancy", "PanelTitle", "PatchStyle", "PlayerPreviewComponent",
                        "PopupMenuLayerStyle", "PrimaryButton", "PrimaryTextButton", "ProgressBar", "ReorderableListGrip",
                        "Row", "RowHintContainer", "RowLabel", "RowLabelContainer", "SceneBlur", "ScrollbarStyle",
                        "SecondaryButton", "SecondaryTextButton", "SectionContainer", "SectionHeader", "Sep",
                        "Separator", "Slider", "SliderNumberField", "SliderStyle", "SmallSecondaryTextButton", "Sprite",
                        "StatNameLabel", "StatNameValueLabel", "Tab", "TabButton", "TabNavigation", "TabNavigationStyle",
                        "TabSeparator", "TagTextButton", "TertiaryTextButton", "TextButton", "TextButtonStyle",
                        "TextField", "TextTooltipStyle", "Title", "TitleLabel", "ToggleButton", "ToolButton",
                        "VerticalActionButtonSeparator", "VerticalSeparator", "Wrapper"
                    )
                    componentTypes.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }

                    val commonProperties = listOf(
                        "ActionName", "Activate", "Alignment", "AllowUnselection", "Anchor", "AreItemsDraggable",
                        "AutoGrow", "AutoScrollDown", "Background", "Bar", "BarTexturePath", "BindingLabelStyle",
                        "Border", "Bottom", "BrokenSlotBackgroundOverlay", "BrokenSlotIconOverlay", "ButtonBackground",
                        "ButtonFill", "ButtonPadding", "ButtonStyle", "Capacity", "ChangedSound", "Checked",
                        "CheckedStyle", "ClearButtonStyle", "CollapseSound", "CollapsedWidth", "Color",
                        "ColorPickerStyle", "ContentPadding", "Count", "Decoration", "Default", "DefaultBackground",
                        "DefaultItemIcon", "DefaultLabelStyle", "Direction", "Disabled", "DraggedHandle",
                        "DurabilityBar", "DurabilityBarAnchor", "DurabilityBarBackground", "EffectHeight",
                        "EffectOffset", "EffectTexturePath", "EffectWidth", "EntryHeight", "EntryIconHeight",
                        "EntryIconWidth", "EntryLabelStyle", "EntrySounds", "ExpandSound", "ExpandedWidth",
                        "FlexWeight", "FontName", "FontSize", "Format", "Frame", "FramesPerSecond", "Full", "Handle",
                        "Height", "HitTestVisible", "Horizontal", "HorizontalAlignment", "HorizontalBorder",
                        "HorizontalPadding", "Hovered", "HoveredBackground", "HoveredHandle", "HoveredLabelStyle",
                        "Icon", "IconAnchor", "IconHeight", "IconOpacity", "IconSelected", "IconTexturePath",
                        "IconWidth", "Id", "Image", "ImageUW", "InfoDisplay", "InputBindingKey", "InputBindingKeyPrefix",
                        "IsReadOnly", "ItemGridStyle", "ItemScale", "ItemStackActivateSound", "KeepScrollPosition",
                        "KeyBindingLabel", "LabelMaskTexturePath", "LabelStyle", "LayoutMode", "Left", "LetterSpacing",
                        "MaskTexturePath", "Max", "MaxLength", "MaxPitch", "MaxSelection", "MaxValue",
                        "MaxVisibleLines", "MaxWidth", "Min", "MinPitch", "MinValue", "MinWidth", "MouseHover",
                        "NumberFieldContainerAnchor", "NumberFieldMaxDecimalPlaces", "NumberFieldStyle", "Offset",
                        "OnlyVisibleWhenHovered", "OpacitySelectorBackground", "OutlineColor", "Padding", "PanelAlign",
                        "PanelScrollbarStyle", "PanelTitleText", "PanelWidth", "PasswordChar", "PerRow",
                        "PlaceholderStyle", "PlaceholderText", "PopupStyle", "Pressed", "PressedBackground",
                        "PressedLabelStyle", "QuantityPopupSlotOverlay", "RenderBold", "RenderItalics",
                        "RenderItemQualityBackground", "RenderUppercase", "Right", "Scale", "ScrollbarStyle",
                        "SelectedButtonStyle", "SelectedEntryIconBackground", "SelectedEntryLabelStyle",
                        "SelectedStyle", "SelectedTab", "SelectedTabStyle", "ShowLabel", "ShowScrollbar", "Size",
                        "SliderStyle", "SlotBackground", "SlotDeleteIcon", "SlotDropIcon", "SlotHoverOverlay",
                        "SlotIconSize", "SlotSize", "SlotSpacing", "SlotsPerRow", "Sounds", "Spacing", "Step", "Style",
                        "TabSounds", "TabStyle", "Text", "TextColor", "TextFieldDecoration", "TextSpans",
                        "TextTooltipStyle", "TexturePath", "TooltipStyle", "TooltipText", "TooltipTextSpans", "Top",
                        "Unchecked", "Value", "Vertical", "VerticalAlignment", "VerticalBorder", "Visible", "Volume",
                        "Width", "Wrap"
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
