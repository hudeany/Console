package de.soderer.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

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

	private static volatile Queue<Character> consoleInputBuffer = null;

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
	 * There is no Windows Code for ReverseTab
	 */
	public static final int KeyCode_ReverseTab = -1;
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
		if (consoleType == null) {
			if (SystemUtilities.isWindowsSystem() || (System.console() != null && System.getenv().get("TERM") != null)) {
				consoleType = ConsoleType.ANSI;
				try {
					getTerminalSize();
				} catch (@SuppressWarnings("unused") final Throwable t) {
					consoleType = ConsoleType.TEST;
				}
			} else {
				consoleType = ConsoleType.TEST;
			}
		}
		return consoleType;
	}

	/**
	 * Switches the Linux terminal into raw mode without echo via 'stty', so single keys can be read.
	 * A shutdown hook resets the mode on exit.
	 *
	 * @return true if the mode was switched, false if it was already active
	 * @throws Exception if 'stty' fails
	 */
	public static boolean activateLinuxConsoleRawMode() throws Exception {
		if (!linuxConsoleActivatedRawMode) {
			final int exitCode = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "stty raw -echo < /dev/tty" }).waitFor();
			if (exitCode != 0) {
				throw new Exception("Cannot activate linux console raw mode: 'stty' returned exit code " + exitCode);
			}
			linuxConsoleActivatedRawMode = true;
			registerShutdownHook();
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
	public static void deactivateLinuxConsoleRawMode() throws Exception {
		if (linuxConsoleActivatedRawMode) {
			final int exitCode = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "stty -raw echo </dev/tty" }).waitFor();
			// Mark as deactivated even on failure, so we don't get stuck retrying a broken 'stty' on every subsequent call
			linuxConsoleActivatedRawMode = false;
			if (exitCode != 0) {
				throw new Exception("Cannot deactivate linux console raw mode: 'stty' returned exit code " + exitCode);
			}
		}
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					deactivateLinuxConsoleRawMode();
				} catch (@SuppressWarnings("unused") final Exception e) {
					// do nothing
				}
			}
		});
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
			// Not enough space left for a bar, only render percentage and info parts
			return leftPart.substring(0, leftPart.length() - 1) + rightPart.substring(1);
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

					int keyBuffer;
					while ((keyBuffer = WindowsKeyStrokeReader.readKey(false)) > 0) {
						response.append((char) keyBuffer);
					}
				} else {
					final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
					try {
						// Request current cursor position
						System.out.print("\033[6n");

						try (Reader inputReader = System.console().reader()) {
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
	 * If a Windows console does not answer within a second, the console type is set to TEST and 24x80 is returned.
	 *
	 * @return terminal size
	 * @throws Exception if the terminal does not answer as expected
	 */
	public static Size getTerminalSize() throws Exception {
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
						consoleType = ConsoleType.TEST;
						return new Size(24, 80);
					}
				} else {
					final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
					try {
						// Request current cursor position
						System.out.print("\033[6n");

						try (Reader inputReader = System.console().reader()) {
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
	 * Known limitation: escape sequences are identified purely by the number of bytes read in one {@link Reader#read(char[])}
	 * call (3, 4 or 6 bytes). If an escape sequence and a following regular key end up in the same OS-level read
	 * (e.g. very fast typing right after an arrow key), this can misidentify the key. This has not been changed here,
	 * since a correct fix requires stateful byte-by-byte parsing of the escape sequence grammar instead of length-based
	 * dispatch, which is a larger change than a targeted bug fix.
	 *
	 * @return key code: one of the KeyCode_* constants for special keys, the character code otherwise
	 * @throws Exception if the console cannot be read
	 */
	public static int readKey() throws Exception {
		try {
			if (SystemUtilities.isWindowsSystem()) {
				return WindowsKeyStrokeReader.readKey(true);
			} else {
				if (consoleInputBuffer != null) {
					final int returnValue = consoleInputBuffer.remove();
					if (consoleInputBuffer.size() == 0) {
						consoleInputBuffer = null;
					}
					return returnValue;
				} else {
					final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
					try (Reader consoleReader = System.console().reader()) {
						int nextKeyCode = -1;

						final char[] inputReadBuffer = new char[256];
						int inputReadBytes;
						// Read method always waits for next key pressed, no matter how much data is still left in the console-reader
						inputReadBytes = consoleReader.read(inputReadBuffer);
						if (inputReadBytes > 1 && inputReadBuffer[0] != 27) {
							consoleInputBuffer = new LinkedList<>();
							for (int i = 0; i < inputReadBytes; i++) {
								consoleInputBuffer.add(inputReadBuffer[i]);
							}
							nextKeyCode = consoleInputBuffer.remove();
						} else {
							nextKeyCode = inputReadBuffer[0];
						}

						if (nextKeyCode == 3) {
							consoleInputBuffer = null;
							return KeyCode_CtrlC;
						} else if (nextKeyCode == 8) {
							consoleInputBuffer = null;
							return KeyCode_Backspace;
						} else if (nextKeyCode == 9) {
							consoleInputBuffer = null;
							return KeyCode_Tab;
						} else if (nextKeyCode == 13) {
							consoleInputBuffer = null;
							return KeyCode_Enter;
						} else if (nextKeyCode == 22) {
							consoleInputBuffer = null;
							return KeyCode_CtrlV;
						} else if (nextKeyCode == 24) {
							consoleInputBuffer = null;
							return KeyCode_CtrlX;
						} else if (nextKeyCode == 27) {
							consoleInputBuffer = null;
							// Escape sequence is entered
							if (inputReadBytes == 3) {
								if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 65) {
									return KeyCode_ArrowUp;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 66) {
									return KeyCode_ArrowDown;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 67) {
									return KeyCode_ArrowRight;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 68) {
									return KeyCode_ArrowLeft;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 70) {
									return KeyCode_End;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 72) {
									return KeyCode_Home;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 90) {
									return KeyCode_ReverseTab;
								} else {
									return KeyCode_Escape;
								}
							} else if (inputReadBytes == 4) {
								if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 50 && inputReadBuffer[3] == 126) {
									return KeyCode_Insert;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 51 && inputReadBuffer[3] == 126) {
									return KeyCode_Delete;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 53 && inputReadBuffer[3] == 126) {
									return KeyCode_PageUp;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 54 && inputReadBuffer[3] == 126) {
									return KeyCode_PageDown;
								} else {
									return KeyCode_Escape;
								}
							} else if (inputReadBytes == 6) {
								if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 70) {
									return KeyCode_CtrlEnd;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 72) {
									return KeyCode_CtrlHome;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 65) {
									return KeyCode_CtrlArrowUp;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 66) {
									return KeyCode_CtrlArrowDown;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 67) {
									return KeyCode_CtrlArrowRight;
								} else if (inputReadBuffer[1] == 91 && inputReadBuffer[2] == 49 && inputReadBuffer[3] == 59 && inputReadBuffer[4] == 53 && inputReadBuffer[5] == 68) {
									return KeyCode_CtrlArrowLeft;
								} else {
									return KeyCode_Escape;
								}
							} else {
								return KeyCode_Escape;
							}
						} else if (nextKeyCode == 127) {
							consoleInputBuffer = null;
							return KeyCode_Backspace;
						} else {
							return nextKeyCode;
						}
					} finally {
						if (linuxConsoleMustBeReset) {
							deactivateLinuxConsoleRawMode();
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

	/**
	 * Reads the next raw character from the console, without interpreting escape sequences.
	 *
	 * @return character code, or -1 at the end of the input
	 * @throws Exception if the console cannot be read
	 */
	public static int readNextRawKey() throws Exception {
		try {
			if (SystemUtilities.isWindowsSystem()) {
				return WindowsKeyStrokeReader.readKey(true);
			} else {
				final boolean linuxConsoleMustBeReset = activateLinuxConsoleRawMode();
				try (Reader consoleReader = System.console().reader()) {
					return consoleReader.read();
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
