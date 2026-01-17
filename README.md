# Hytale Modding Tool

An IntelliJ IDEA plugin designed to streamline the modding experience for Hytale. This tool provides advanced language support and a real-time layout preview for Hytale's custom UI system.

## Features

### 🎨 Hytale UI Visualizer
* **Real-time Layout Preview**: Instantly visualize `.ui` files directly within IntelliJ IDEA.
* **Compose-powered Rendering**: Uses JetBrains Compose to accurately render Hytale UI components.
* **Component Support**: Supports standard Hytale UI elements including `Group`, `Label`, `TextButton`, `CheckBox`, and more.
* **Style Resolution**: Fully supports global styles (`@style`), variables, and complex property mapping.
* **Layout Accuracy**: Handles padding, margins, flex weights, and various layout modes (Left, Right, Center, Bottom).

### 📝 Language Support
* **Syntax Highlighting**: Dedicated highlighter for `.ui` files to improve code readability.
* **Code Completion**: Smart completion for Hytale UI properties and component types.
* **Color Provider**: In-editor color previews and pickers for Hytale hex colors.
* **Structure Parsing**: Robust parser that understands Hytale's unique UI definition syntax.

## Getting Started

### Prerequisites
* IntelliJ IDEA (2023.1 or newer recommended)
* Hytale game files (for reference)

### Installation
1. Open IntelliJ IDEA.
2. Go to `Settings` > `Plugins` > `Marketplace`.
3. Search for "Hytale Modding Tool".
4. Click `Install`.

## Usage
Simply open any `.ui` file in your Hytale project. The "Layout" tab will appear at the bottom or side of the editor, allowing you to switch between the text definition and the visual preview.

## Development

This project is built using the IntelliJ Platform Plugin Template.

### Building
To build the plugin and run it in a development instance of IntelliJ:
```bash
./gradlew runIde
```

### Testing
```bash
./gradlew test
```

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request or open an issue for any bugs or feature requests.

## License
This project is licensed under the MIT License - see the LICENSE file for details.