package de.soderer.console;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;

import de.soderer.console.ConsoleUtilities.TextAttribute;
import de.soderer.console.utilities.SystemUtilities;
import de.soderer.console.utilities.Utilities;

/**
 * Password input for ANSI consoles, showing a hide character instead of the entered characters, with cursor movement,
 * insert/overwrite mode and paste (Ctrl+V). The input is kept as char array and never as String.
 * In other consoles the password is read via {@link System#console()}.
 */
public class PasswordConsoleInput {
	private boolean silent = false;
	private final char hideCharacter;
	private String allowedCharacters = null;
	private String prompt = "";
	/**
	 * Content the input starts with.
	 */
	protected String presetContent = "";

	/**
	 * Creates a password input showing '*' for each entered character.
	 */
	public PasswordConsoleInput() {
		this('*');
	}

	/**
	 * Creates a password input showing the given character for each entered character.
	 *
	 * @param hideCharacter character shown instead of the entered characters
	 */
	public PasswordConsoleInput(final char hideCharacter) {
		this.hideCharacter = hideCharacter;
	}

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
	 * @return character shown instead of the entered characters
	 */
	public char getHideCharacter() {
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
	public PasswordConsoleInput withPrompt(final String newPrompt) {
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
	public PasswordConsoleInput withPresetContent(final String newPresetContent) {
		setPresetContent(newPresetContent);
		return this;
	}

	/**
	 * Reads a password until Enter is pressed.
	 *
	 * @return entered password, null at the end of the input of a non ANSI console
	 * @throws Exception if the console cannot be read, e.g. if no console is available
	 */
	public char[] readInput() throws Exception {
		if (ConsoleUtilities.getConsoleType() == ConsoleType.ANSI) {
			boolean insertMode = true;
			char[] content = presetContent == null ? "".toCharArray() : presetContent.toCharArray();
			int cursorIndex = content.length;
			boolean inputCompleted = false;

			// Keep the Linux raw mode active for the whole input instead of switching it for every single key.
			// Otherwise keys typed in between would be echoed by the terminal in clear text, and each key would start two 'stty' processes.
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

					// Only hide characters are drawn, the password itself never becomes a String
					if (cursorIndex < content.length) {
						renderer.render(Utilities.repeat(hideCharacter, cursorIndex), Character.toString(hideCharacter), Utilities.repeat(hideCharacter, content.length - cursorIndex - 1), cursorTextAttributes);
					} else {
						renderer.render(Utilities.repeat(hideCharacter, content.length), " ", "", cursorTextAttributes);
					}

					final int keyCode = ConsoleUtilities.readKey();

					if (keyCode == ConsoleUtilities.KeyCode_EndOfInput) {
						throw new EOFException("End of console input");
					} else if (keyCode == ConsoleUtilities.KeyCode_CtrlC) {
						ConsoleUtilities.showCursor();
						System.exit(1);
					} else if (keyCode == ConsoleUtilities.KeyCode_CtrlV) {
						// Line breaks and other control characters are not allowed in a single line input
						final String data = ConsoleUtilities.removeControlCharacters(readClipboardTextOrNull());
						if (data == null) {
							if (!silent) {
								beep();
							}
						} else if (insertMode || cursorIndex == content.length) {
							final char[] newContent = new char[content.length + data.length()];
							for (int i = 0; i < cursorIndex; i++) {
								newContent[i] = content[i];
							}
							final char[] dataArray = data.toCharArray();
							for (int i = 0; i < data.length(); i++) {
								newContent[cursorIndex + i] = dataArray[i];
							}
							for (int i = cursorIndex; i < content.length; i++) {
								newContent[dataArray.length + i] = content[i];
							}
							Utilities.clear(dataArray);
							Utilities.clear(content);
							content = newContent;
							cursorIndex += data.length();
						} else {
							final int restLength = Math.max(content.length - cursorIndex - data.length(), 0);
							final char[] newContent = new char[Math.max(content.length, cursorIndex + data.length())];
							for (int i = 0; i < cursorIndex; i++) {
								newContent[i] = content[i];
							}
							final char[] dataArray = data.toCharArray();
							for (int i = 0; i < data.length(); i++) {
								newContent[cursorIndex + i] = dataArray[i];
							}
							if (restLength > 0) {
								for (int i = cursorIndex + data.length(); i < content.length; i++) {
									newContent[i] = content[i];
								}
							}
							Utilities.clear(dataArray);
							Utilities.clear(content);
							content = newContent;
							cursorIndex += data.length();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Escape) {
						// do nothing
					} else if (keyCode == ConsoleUtilities.KeyCode_Enter) {
						// Remove special displayed cursor
						renderer.renderFinal(Utilities.repeat(hideCharacter, content.length));
						ConsoleUtilities.showCursor();
						inputCompleted = true;
						return content;
					} else if (keyCode == ConsoleUtilities.KeyCode_Backspace) {
						if (cursorIndex > 0) {
							final char[] newContent = new char[content.length - 1];
							for (int i = 0; i < cursorIndex - 1; i++) {
								newContent[i] = content[i];
							}
							for (int i = cursorIndex; i < content.length; i++) {
								newContent[i - 1] = content[i];
							}
							Utilities.clear(content);
							content = newContent;
							cursorIndex--;
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Delete) {
						if (cursorIndex < content.length) {
							final char[] newContent = new char[content.length - 1];
							for (int i = 0; i < cursorIndex; i++) {
								newContent[i] = content[i];
							}
							for (int i = cursorIndex + 1; i < content.length; i++) {
								newContent[i - 1] = content[i];
							}
							Utilities.clear(content);
							content = newContent;
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
						if (cursorIndex < content.length) {
							cursorIndex++;
						} else if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowDown) {
						// do nothing
					} else if (keyCode == ConsoleUtilities.KeyCode_ArrowUp) {
						// do nothing
					} else if (keyCode == ConsoleUtilities.KeyCode_PageDown) {
						if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_PageUp) {
						if (!silent) {
							beep();
						}
					} else if (keyCode == ConsoleUtilities.KeyCode_Home) {
						cursorIndex = 0;
					} else if (keyCode == ConsoleUtilities.KeyCode_End) {
						cursorIndex = content.length;
					} else if (keyCode == ConsoleUtilities.KeyCode_Insert) {
						insertMode = !insertMode;
					} else if (keyCode == ConsoleUtilities.KeyCode_Tab || keyCode == ConsoleUtilities.KeyCode_ReverseTab) {
						if (!silent) {
							beep();
						}
					} else {
						// Control characters and special key codes are excluded, so they cannot silently end up as literal characters in the password.
						if ((allowedCharacters != null && allowedCharacters.contains("" + (char) keyCode)) || ConsoleUtilities.isInsertableCharacter(keyCode)) {
							if (insertMode || cursorIndex == content.length) {
								final char[] newContent = new char[content.length + 1];
								for (int i = 0; i < cursorIndex; i++) {
									newContent[i] = content[i];
								}
								newContent[cursorIndex] = (char) keyCode;
								for (int i = cursorIndex; i < content.length; i++) {
									newContent[1 + i] = content[i];
								}
								Utilities.clear(content);
								content = newContent;
								cursorIndex++;
							} else {
								content[cursorIndex] = (char) keyCode;
								cursorIndex++;
							}
						}
					}
				}
			} finally {
				ConsoleUtilities.showCursor();
				if (!inputCompleted) {
					// Input aborted by an exception: do not leave the partially entered password in memory
					Utilities.clear(content);
				}
				if (linuxRawModeMustBeReset) {
					ConsoleUtilities.deactivateLinuxConsoleRawMode();
				}
			}
		} else {
			return readPasswordViaSystemConsole();
		}
	}

	/**
	 * Fallback input path used for {@link ConsoleType#TEST} and {@link ConsoleType#NO_ANSI}.
	 * Throws a clear exception instead of a NullPointerException when no real console is attached
	 * (e.g. running inside an IDE or with redirected/piped standard input).
	 */
	private char[] readPasswordViaSystemConsole() throws IllegalStateException {
		final java.io.Console console = System.console();
		if (console == null) {
			throw new IllegalStateException("No console available to read input from (e.g. running without a real terminal, or with redirected standard input)");
		}
		return console.readPassword(prompt);
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
	 * image). A failed paste should not abort the input loop or lose an already-entered password.
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
