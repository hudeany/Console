# soderer-console

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)

**Interactive console input, menus and ANSI output for Java command line applications.**

`soderer-console` turns a plain terminal into a comfortable user interface: line editing with cursor keys,
auto completion, password input, file path completion, nested menus, colored output and progress bars.
It runs on Linux, macOS and Windows and falls back gracefully when no real terminal is attached (e.g. in an IDE).

```
**************************
* My Application         *
**************************

Main menu

Please choose (Blank => Quit):
  1) Import data
  2) Settings
  0) Quit
 > _

 42% [==================>                         ] 1.2000 GB 35.000 MB/s eta 21s
```

---

## Features

| | |
|---|---|
| **Line editing** | `SimpleConsoleInput` with cursor movement, Home/End, Insert/overwrite mode, Delete/Backspace and clipboard paste (Ctrl+V) |
| **Auto completion** | Tab completes from a list of values, Arrow up/down and Page up/down cycle through them |
| **Password input** | `PasswordConsoleInput` masks the input and returns a `char[]` that can be wiped after use |
| **File path input** | `FilepathConsoleInput` completes files and directories, understands `~` and both `/` and `\` |
| **Menus** | `ConsoleMenu` for nested, numbered menus with colored messages, warnings and errors |
| **ANSI output** | 16 foreground/background colors, text attributes (bold, italic, underline, ...), cursor control |
| **Progress bar** | `getConsoleProgressString` with percentage, human readable amounts, speed and ETA |
| **Terminal info** | Detection of console type, terminal size and cursor position |

## Requirements

- Java 17 or newer
- [JNA](https://github.com/java-native-access/jna) (`net.java.dev.jna:jna`), used on Windows to read single key strokes.
  On Linux and macOS keys are read via `stty`, JNA is not used there.

## Installation

### Maven

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>soderer-console</artifactId>
	<version>VERSION</version>
</dependency>
```

### Gradle

```groovy
implementation "de.soderer:soderer-console:VERSION"
```

### Plain jar

Download `soderer-console-VERSION.jar` from the releases page and put it on the classpath together with the JNA jar.

## Usage

### Reading text

```java
String name = new SimpleConsoleInput()
		.withPrompt("Your name: ")
		.withPresetContent("Andreas")
		.readInput();
```

### Auto completion

```java
String color = new SimpleConsoleInput()
		.withPrompt("Color: ")
		.withAutoCompletionStrings(List.of("red", "green", "blue"))
		.readInput();
```

Type `gr` and press **Tab** to get `green`, or browse all values with **Arrow up/down**.

### Reading a password

```java
char[] password = new PasswordConsoleInput()
		.withPrompt("Password: ")
		.readInput();
try {
	// use the password
} finally {
	Utilities.clear(password); // overwrite the password in memory
}
```

`new PasswordConsoleInput('#')` uses another mask character instead of `*`.

### Selecting a file

```java
String file = new FilepathConsoleInput()
		.withPrompt("Import file: ")
		.withPresetContent("~/data/")
		.readInput();
```

**Tab** completes the current path or steps into a directory, **Page up/down** cycles through the files of the current directory.

### Menus

Sub classes override `show()` to implement their action. Sub menus are added to their parent automatically.

```java
ConsoleMenu mainMenu = new ConsoleMenu("My Application");
new ImportMenu(mainMenu, "Import data");
new SettingsMenu(mainMenu, "Settings");
mainMenu.show();
```

```java
public class ImportMenu extends ConsoleMenu {
	public ImportMenu(ConsoleMenu parentMenu, String title) throws Exception {
		super(parentMenu, title);
	}

	@Override
	public int show() throws Exception {
		// ... do the import ...
		getParentMenu().getMessages().add("Import finished");
		return 0; // back to the parent menu
	}
}
```

Messages, warnings and errors are shown in green, yellow and red the next time a menu is displayed.
Entering nothing or `0` goes back, `q`, `quit` or `exit` ends the application.

### Colored output

```java
System.out.println(ConsoleUtilities.getAnsiColoredText("Done", TextColor.Light_green));
System.out.println(ConsoleUtilities.getAnsiFormattedText("Attention", TextColor.White, TextColor.Red, TextAttribute.HIGH_INTENSITY));
ConsoleUtilities.printBoxed("Welcome");
```

### Progress bar

```java
LocalDateTime start = LocalDateTime.now();
for (long done = 0; done <= total; done += chunk) {
	ConsoleUtilities.saveCurrentCursorPosition();
	System.out.print(ConsoleUtilities.getConsoleProgressString(80, start, total, done, "B"));
	ConsoleUtilities.moveCursorToSavedPosition();
}
```

## Console types

`ConsoleUtilities.getConsoleType()` detects once how the console can be used:

| Type | Environment | Behaviour |
|---|---|---|
| `ANSI` | Real terminal (Linux/macOS with `TERM`, Windows console) | Full line editing, colors and cursor control |
| `TEST` | IDE console, redirected input | Input via `System.console()` without line editing |

Without any console (e.g. piped standard input) `readInput()` throws an `IllegalStateException` with a clear message instead of failing with a `NullPointerException`.

## Building

The project is built with [Apache Ant](https://ant.apache.org/). Dependencies are downloaded automatically.

```sh
ant build_jar
```

This compiles the sources, runs the JUnit tests and creates the following files in `build/`:

- `soderer-console-VERSION.jar`
- `soderer-console-VERSION-sources.jar`
- `soderer-console-VERSION-javadoc.jar`

The version is read from the file `build.version`.

## License

Released under the [MIT License](LICENSE.txt).
