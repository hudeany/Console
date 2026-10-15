package de.soderer.console;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import de.soderer.console.utilities.DateUtilities;
import de.soderer.console.utilities.SystemUtilities;
import de.soderer.console.utilities.Utilities;

/**
 * Static helper methods for ANSI terminals: reading single keys, cursor positioning, colored text, terminal size
 * detection and progress bars.
 * <p>
 * Key codes returned by {@link #readKey()} are the values of the KeyCode_* constants for special keys and the
 * character code for all other keys.
 */
public class ConsoleUtilities {
	private ConsoleUtilities() {
		// Only static methods
	}

	private static volatile ConsoleType consoleType = null;
	private static volatile boolean linuxConsoleActivatedRawMode = false;

	/**
	 * Key code of Ctrl+C.
	 */
	public static final int KeyCode_CtrlC = 3;
	/**
	 * Key code of Ctrl+V (paste).
	 */
	public static final int KeyCode_CtrlV = 22;
	/**
	 * Key code of Ctrl+X.
	 */
	public static final int KeyCode_CtrlX = 24;

	/**
	 * Key code of Ctrl+Home.
	 */
	public static final int KeyCode_CtrlHome = 57463;
	/**
	 * Key code of Ctrl+End.
	 */
	public static final int KeyCode_CtrlEnd = 57461;
	/**
	 * Key code of Ctrl+Arrow up.
	 */
	public static final int KeyCode_CtrlArrowUp = 57485;
	/**
	 * Key code of Ctrl+Arrow left.
	 */
	public static final int KeyCode_CtrlArrowLeft = 57459;
	/**
	 * Key code of Ctrl+Arrow right.
	 */
	public static final int KeyCode_CtrlArrowRight = 57460;
	/**
	 * Key code of Ctrl+Arrow down.
	 */
	public static final int KeyCode_CtrlArrowDown = 57489;

	/**
	 * Key code of Enter.
	 */
	public static final int KeyCode_Enter = 13;
	/**
	 * Key code of Backspace.
	 */
	public static final int KeyCode_Backspace = 8;
	/**
	 * Key code of Tab.
	 */
	public static final int KeyCode_Tab = 9;
	/**
	 * Key code of Shift+Tab (reverse tab): 0xE000 + scan code 0x0F, the same scheme as the other special keys on Windows.
	 * Linux terminals send the escape sequence "ESC [ Z", which is mapped to this code.
	 * <p>
	 * Before, this was -1, which could not be distinguished from {@link #KeyCode_EndOfInput}.
	 */
	public static final int KeyCode_ReverseTab = 57359;
	/**
	 * Returned at the end of the input, e.g. at the end of redirected standard input.
	 * Input loops must stop reading when they get this code.
	 */
	public static final int KeyCode_EndOfInput = -1;
	/**
	 * Key code of Escape.
	 */
	public static final int KeyCode_Escape = 27;

	/**
	 * Key code of Home.
	 */
	public static final int KeyCode_Home = 57415;
	/**
	 * Key code of Page up.
	 */
	public static final int KeyCode_PageUp = 57417;
	/**
	 * Key code of End.
	 */
	public static final int KeyCode_End = 57423;
	/**
	 * Key code of Insert.
	 */
	public static final int KeyCode_Insert = 57426;
	/**
	 * Key code of Delete.
	 */
	public static final int KeyCode_Delete = 57427;
	/**
	 * Key code of Page down.
	 */
	public static final int KeyCode_PageDown = 57425;

	/**
	 * Key code of Arrow up.
	 */
	public static final int KeyCode_ArrowUp = 57416;
	/**
	 * Key code of Arrow left.
	 */
	public static final int KeyCode_ArrowLeft = 57419;
	/**
	 * Key code of Arrow right.
	 */
	public static final int KeyCode_ArrowRight = 57421;
	/**
	 * Key code of Arrow down.
	 */
	public static final int KeyCode_ArrowDown = 57424;

	/**
	 * Key code of F1 (0xE000 + scan code 0x3B, as on Windows).
	 */
	public static final int KeyCode_F1 = 57403;

	/**
	 * Key code of F2 (0xE000 + scan code 0x3C, as on Windows).
	 */
	public static final int KeyCode_F2 = 57404;

	/**
	 * Key code of F3 (0xE000 + scan code 0x3D, as on Windows).
	 */
	public static final int KeyCode_F3 = 57405;

	/**
	 * Key code of F4 (0xE000 + scan code 0x3E, as on Windows).
	 */
	public static final int KeyCode_F4 = 57406;

	/**
	 * Key code of F5 (0xE000 + scan code 0x3F, as on Windows).
	 */
	public static final int KeyCode_F5 = 57407;

	/**
	 * Key code of F6 (0xE000 + scan code 0x40, as on Windows).
	 */
	public static final int KeyCode_F6 = 57408;

	/**
	 * Key code of F7 (0xE000 + scan code 0x41, as on Windows).
	 */
	public static final int KeyCode_F7 = 57409;

	/**
	 * Key code of F8 (0xE000 + scan code 0x42, as on Windows).
	 */
	public static final int KeyCode_F8 = 57410;

	/**
	 * Key code of F9 (0xE000 + scan code 0x43, as on Windows).
	 */
	public static final int KeyCode_F9 = 57411;

	/**
	 * Key code of F10 (0xE000 + scan code 0x44, as on Windows).
	 */
	public static final int KeyCode_F10 = 57412;

	/**
	 * Key code of F11 (0xE000 + scan code 0x85, as on Windows).
	 */
	public static final int KeyCode_F11 = 57477;

	/**
	 * Key code of F12 (0xE000 + scan code 0x86, as on Windows).
	 */
	public static final int KeyCode_F12 = 57478;

	/**
	 * ANSI colors for foreground and background of text.
	 * <p>
	 * Additional attributes:
	 * Bold 1,
	 * Underline 4,
	 * No underline 24,
	 * Negative (reverse the foreground and background) 7,
	 * Positive (no negative) 27,
	 * Default 0
	 */
	public enum TextColor {
		/**
		 * Black.
		 */
		Black(30, 40),
		/**
		 * Red.
		 */
		Red(31, 41),
		/**
		 * Green.
		 */
		Green(32, 42),
		/**
		 * Yellow.
		 */
		Yellow(33, 43),
		/**
		 * Blue.
		 */
		Blue(34, 44),
		/**
		 * Magenta.
		 */
		Magenta(35, 45),
		/**
		 * Cyan.
		 */
		Cyan(36, 46),
		/**
		 * Light gray.
		 */
		Light_gray(37, 47),
		/**
		 * Dark gray.
		 */
		Dark_gray(90, 100),
		/**
		 * Light red.
		 */
		Light_red(91, 101),
		/**
		 * Light green.
		 */
		Light_green(92, 102),
		/**
		 * Light yellow.
		 */
		Light_yellow(93, 103),
		/**
		 * Light blue.
		 */
		Light_blue(94, 104),
		/**
		 * Light magenta.
		 */
		Light_magenta(95, 105),
		/**
		 * Light cyan.
		 */
		Light_cyan(96, 106),
		/**
		 * White.
		 */
		White(97, 107);

		private final int foreGroundColorCode;
		private final int backGroundColorCode;

		TextColor(final int foreGroundColorCode, final int backGroundColorCode) {
			this.foreGroundColorCode = foreGroundColorCode;
			this.backGroundColorCode = backGroundColorCode;
		}

		/**
		 * Returns the ANSI code of this color as foreground color.
		 *
		 * @return ANSI foreground color code
		 */
		public int getForeGroundColorCode() {
			return foreGroundColorCode;
		}

		/**
		 * Returns the ANSI code of this color as background color.
		 *
		 * @return ANSI background color code
		 */
		public int getBackGroundColorCode() {
			return backGroundColorCode;
		}
	}

	/**
	 * ANSI text attributes.
	 */
	public enum TextAttribute {
		/**
		 * Reset Text decoration
		 */
		RESET("\033[0m"),

		/**
		 * Bold or high intensity text.
		 */
		HIGH_INTENSITY("\033[1m"),

		/**
		 * Not supported by Windows console
		 */
		LOW_INTENSITY("\033[2m"),

		/**
		 * Not supported by Windows console
		 */
		ITALIC("\033[3m"),

		/**
		 * Underlined text.
		 */
		UNDERLINE("\033[4m"),

		/**
		 * Not supported by Windows console
		 */
		BLINK("\033[5m"),

		/**
		 * Not supported by Windows console
		 */
		RAPID_BLINK("\033[6m"),

		/**
		 * Swapped foreground and background colors.
		 */
		REVERSE_VIDEO("\033[7m"),

		/**
		 * Invisible text.
		 */
		INVISIBLE_TEXT("\033[8m");

		private final String ansiCode;

		TextAttribute(final String ansiCode) {
			this.ansiCode = ansiCode;
		}

		/**
		 * Returns the ANSI escape sequence of this attribute.
		 *
		 * @return ANSI escape sequence
		 */
		public String getAnsiCode() {
			return ansiCode;
		}
	}

	/**
	 * Detects the type of the console once: ANSI for Windows consoles and terminals with a TERM variable, whose size
	 * can be detected, TEST otherwise (e.g. IDE consoles).
	 *
	 * @return detected console type
	 */
	public static ConsoleType getConsoleType() {
		final ConsoleType detectedConsoleType = consoleType;
		if (detectedConsoleType != null) {
			return detectedConsoleType;
		}
		// Synchronized, so concurrent first calls do not run the detection (and its terminal requests) twice
		synchronized (ConsoleUtilities.class) {
			if (consoleType == null) {
				consoleType = detectConsoleType();
			}
			return consoleType;
		}
	}

	private static ConsoleType detectConsoleType() {
		final boolean ansiCandidate;
		if (SystemUtilities.isWindowsSystem()) {
			// Without a real console on stdin and stdout (redirected streams, IDE console) the detection below would
			// consume redirected input and write escape sequences into redirected output
			ansiCandidate = isWindowsConsoleAttached();
		} else {
			final String term = System.getenv("TERM");
			ansiCandidate = isInteractiveSystemConsole() && term != null && !"dumb".equalsIgnoreCase(term);
		}

		if (ansiCandidate) {
			try {
				// The console only counts as ANSI console, if it answers the request for the cursor position in time
				readTerminalSize();
				return ConsoleType.ANSI;
			} catch (@SuppressWarnings("unused") final Throwable t) {
				return ConsoleType.TEST;
			}
		} else {
			return ConsoleType.TEST;
		}
	}

	private static boolean isWindowsConsoleAttached() {
		try {
			return WindowsKeyStrokeReader.isConsoleAttached();
		} catch (@SuppressWarnings("unused") final Throwable t) {
			// e.g. JNA not available
			return false;
		}
	}

	/**
	 * Since Java 22 System.console() also returns a Console object for redirected streams, so Console.isTerminal()
	 * must be checked additionally. It is called via reflection to stay compatible with older Java versions.
	 */
	private static boolean isInteractiveSystemConsole() {
		final java.io.Console console = System.console();
		if (console == null) {
			return false;
		}
		try {
			final Object isTerminal = java.io.Console.class.getMethod("isTerminal").invoke(console);
			return Boolean.TRUE.equals(isTerminal);
		} catch (@SuppressWarnings("unused") final NoSuchMethodException e) {
			// Java 21 and older: a non-null console is always an interactive terminal
			return true;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return true;
		}
	}

	/**
	 * Switches the Linux terminal into raw mode without echo via 'stty', so single keys can be read.
	 * Output post-processing stays active, so "\n" still starts a new line at column 1 while raw mode is active.
	 * A shutdown hook resets the mode on exit.
	 *
	 * @return true if the mode was switched, false if it was already active
	 * @throws Exception if 'stty' fails
	 */
	public static synchronized boolean activateLinuxConsoleRawMode() throws Exception {
		if (!linuxConsoleActivatedRawMode) {
			final int exitCode = executeStty("raw -echo opost");
			if (exitCode != 0) {
				throw new Exception("Cannot activate linux console raw mode: 'stty' returned exit code " + exitCode);
			}
			linuxConsoleActivatedRawMode = true;
			registerShutdownHookOnce();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Switches the Linux terminal back from raw mode to normal line mode with echo via 'stty'.
	 *
	 * @throws Exception if 'stty' fails
	 */
	public static synchronized void deactivateLinuxConsoleRawMode() throws Exception {
		if (linuxConsoleActivatedRawMode) {
			final int exitCode = executeStty("-raw echo");
			// Mark as deactivated even on failure, so we don't get stuck retrying a broken 'stty' on every subsequent call
			linuxConsoleActivatedRawMode = false;
			if (exitCode != 0) {
				throw new Exception("Cannot deactivate linux console raw mode: 'stty' returned exit code " + exitCode);
			}
		}
	}

	/**
	 * Executes 'stty' on the controlling terminal and closes all streams of the process afterwards.
	 */
	private static int executeStty(final String sttyArguments) throws Exception {
		final Process process = new ProcessBuilder("/bin/sh", "-c", "stty " + sttyArguments + " < /dev/tty").redirectErrorStream(true).start();
		try (InputStream processOutput = process.getInputStream()) {
			process.getOutputStream().close();
			// Drain the output, so the process cannot block on a full pipe
			processOutput.readAllBytes();
			return process.waitFor();
		}
	}

	private static volatile boolean shutdownHookRegistered = false;

	/**
	 * Registers the shutdown hook resetting the raw mode only once, no matter how often the raw mode is activated.
	 */
	private static synchronized void registerShutdownHookOnce() {
		if (!shutdownHookRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread("ConsoleRawModeReset") {
				@Override
				public void run() {
					try {
						deactivateLinuxConsoleRawMode();
					} catch (@SuppressWarnings("unused") final Exception e) {
						// do nothing
					}
				}
			});
			shutdownHookRegistered = true;
		}
	}

	/**
	 * Checks whether a key code read by {@link #readKey()} is a character that may be inserted into a text input.
	 * Control characters (including DEL) and the key codes of special keys (0xE000 and above) are excluded, as well
	 * as code points of 0x2000 and above (to prevent Chinese symbols on some systems, that occur without intention),
	 * except for the Euro sign.
	 *
	 * @param keyCode key code to check
	 * @return true if the key code is an insertable character
	 */
	public static boolean isInsertableCharacter(final int keyCode) {
		if (keyCode == '\u20AC') {
			return true;
		} else {
			return keyCode >= 0x20 && keyCode < 0x2000 && !Character.isISOControl(keyCode);
		}
	}

	/**
	 * Removes all control characters (e.g. line breaks, tabs and escape sequence introducers) from a text,
	 * e.g. for pasted clipboard content in a single line input.
	 *
	 * @param text text to clean, may be null
	 * @return text without control characters, or null for null input
	 */
	public static String removeControlCharacters(final String text) {
		if (text == null) {
			return null;
		}
		final StringBuilder result = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			final char character = text.charAt(i);
			if (!Character.isISOControl(character)) {
				result.append(character);
			}
		}
		return result.toString();
	}

	/**
	 * Create a progress string for terminal output e.g.: "65% [=================================&gt;                   ] 103.234 200/s eta 5m"
	 *
	 * @param lineLength total length of the progress string
	 * @param start start time of the work
	 * @param itemsToDo total number of items, 0 or below if unknown
	 * @param itemsDone number of items done
	 * @param itemUnitSign unit of the items for display, may be null
	 * @return progress string of the given length (or longer, if the info texts do not fit)
	 */
	public static String getConsoleProgressString(final int lineLength, final LocalDateTime start, final long itemsToDo, final long itemsDone, final String itemUnitSign) {
		final LocalDateTime now = LocalDateTime.now();
		String itemsToDoString = "??";
		String percentageString = " 0%";
		String speedString = "???/s";
		String etaString = "eta ???";
		int percentageDone = 0;

		if (itemsToDo > 0) {
			itemsToDoString = Utilities.getHumanReadableNumber(itemsToDo, itemUnitSign, true, 5, true, Locale.ENGLISH);

			if (itemsDone > 0) {
				percentageDone = (int) (itemsDone * 100 / itemsToDo);
				percentageString = Utilities.leftPad(percentageDone + "%", 3);
				long elapsedSeconds = Duration.between(start, now).toSeconds();
				// Prevent division by zero, when start is fast
				if (elapsedSeconds == 0) {
					elapsedSeconds = 1;
				}
				final int speed = (int) (itemsDone / elapsedSeconds);
				speedString = Utilities.getHumanReadableNumber(speed, "", true, 5, true, Locale.ENGLISH) + (itemUnitSign == null ? "" : itemUnitSign) + "/s";
				final LocalDateTime estimatedEnd = DateUtilities.calculateETA(start, itemsToDo, itemsDone);
				// No estimation possible e.g. for more items done than expected or a start time in the future
				if (estimatedEnd != null) {
					etaString = "eta " + DateUtilities.getShortHumanReadableTimespan(Duration.between(now, estimatedEnd), false, true);
				}
			}
		}

		final String leftPart = percentageString + " [";
		final String rightPart = "] " + itemsToDoString + " " + speedString + " " + etaString;
		final int barWith = Math.max(0, lineLength - (leftPart.length() + rightPart.length()));
		if (barWith == 0) {
			// Not enough space left for a bar, only render percentage and info parts (rightPart starts with "] ")
			return percentageString + rightPart.substring(1);
		}
		int barDone = barWith * percentageDone / 100;
		if (barDone < 1) {
			barDone = 1;
		} else if (barDone >= barWith) {
			barDone = barWith;
		}
		return leftPart + Utilities.repeat("=", barDone - 1) + ">" + Utilities.repeat(" ", barWith - barDone) + rightPart;
	}

	/**
	 * Prints a text framed by '*' characters.
	 *
	 * @param text text to print, nothing is printed for null
	 */
	public static void printBoxed(final String text) {
		printBoxed(text, '*');
	}

	/**
	 * Prints a text framed by the given character.
	 *
	 * @param text text to print, nothing is printed for null
	 * @param boxChar character of the frame
	 */
	public static void printBoxed(final String text, final char boxChar) {
		if (text != null) {
			System.out.println(Utilities.repeat(boxChar, text.length() + 4));
			System.out.println(boxChar + " " + text + " " + boxChar);
			System.out.println(Utilities.repeat(boxChar, text.length() + 4));
		}
	}

	/**
	 * Clears the console screen ("cls" on Windows, ANSI sequence otherwise).
	 */
	public static void clearScreen() {
		if (SystemUtilities.isWindowsSystem()) {
			try {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} catch (@SuppressWarnings("unused") final Exception e) {
				System.err.println("Cannot clear Windows console");
			}
		} else {
			System.out.print("\033[H\033[2J");
			System.out.flush();
		}
	}

	/**
	 * Returns a text with ANSI codes for the given foreground color, followed by a reset.
	 *
	 * @param text text to color
	 * @param foreGroundColor foreground color, null for none
	 * @return text with ANSI codes
	 */
	public static String getAnsiColoredText(final String text, final TextColor foreGroundColor) {
		return getAnsiFormattedText(text, foreGroundColor, null);
	}

	/**
	 * Returns a text with ANSI codes for the given colors, followed by a reset.
	 *
	 * @param foreGroundColor foreground color, null for none
	 * @param backGroundColor background color, null for none
	 * @param text text to color
	 * @return text with ANSI codes
	 */
	public static String getAnsiColoredText(final TextColor foreGroundColor, final TextColor backGroundColor, final String text) {
		return getAnsiFormattedText(text, foreGroundColor, backGroundColor);
	}

	/**
	 * Returns a text with ANSI codes for the given colors and attributes, followed by a reset.
	 *
	 * @param text text to format
	 * @param foreGroundColor foreground color, null for none
	 * @param backGroundColor background color, null for none
	 * @param attributes text attributes, may be empty
	 * @return text with ANSI codes
	 */
	public static String getAnsiFormattedText(final String text, final TextColor foreGroundColor, final TextColor backGroundColor, final TextAttribute... attributes) {
		final StringBuilder builder = new StringBuilder();
		if (foreGroundColor != null) {
			builder.append("\033[" + foreGroundColor.getForeGroundColorCode() + "m");
		}
		if (backGroundColor != null) {
			builder.append("\033[" + backGroundColor.getBackGroundColorCode() + "m");
		}
		if (attributes != null) {
			for (final TextAttribute attribute : attributes) {
				builder.append(attribute.getAnsiCode());
			}
		}
		builder.append(text);
		builder.append(TextAttribute.RESET.getAnsiCode());
		return builder.toString();
	}

	/**
	 * Saves the current cursor position (ANSI).
	 */
	public static void saveCurrentCursorPosition() {
		System.out.print("\033[s");
	}

	/**
	 * Moves the cursor to a position (ANSI).
	 *
	 * @param line 1-based line
	 * @param column 1-based column
	 */
	public static void moveCursorToPosition(final int line, final int column) {
		System.out.print("\033[" + line + ";" + column + "H");
	}

	/**
	 * Moves the cursor back to the position saved by {@link #saveCurrentCursorPosition()} (ANSI).
	 */
	public static void moveCursorToSavedPosition() {
		System.out.print("\033[u");
	}

	/**
	 * Clears the current line from the cursor to its end (ANSI).
	 */
	public static void clearLineContentAfterCursor() {
		System.out.print("\033[K");
	}

	/**
	 * Hides the cursor (ANSI).
	 */
	public static void hideCursor() {
		System.out.print("\033[?25l");
	}

	/**
	 * Shows the cursor again (ANSI).
	 */
	public static void showCursor() {
		System.out.print("\033[?25h");
	}

	/**
	 * Parses an ANSI cursor position report of the form "...[<first>;<second>R" out of the given response buffer.
	 * Validates that all expected markers are present before extracting substrings, so a malformed or unexpected
	 * terminal response results in a clear exception instead of a StringIndexOutOfBoundsException / NumberFormatException.
	 */
	private static int[] parseAnsiPositionResponse(final StringBuilder response, final String errorContext) throws Exception {
		final int bracketIndex = response.lastIndexOf("[");
		final int semicolonIndex = bracketIndex > -1 ? response.indexOf(";", bracketIndex) : -1;
		final int terminatorIndex = semicolonIndex > -1 ? response.indexOf("R", semicolonIndex) : -1;
		if (bracketIndex == -1 || semicolonIndex == -1 || terminatorIndex == -1) {
			throw new Exception(errorContext + ": unexpected terminal response '" + response + "'");
		}
		try {
			final int first = Integer.parseInt(response.substring(bracketIndex + 1, semicolonIndex));
			final int second = Integer.parseInt(response.substring(semicolonIndex + 1, terminatorIndex));
			return new int[] { first, second };
		} catch (final NumberFormatException e) {
			throw new Exception(errorContext + ": unexpected terminal response '" + response + "'", e);
		}
	}

	/**
	 * Reads the answer of a Windows console to an ANSI position request ("\033[6n") up to the terminating 'R'.
	 * The answer may arrive with a delay, so reading does not stop at the first moment without available input,
	 * but only when the terminator was read or the timeout has passed.
	 *
	 * @param response buffer receiving the answer
	 * @param timeoutMillis maximum time to wait for the answer
	 * @throws Exception if the console cannot be read
	 */
	static void readWindowsAnsiPositionResponse(final StringBuilder response, final long timeoutMillis) throws Exception {
		final long deadline = System.currentTimeMillis() + timeoutMillis;
		while (true) {
			final int keyCode = WindowsKeyStrokeReader.readKey(false);
			if (keyCode > 0) {
				response.append((char) keyCode);
				if (keyCode == 'R') {
					return;
				}
			} else if (keyCode == KeyCode_EndOfInput || System.currentTimeMillis() > deadline) {
				// End of input or no (complete) answer in time
				return;
			} else {
				Thread.sleep(5);
			}
		}
	}

	/**
	 * Requests the current cursor position from the terminal (ANSI "\033[6n").
	 *
	 * @return current cursor position
	 * @throws Exception if the terminal does not answer as expected
	 */
	public static Position getCursorPosition() throws Exception {
		try {
			final StringBuilder response = new StringBuilder();
			saveCurrentCursorPosition();
			try {
				if (SystemUtilities.isWindowsSystem()) {
					// Request current cursor position
					System.out.print("\033[6n");
					readWindowsAnsiPositionResponse(response, 1000);
				} else {
					final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
					try {
						// Request current cursor position
						System.out.print("\033[6n");

						// The console reader is shared by the whole application and must not be closed
						final Reader inputReader = getConsoleReader();
						int byteBuffer;
						while ((byteBuffer = inputReader.read()) > -1) {
							if (byteBuffer == 3) {
								break;
							} else if (byteBuffer == 27) {
								response.append("\\033");
							} else {
								response.append((char) byteBuffer);
								if ('R' == byteBuffer) {
									break;
								}
							}
						}
					} finally {
						if (linuxConsoleMustBeReset) {
							deactivateLinuxConsoleRawMode();
						}
					}
				}
			} finally {
				moveCursorToSavedPosition();
			}

			final int[] positionValues = parseAnsiPositionResponse(response, "Cannot detect cursor position");
			return new Position(positionValues[0], positionValues[1]);
		} catch (final Throwable t) {
			// Catching Throwable (not just Exception) here as well, since WindowsKeyStrokeReader.readKey() can
			// propagate an unchecked Error (e.g. UnsatisfiedLinkError from the native JNA call) that would
			// otherwise bypass this method's declared "throws Exception" contract entirely.
			throw new Exception("Cannot detect cursor position", t);
		}
	}

	/**
	 * Detects the terminal size by moving the cursor to the far bottom right and requesting its position.
	 * If a Windows console does not answer within a second, 24x80 is returned.
	 * <p>
	 * Before, a missing answer also switched the detected console type to TEST as a side effect. This is only done
	 * during the console type detection now.
	 *
	 * @return terminal size
	 * @throws Exception if the terminal does not answer as expected
	 */
	public static Size getTerminalSize() throws Exception {
		try {
			return readTerminalSize();
		} catch (@SuppressWarnings("unused") final TimeoutException e) {
			return new Size(24, 80);
		}
	}

	/**
	 * Requests the terminal size from the terminal.
	 *
	 * @throws TimeoutException if a Windows console does not answer within a second
	 * @throws Exception if the terminal does not answer as expected
	 */
	private static Size readTerminalSize() throws Exception {
		try {
			final StringBuilder response = new StringBuilder();
			saveCurrentCursorPosition();
			try {
				moveCursorToPosition(3000, 3000);

				if (SystemUtilities.isWindowsSystem()) {
					final WindowsConsoleWidthDetection consoleWidthDetection = new WindowsConsoleWidthDetection(response);
					final Thread consoleWithDetectionThread = new Thread(consoleWidthDetection, "ConsoleWidthDetection");
					// Daemon: if the underlying native read blocks forever (e.g. no real console attached),
					// interrupt() below cannot unblock it, so this thread may never terminate.
					// Marking it a daemon at least prevents it from keeping the JVM alive.
					consoleWithDetectionThread.setDaemon(true);
					consoleWithDetectionThread.start();
					// join() (instead of sleep and a state check) also makes the response written by the thread visible here
					consoleWithDetectionThread.join(1000);
					if (consoleWithDetectionThread.isAlive()) {
						// Note: interrupt() cannot cancel a thread blocked in a native call (_getwch/getwchar).
						// The thread is abandoned here (daemon) rather than actually stopped; this is a known limitation.
						consoleWithDetectionThread.interrupt();
						throw new TimeoutException("Console did not answer the request for the cursor position in time");
					}
				} else {
					final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
					try {
						// Request current cursor position
						System.out.print("\033[6n");

						// The console reader is shared by the whole application and must not be closed
						final Reader inputReader = getConsoleReader();
						int byteBuffer;
						while ((byteBuffer = inputReader.read()) > -1) {
							if (byteBuffer == 3) {
								break;
							} else if (byteBuffer == 27) {
								response.append("\\033");
							} else {
								response.append((char) byteBuffer);
								if ('R' == byteBuffer) {
									break;
								}
							}
						}
					} finally {
						if (linuxConsoleMustBeReset) {
							deactivateLinuxConsoleRawMode();
						}
					}
				}
			} finally {
				moveCursorToSavedPosition();
			}

			final int[] sizeValues = parseAnsiPositionResponse(response, "Cannot detect terminal size");
			return new Size(sizeValues[0], sizeValues[1]);
		} catch (final TimeoutException e) {
			throw e;
		} catch (final Throwable t) {
			throw new Exception("Cannot detect terminal size", t);
		}
	}

	/**
	 * Detects the terminal size via the "tput lines" and "tput cols" commands.
	 *
	 * @return terminal size
	 * @throws Exception if tput is not available or returns unexpected output
	 */
	public static Size getUnixTerminalSizeByTput() throws Exception {
		try {
			return new Size(Integer.parseInt(executeTputCommand("lines").trim()), Integer.parseInt(executeTputCommand("cols").trim()));
		} catch (final Exception e) {
			throw new Exception("Cannot detect terminal size", e);
		}
	}

	private static String executeTputCommand(final String command) throws IOException {
		final ProcessBuilder processBuilder = new ProcessBuilder("tput", command);
		try (InputStream inputStream = processBuilder.start().getInputStream()) {
			int readBuffer = 0;
			final StringBuffer buffer = new StringBuffer();
			while ((readBuffer = inputStream.read()) != -1) {
				buffer.append((char) readBuffer);
			}
			return buffer.toString();
		}
	}

	/**
	 * Reads a single key from the console (Linux: raw termios mode via 'stty', Windows: {@link WindowsKeyStrokeReader}).
	 * <p>
	 * On Linux the characters read from the terminal are converted by {@link AnsiKeySequenceParser}, which recognizes
	 * escape sequences by their grammar. Characters read ahead (e.g. pasted text or fast typing) are kept for the next
	 * calls. A single Escape key is reported after a short wait for further characters of a possible escape sequence.
	 *
	 * @return key code: one of the KeyCode_* constants for special keys, the character code otherwise
	 * @throws Exception if the console cannot be read, {@link EOFException} if the terminal is gone
	 */
	public static int readKey() throws Exception {
		try {
			if (SystemUtilities.isWindowsSystem()) {
				return WindowsKeyStrokeReader.readKey(true);
			} else {
				synchronized (LINUX_PENDING_INPUT) {
					while (true) {
						int keyCode = AnsiKeySequenceParser.parse(LINUX_PENDING_INPUT, false);
						if (keyCode == AnsiKeySequenceParser.NEED_MORE_INPUT) {
							final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
							try {
								if (LINUX_PENDING_INPUT.length() == 0) {
									// Waits for the next key
									readLinuxConsoleInput();
									continue;
								} else if (waitForLinuxConsoleInput(ESCAPE_SEQUENCE_TIMEOUT_MILLIS)) {
									// The rest of a started escape sequence
									readLinuxConsoleInput();
									continue;
								} else {
									// No further characters: a single Escape key or an incomplete sequence
									keyCode = AnsiKeySequenceParser.parse(LINUX_PENDING_INPUT, true);
								}
							} finally {
								if (linuxConsoleMustBeReset) {
									deactivateLinuxConsoleRawMode();
								}
							}
						}

						if (keyCode != AnsiKeySequenceParser.NO_KEY && keyCode != AnsiKeySequenceParser.NEED_MORE_INPUT) {
							return keyCode;
						}
					}
				}
			}
		} catch (final Throwable t) {
			// Catching Throwable (not just Exception) so an unchecked Error from the underlying native JNA call
			// (e.g. UnsatisfiedLinkError) is also wrapped here instead of silently breaking this method's
			// declared "throws Exception" contract. No separate printStackTrace() here - the cause is preserved
			// on the wrapped exception, so logging it is left to the caller instead of logging it twice.
			throw new Exception("Cannot readKey", t);
		}
	}

	/** Maximum time to wait for the rest of an escape sequence after an ESC character */
	private static final long ESCAPE_SEQUENCE_TIMEOUT_MILLIS = 50;

	/** Characters read from the Linux terminal, which were not yet returned as keys. Also used as lock. */
	private static final StringBuilder LINUX_PENDING_INPUT = new StringBuilder();

	/**
	 * Returns the reader of the system console. The reader is shared by the whole application, so it must not be closed.
	 */
	private static Reader getConsoleReader() throws IOException {
		final java.io.Console console = System.console();
		if (console == null) {
			throw new IOException("No console available");
		}
		return console.reader();
	}

	/**
	 * Reads the characters available from the Linux terminal (at least one, waiting for it) into the pending input.
	 */
	private static void readLinuxConsoleInput() throws IOException {
		// At least the size of the internal buffer of the console reader, so it hands over everything it has read
		final char[] inputReadBuffer = new char[1024];
		final int inputReadCharacters = getConsoleReader().read(inputReadBuffer);
		if (inputReadCharacters < 1) {
			// Terminal is gone (e.g. closed SSH session). Returning a key code here would make input loops spin forever.
			throw new EOFException("End of console input");
		}
		LINUX_PENDING_INPUT.append(inputReadBuffer, 0, inputReadCharacters);
	}

	/**
	 * Waits up to the given time for further input from the Linux terminal.
	 *
	 * @return true if input is available
	 */
	private static boolean waitForLinuxConsoleInput(final long timeoutMillis) throws Exception {
		final Reader consoleReader = getConsoleReader();
		final long deadline = System.currentTimeMillis() + timeoutMillis;
		// The console reader of newer Java versions (java.io.ProxyingConsole since Java 21) does not implement ready() and
		// always returns false. System.in reads the same file descriptor, so its available() shows the waiting input.
		// System.in itself is never read here, so it holds no buffered data of its own.
		while (!consoleReader.ready() && System.in.available() == 0) {
			if (System.currentTimeMillis() >= deadline) {
				return false;
			}
			Thread.sleep(5);
		}
		return true;
	}

	/**
	 * Reads the next raw character from the console, without interpreting escape sequences.
	 *
	 * @return character code, or {@link #KeyCode_EndOfInput} at the end of the input
	 * @throws Exception if the console cannot be read
	 */
	public static int readNextRawKey() throws Exception {
		try {
			if (SystemUtilities.isWindowsSystem()) {
				return WindowsKeyStrokeReader.readKey(true);
			} else {
				final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
				try {
					// Characters already read ahead by readKey() come first
					synchronized (LINUX_PENDING_INPUT) {
						if (LINUX_PENDING_INPUT.length() > 0) {
							final char nextCharacter = LINUX_PENDING_INPUT.charAt(0);
							LINUX_PENDING_INPUT.deleteCharAt(0);
							return nextCharacter;
						}
					}
					return getConsoleReader().read();
				} finally {
					if (linuxConsoleMustBeReset) {
						deactivateLinuxConsoleRawMode();
					}
				}
			}
		} catch (final Throwable t) {
			throw new Exception("Cannot readNextRawKey", t);
		}
	}
}
