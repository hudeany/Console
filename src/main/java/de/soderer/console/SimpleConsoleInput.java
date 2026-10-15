package de.soderer.console;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

import de.soderer.console.ConsoleUtilities.TextAttribute;
import de.soderer.console.utilities.SystemUtilities;
import de.soderer.console.utilities.Utilities;

/**
 * Line input for ANSI consoles with cursor movement, insert/overwrite mode, paste (Ctrl+V), auto completion
 * (Tab, Page up/down) and optional hidden input. In other consoles the input is read line by line via
 * {@link System#console()}.
 */
public class SimpleConsoleInput {
	/**
	 * Creates an input with default settings.
	 */
	public SimpleConsoleInput() {
		// Default settings
	}

	private boolean silent = false;
	private Character hideCharacter = null;
	private String allowedCharacters = null;
	private String prompt = "";
	/**
	 * Content the input starts with.
	 */
	protected String presetContent = "";
	/**
	 * Values offered for auto completion, null for none.
	 */
	protected List<String> autoCompletionStrings = null;

	/**
	 * When silent = false a system beep occurs on input error
	 *
	 * @return true if no beep occurs on input errors
	 */
	public boolean isSilent() {
		return silent;
	}

	/**
	 * When silent = false a system beep occurs on input error
	 *
	 * @param silent true to suppress the beep on input errors
	 */
	public void setSilent(final boolean silent) {
		this.silent = silent;
	}

	/**
	 * For password inputs
	 *
	 * @return character shown instead of the entered characters, null to show them
	 */
	public Character getHideCharacter() {
		return hideCharacter;
	}

	/**
	 * Get allowed Characters. Default is all chars of german keyboard layout
	 *
	 * @return additionally allowed characters, null for the default
	 */
	public String getAllowedCharacters() {
		return allowedCharacters;
	}

	/**
	 * Set allowed Characters. Default is all chars of german keyboard layout
	 *
	 * @param allowedCharacters additionally allowed characters, null for the default
	 */
	public void setAllowedCharacters(final String allowedCharacters) {
		this.allowedCharacters = allowedCharacters;
	}

	/**
	 * For password inputs
	 *
	 * @param hideCharacter character shown instead of the entered characters, null to show them
	 */
	public void setHideCharacter(final Character hideCharacter) {
		this.hideCharacter = hideCharacter;
	}

	/**
	 * Sets the character shown instead of the entered characters.
	 *
	 * @param newHideCharacter character shown instead of the entered characters, null to show them
	 * @return this input for chaining
	 */
	public SimpleConsoleInput withHideCharacter(final Character newHideCharacter) {
		setHideCharacter(newHideCharacter);
		return this;
	}

	/**
	 * Returns the prompt shown in front of the input.
	 *
	 * @return prompt, never null
	 */
	public String getPrompt() {
		return prompt;
	}

	/**
	 * Sets the prompt shown in front of the input.
	 *
	 * @param prompt prompt, null for none
	 */
	public void setPrompt(final String prompt) {
		this.prompt = (prompt == null ? "" : prompt);
	}

	/**
	 * Sets the prompt shown in front of the input.
	 *
	 * @param newPrompt prompt, null for none
	 * @return this input for chaining
	 */
	public SimpleConsoleInput withPrompt(final String newPrompt) {
		setPrompt(newPrompt);
		return this;
	}

	/**
	 * Returns the content the input starts with.
	 *
	 * @return preset content, never null
	 */
	public String getPresetContent() {
		return presetContent;
	}

	/**
	 * Sets the content the input starts with.
	 *
	 * @param presetContent preset content, null for none
	 */
	public void setPresetContent(final String presetContent) {
		this.presetContent = (presetContent == null ? "" : presetContent);
	}

	/**
	 * Sets the content the input starts with.
	 *
	 * @param newPresetContent preset content, null for none
	 * @return this input for chaining
	 */
	public SimpleConsoleInput withPresetContent(final String newPresetContent) {
		setPresetContent(newPresetContent);
		return this;
	}

	/**
	 * Returns the values offered for auto completion.
	 *
	 * @return auto completion values, may be null
	 */
	public List<String> getAutoCompletionStrings() {
		return autoCompletionStrings;
	}

	/**
	 * Sets the values offered for auto completion.
	 *
	 * @param autoCompletionStrings auto completion values, null for none
	 */
	public void setAutoCompletionStrings(final List<String> autoCompletionStrings) {
		this.autoCompletionStrings = autoCompletionStrings;
	}

	/**
	 * Sets the values offered for auto completion.
	 *
	 * @param newAutoCompletionStrings auto completion values, null for none
	 * @return this input for chaining
	 */
	public SimpleConsoleInput withAutoCompletionStrings(final List<String> newAutoCompletionStrings) {
		setAutoCompletionStrings(newAutoCompletionStrings);
		return this;
	}

	/**
	 * Adds a value offered for auto completion.
	 *
	 * @param autoCompletionString auto completion value
	 * @return this input for chaining
	 */
	public SimpleConsoleInput addAutoCompletionString(final String autoCompletionString) {
		if (autoCompletionStrings == null) {
			autoCompletionStrings = new ArrayList<>();
		}
		autoCompletionStrings.add(autoCompletionString);
		return this;
	}

	/**
	 * Reads a line of input until Enter is pressed.
	 *
	 * @return entered text, null at the end of the input of a non ANSI console
	 * @throws Exception if the console cannot be read, e.g. if no console is available
	 */
	public String readInput() throws Exception {
		if (ConsoleUtilities.getConsoleType() == ConsoleType.ANSI) {
			boolean insertMode = true;
			String content = presetContent == null ? "" : presetContent;
			int cursorIndex = content.length();

			// Keep the Linux raw mode active for the whole input instead of switching it for every single key.
			// Otherwise keys typed in between would be echoed by the terminal, and each key would start two 'stty' processes.
			final boolean linuxRawModeMustBeReset = !SystemUtilities.isWindowsSystem() && ConsoleUtilities.activateLinuxConsoleRawMode();
			try {
				ConsoleUtilities.hideCursor();
				final InputLineRenderer renderer = InputLineRenderer.start(prompt);
				while (true) {
					TextAttribute[] cursorTextAttributes;
					if (insertMode) {
						cursorTextAttributes = new TextAttribute[] {TextAttribute.UNDERLINE, TextAttribute.HIGH_INTENSITY};
					} else {
						cursorTextAttributes = new TextAttribute[] {TextAttribute.REVERSE_VIDEO};
					}

					final String displayedContent = hideCharacter == null ? content : Utilities.repeat(hideCharacter, content.length());
					if (cursorIndex < displayedContent.length()) {
						renderer.render(displayedContent.substring(0, cursorIndex), displayedContent.substring(cursorIndex, cursorIndex + 1), displayedContent.substring(cursorIndex + 1), cursorTextAttributes);
					} else {
						renderer.render(displayedContent, " ", "", cursorTextAttributes);
					}

					final int keyCode = ConsoleUtilities.readKey();

					if (keyCode == ConsoleUtilities.KeyCode_EndOfInput) {
						throw new EOFException("End of console input");
					} else if (keyCode == ConsoleUtilities.KeyCode_CtrlC) {
						ConsoleUtilities.showCursor();
						System.exit(1);
					} else if (keyCode == ConsoleUtilities.KeyCode_CtrlV) {
						// Line breaks and other control characters are not allowed in a single line input
						final String clipboardText = ConsoleUtilities.removeControlCharacters(readClipboardTextOrNull());
						final String data = clipboardText == null ? null : normalizeInsertedText(clipboardText);
						if (data == null) {
							if (!silent) {
								beep();
							}
						} else if (insertMode || cursorIndex == content.length()) {
							content = content.substring(0, cursorIndex) + data + content.substring(cursorIndex);
							cursorIndex += data.length();
						} else {
							final String rest = content.substring(cursorIndex);
							content = content.substring(0, cursorIndex) + data + (rest.length() > data.length() ? rest.substring(data.length()) : "");
							cursorIndex += data.length();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Escape) {
						// do nothing
					} else if (keyCode == ConsoleUtilities.KeyCode_Enter) {
						// Remove special displayed cursor. Never show hidden input in clear text.
						renderer.renderFinal(hideCharacter == null ? content : Utilities.repeat(hideCharacter, content.length()));
						ConsoleUtilities.showCursor();
						return content;
					} else if (keyCode == ConsoleUtilities.KeyCode_Backspace) {
						if (cursorIndex > 0) {
							content = content.substring(0, cursorIndex - 1) + content.substring(cursorIndex);
							cursorIndex--;
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Delete) {
						if (cursorIndex < content.length()) {
							content = content.substring(0, cursorIndex) + content.substring(cursorIndex + 1);
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowLeft) {
						if (cursorIndex > 0) {
							cursorIndex--;
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowRight) {
						if (cursorIndex < content.length()) {
							cursorIndex++;
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowDown) {
						final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
						if (autoCompletionStringsToCheck != null && autoCompletionStringsToCheck.size() > 0) {
							int currentIndexOfAutocompletion = -1;
							for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
								if (autoCompletionStringsToCheck.get(i) != null && isSameAutoCompletionValue(autoCompletionStringsToCheck.get(i), content)) {
									currentIndexOfAutocompletion = i;
									break;
								}
							}

							if (currentIndexOfAutocompletion == -1) {
								currentIndexOfAutocompletion = autoCompletionStringsToCheck.size() - 1;
							}

							int nextIndexOfAutocompletion;
							if (currentIndexOfAutocompletion < (autoCompletionStringsToCheck.size() - 1)) {
								nextIndexOfAutocompletion = currentIndexOfAutocompletion + 1;
							} else {
								nextIndexOfAutocompletion = 0;
							}

							content = autoCompletionStringsToCheck.get(nextIndexOfAutocompletion);
							cursorIndex = content.length();
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowUp || keyCode == ConsoleUtilities.KeyCode_ReverseTab) {
						// Shift+Tab steps backward through the auto completion values like Arrow up. The key code is passed on,
						// so subclasses can offer different values for it (e.g. the parent directory in FilepathConsoleInput).
						final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
						if (autoCompletionStringsToCheck != null && autoCompletionStringsToCheck.size() > 0) {
							int currentIndexOfAutocompletion = -1;
							for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
								if (autoCompletionStringsToCheck.get(i) != null && isSameAutoCompletionValue(autoCompletionStringsToCheck.get(i), content)) {
									currentIndexOfAutocompletion = i;
									break;
								}
							}

							if (currentIndexOfAutocompletion == -1) {
								currentIndexOfAutocompletion = 0;
							}

							int nextIndexOfAutocompletion;
							if (currentIndexOfAutocompletion > 0) {
								nextIndexOfAutocompletion = currentIndexOfAutocompletion - 1;
							} else {
								nextIndexOfAutocompletion = autoCompletionStringsToCheck.size() - 1;
							}

							content = autoCompletionStringsToCheck.get(nextIndexOfAutocompletion);
							cursorIndex = content.length();
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_PageDown) {
						final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
						if (autoCompletionStringsToCheck != null && autoCompletionStringsToCheck.size() > 0) {
							int autoCompletionIndex = -1;
							for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
								if (autoCompletionStringsToCheck.get(i) != null && isSameAutoCompletionValue(autoCompletionStringsToCheck.get(i), content)) {
									autoCompletionIndex = i;
									break;
								}
							}
							if (autoCompletionIndex == autoCompletionStringsToCheck.size() - 1) {
								content = autoCompletionStringsToCheck.get(0);
								cursorIndex = content.length();
							} else if (autoCompletionIndex >= 0) {
								content = autoCompletionStringsToCheck.get(autoCompletionIndex + 1);
								cursorIndex = content.length();
							} else if (!silent) {
								beep();
							}
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_PageUp) {
						final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
						if (autoCompletionStringsToCheck != null && autoCompletionStringsToCheck.size() > 0) {
							int autoCompletionIndex = -1;
							for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
								if (autoCompletionStringsToCheck.get(i) != null && isSameAutoCompletionValue(autoCompletionStringsToCheck.get(i), content)) {
									autoCompletionIndex = i;
									break;
								}
							}
							if (autoCompletionIndex == 0) {
								content = autoCompletionStringsToCheck.get(autoCompletionStringsToCheck.size() - 1);
								cursorIndex = content.length();
							} else if (autoCompletionIndex > 0) {
								content = autoCompletionStringsToCheck.get(autoCompletionIndex - 1);
								cursorIndex = content.length();
							} else if (!silent) {
								beep();
							}
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Home) {
						cursorIndex = 0;
					} else if (keyCode == ConsoleUtilities.KeyCode_End) {
						cursorIndex = content.length();
					} else if (keyCode == ConsoleUtilities.KeyCode_Insert) {
						insertMode = !insertMode;
					} else if (keyCode == ConsoleUtilities.KeyCode_Tab) {
						final String prefix = content.substring(0, cursorIndex);
						final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(prefix, keyCode);
						if (autoCompletionStringsToCheck != null) {
							String autoCompletionStringCandidate = null;
							for (final String autoCompletionString : autoCompletionStringsToCheck) {
								if (autoCompletionString != null && matchesAutoCompletionPrefix(autoCompletionString, prefix)) {
									if (autoCompletionStringCandidate == null) {
										autoCompletionStringCandidate = autoCompletionString;
									} else {
										// Ambiguous: beeped once below
										autoCompletionStringCandidate = null;
										break;
									}
								}
							}
							if (autoCompletionStringCandidate != null) {
								content = autoCompletionStringCandidate + content.substring(cursorIndex);
								cursorIndex = autoCompletionStringCandidate.length();
							} else if (!silent) {
								beep();
							}
						} else if (!silent) {
							beep();
						}
					} else {
						// Control characters and special key codes are excluded, so they cannot silently end up as literal characters in the input.
						if ((allowedCharacters != null && allowedCharacters.contains("" + (char) keyCode)) || ConsoleUtilities.isInsertableCharacter(keyCode)) {
							final String insertedText = normalizeInsertedText(Character.toString((char) keyCode));
							if (insertMode || cursorIndex == content.length()) {
								content = content.substring(0, cursorIndex) + insertedText + content.substring(cursorIndex);
							} else {
								final String rest = content.substring(cursorIndex);
								content = content.substring(0, cursorIndex) + insertedText + (rest.length() > insertedText.length() ? rest.substring(insertedText.length()) : "");
							}
							cursorIndex += insertedText.length();
						}
					}
				}
			} finally {
				ConsoleUtilities.showCursor();
				if (linuxRawModeMustBeReset) {
					ConsoleUtilities.deactivateLinuxConsoleRawMode();
				}
			}
		} else {
			return readViaSystemConsole();
		}
	}

	/**
	 * Fallback input path used for {@link ConsoleType#TEST} and {@link ConsoleType#NO_ANSI}.
	 * Throws a clear exception instead of a NullPointerException when no real console is attached
	 * (e.g. running inside an IDE or with redirected/piped standard input).
	 */
	private String readViaSystemConsole() throws IllegalStateException {
		final java.io.Console console = System.console();
		if (console == null) {
			throw new IllegalStateException("No console available to read input from (e.g. running without a real terminal, or with redirected standard input)");
		}
		if (hideCharacter != null) {
			// readPassword returns null at the end of the input stream
			final char[] password = console.readPassword(prompt);
			return password == null ? null : new String(password);
		} else {
			return console.readLine(prompt);
		}
	}

	/**
	 * Override hook for other autoComplete functions
	 *
	 * @param checkContent current input content
	 * @param keyCode key triggering the auto completion (Tab, Page up or Page down)
	 * @return values to offer for auto completion, may be null
	 */
	protected List<String> getAutoCompletionStrings(final String checkContent, final int keyCode) {
		return autoCompletionStrings;
	}

	/**
	 * Override hook to normalize text before it is inserted into the input, either typed or pasted.
	 * Used e.g. by {@link FilepathConsoleInput} to convert '/' into the Windows path separator, so the content always
	 * matches the auto completion values, which use the system's path separator.
	 *
	 * @param text text to insert, never null
	 * @return text to insert instead, never null
	 */
	@SuppressWarnings("static-method")
	protected String normalizeInsertedText(final String text) {
		return text;
	}

	/**
	 * Override hook deciding whether an auto completion value matches the text in front of the cursor on Tab.
	 * Used e.g. by {@link FilepathConsoleInput} for case insensitive matching on Windows.
	 *
	 * @param autoCompletionString auto completion value, never null
	 * @param prefix text in front of the cursor
	 * @return true if the value completes the prefix
	 */
	@SuppressWarnings("static-method")
	protected boolean matchesAutoCompletionPrefix(final String autoCompletionString, final String prefix) {
		return autoCompletionString.startsWith(prefix);
	}

	/**
	 * Override hook deciding whether the current content equals an auto completion value, to find the current position
	 * when stepping through the values with Arrow up/down and Page up/down.
	 * Used e.g. by {@link FilepathConsoleInput} for case insensitive comparison on Windows.
	 *
	 * @param autoCompletionString auto completion value, never null
	 * @param content current input content
	 * @return true if both are the same value
	 */
	@SuppressWarnings("static-method")
	protected boolean isSameAutoCompletionValue(final String autoCompletionString, final String content) {
		return autoCompletionString.equals(content);
	}

	/**
	 * Beeps via AWT, ignoring failures. On a headless system (e.g. a server without a graphical environment)
	 * {@link Toolkit#getDefaultToolkit()} can throw a HeadlessException/AWTError - a missed beep is not worth
	 * aborting the whole input loop over.
	 */
	private static void beep() {
		try {
			Toolkit.getDefaultToolkit().beep();
		} catch (@SuppressWarnings("unused") final Throwable t) {
			// do nothing
		}
	}

	/**
	 * Reads plain text from the system clipboard, or returns null if that is not possible: no AWT/graphical
	 * environment available (headless), no clipboard access, or the clipboard content is not text (e.g. an
	 * image). A failed paste should not abort the input loop or lose already-typed content.
	 */
	private static String readClipboardTextOrNull() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		} catch (@SuppressWarnings("unused") final Throwable t) {
			return null;
		}
	}
}
