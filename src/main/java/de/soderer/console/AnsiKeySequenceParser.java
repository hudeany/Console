package de.soderer.console;

/**
 * Stateful parser for the key input of ANSI terminals (Linux), which converts characters and escape sequences
 * into the key codes of {@link ConsoleUtilities}.
 * <p>
 * Escape sequences are recognized by their grammar instead of the number of characters read at once, so a sequence
 * followed by further keys in the same read (fast typing, paste) and a sequence split over several reads are both
 * handled correctly:
 * <ul>
 * <li>CSI: ESC '[' parameter characters (0x30-0x3F), intermediate characters (0x20-0x2F), one final character (0x40-0x7E)</li>
 * <li>SS3: ESC 'O' one final character (application cursor mode, F1-F4)</li>
 * <li>ESC followed by any other character: Alt+key, the key itself is returned (like on Windows)</li>
 * </ul>
 * Unknown complete sequences are skipped instead of ending up as text in the input.
 * Package-private and free of terminal access, so it can be tested on all systems.
 */
final class AnsiKeySequenceParser {
	/** The pending input starts with an incomplete escape sequence, more input is needed */
	static final int NEED_MORE_INPUT = Integer.MIN_VALUE;
	/** A complete but unknown escape sequence was consumed, there is no key for the application */
	static final int NO_KEY = Integer.MIN_VALUE + 1;

	private static final char ESC = 27;
	private static final char DEL = 127;

	private AnsiKeySequenceParser() {
		// Only static methods
	}

	/**
	 * Parses the next key from the start of the pending input and removes its characters from it.
	 *
	 * @param pending characters read from the terminal, not yet converted to keys
	 * @param inputComplete true if no further input is to be expected right now (waiting for it timed out),
	 *        so an incomplete escape sequence is taken as the Escape key
	 * @return key code, {@link #NEED_MORE_INPUT} or {@link #NO_KEY}
	 */
	static int parse(final StringBuilder pending, final boolean inputComplete) {
		if (pending.length() == 0) {
			return NEED_MORE_INPUT;
		}

		final char first = pending.charAt(0);
		if (first != ESC) {
			pending.deleteCharAt(0);
			return mapSingleCharacter(first);
		} else if (pending.length() == 1) {
			if (inputComplete) {
				pending.deleteCharAt(0);
				return ConsoleUtilities.KeyCode_Escape;
			} else {
				return NEED_MORE_INPUT;
			}
		}

		final char second = pending.charAt(1);
		if (second == '[') {
			return parseCsi(pending, inputComplete);
		} else if (second == 'O') {
			return parseSs3(pending, inputComplete);
		} else if (second == ESC) {
			// Escape pressed twice: the first one is a plain Escape, the second one is parsed with the next call
			pending.deleteCharAt(0);
			return ConsoleUtilities.KeyCode_Escape;
		} else {
			// Alt+key: the terminal sends ESC followed by the key
			pending.delete(0, 2);
			return mapSingleCharacter(second);
		}
	}

	private static int mapSingleCharacter(final char character) {
		if (character == DEL) {
			// Backspace sends DEL (127) on most Linux terminals
			return ConsoleUtilities.KeyCode_Backspace;
		} else {
			// Ctrl+C (3), Backspace (8), Tab (9), Enter (13), Ctrl+V (22), Ctrl+X (24) have the values of their key codes
			return character;
		}
	}

	/**
	 * CSI sequence: ESC [ parameters final
	 */
	private static int parseCsi(final StringBuilder pending, final boolean inputComplete) {
		int index = 2;
		while (index < pending.length() && pending.charAt(index) >= 0x30 && pending.charAt(index) <= 0x3F) {
			index++;
		}
		final int parameterEnd = index;
		while (index < pending.length() && pending.charAt(index) >= 0x20 && pending.charAt(index) <= 0x2F) {
			index++;
		}

		if (index >= pending.length()) {
			if (inputComplete) {
				// Incomplete sequence and no more input: Alt+'[' or a broken sequence
				pending.delete(0, index);
				return ConsoleUtilities.KeyCode_Escape;
			} else {
				return NEED_MORE_INPUT;
			}
		}

		final char finalCharacter = pending.charAt(index);
		if (finalCharacter < 0x40 || finalCharacter > 0x7E) {
			// Invalid sequence: drop "ESC [" and keep the rest as normal input
			pending.delete(0, 2);
			return ConsoleUtilities.KeyCode_Escape;
		}

		final String parameters = pending.substring(2, parameterEnd);
		final boolean hasIntermediates = parameterEnd != index;
		pending.delete(0, index + 1);
		if (hasIntermediates) {
			return NO_KEY;
		}

		final String[] parameterParts = parameters.isEmpty() ? new String[0] : parameters.split(";", -1);
		final int firstParameter = getNumericParameter(parameterParts, 0);
		final int modifierParameter = getNumericParameter(parameterParts, 1);
		if (firstParameter < 0 || modifierParameter < 0) {
			// Private parameters like "?" or "<" (e.g. mouse reports) are no keys
			return NO_KEY;
		}
		// xterm modifier encoding: 1 + (Shift ? 1 : 0) + (Alt ? 2 : 0) + (Ctrl ? 4 : 0)
		final boolean ctrlPressed = ((modifierParameter - 1) & 4) != 0;

		switch (finalCharacter) {
			case 'A':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowUp : ConsoleUtilities.KeyCode_ArrowUp;
			case 'B':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowDown : ConsoleUtilities.KeyCode_ArrowDown;
			case 'C':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowRight : ConsoleUtilities.KeyCode_ArrowRight;
			case 'D':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowLeft : ConsoleUtilities.KeyCode_ArrowLeft;
			case 'H':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlHome : ConsoleUtilities.KeyCode_Home;
			case 'F':
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlEnd : ConsoleUtilities.KeyCode_End;
			case 'Z':
				return ConsoleUtilities.KeyCode_ReverseTab;
			case 'P':
				return ConsoleUtilities.KeyCode_F1;
			case 'Q':
				return ConsoleUtilities.KeyCode_F2;
			case 'S':
				return ConsoleUtilities.KeyCode_F4;
			case '~':
				return mapTildeSequence(firstParameter, ctrlPressed);
			default:
				// e.g. 'R': a cursor position report, which arrived outside of a position request (also the
				// ambiguous modified F3 "ESC [ 1 ; 5 R")
				return NO_KEY;
		}
	}

	/**
	 * VT220 style sequences: ESC [ number ~ (Home, End, Insert, Delete, Page up/down, F5-F12)
	 */
	private static int mapTildeSequence(final int number, final boolean ctrlPressed) {
		switch (number) {
			case 1:
			case 7:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlHome : ConsoleUtilities.KeyCode_Home;
			case 4:
			case 8:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlEnd : ConsoleUtilities.KeyCode_End;
			case 2:
				return ConsoleUtilities.KeyCode_Insert;
			case 3:
				return ConsoleUtilities.KeyCode_Delete;
			case 5:
				return ConsoleUtilities.KeyCode_PageUp;
			case 6:
				return ConsoleUtilities.KeyCode_PageDown;
			case 11:
				return ConsoleUtilities.KeyCode_F1;
			case 12:
				return ConsoleUtilities.KeyCode_F2;
			case 13:
				return ConsoleUtilities.KeyCode_F3;
			case 14:
				return ConsoleUtilities.KeyCode_F4;
			case 15:
				return ConsoleUtilities.KeyCode_F5;
			case 17:
				return ConsoleUtilities.KeyCode_F6;
			case 18:
				return ConsoleUtilities.KeyCode_F7;
			case 19:
				return ConsoleUtilities.KeyCode_F8;
			case 20:
				return ConsoleUtilities.KeyCode_F9;
			case 21:
				return ConsoleUtilities.KeyCode_F10;
			case 23:
				return ConsoleUtilities.KeyCode_F11;
			case 24:
				return ConsoleUtilities.KeyCode_F12;
			default:
				return NO_KEY;
		}
	}

	/**
	 * SS3 sequence: ESC O final (application cursor mode and F1-F4)
	 */
	private static int parseSs3(final StringBuilder pending, final boolean inputComplete) {
		if (pending.length() < 3) {
			if (inputComplete) {
				// Alt+'O'
				pending.delete(0, 2);
				return 'O';
			} else {
				return NEED_MORE_INPUT;
			}
		}

		final char finalCharacter = pending.charAt(2);
		pending.delete(0, 3);
		switch (finalCharacter) {
			case 'A':
				return ConsoleUtilities.KeyCode_ArrowUp;
			case 'B':
				return ConsoleUtilities.KeyCode_ArrowDown;
			case 'C':
				return ConsoleUtilities.KeyCode_ArrowRight;
			case 'D':
				return ConsoleUtilities.KeyCode_ArrowLeft;
			case 'H':
				return ConsoleUtilities.KeyCode_Home;
			case 'F':
				return ConsoleUtilities.KeyCode_End;
			case 'P':
				return ConsoleUtilities.KeyCode_F1;
			case 'Q':
				return ConsoleUtilities.KeyCode_F2;
			case 'R':
				return ConsoleUtilities.KeyCode_F3;
			case 'S':
				return ConsoleUtilities.KeyCode_F4;
			case 'M':
				// Enter of the numeric keypad in application keypad mode
				return ConsoleUtilities.KeyCode_Enter;
			default:
				return NO_KEY;
		}
	}

	/**
	 * Returns a numeric parameter of a CSI sequence: 1 if missing or empty (the default of all key sequences),
	 * -1 if it is not a number.
	 */
	private static int getNumericParameter(final String[] parameterParts, final int index) {
		if (index >= parameterParts.length || parameterParts[index].isEmpty()) {
			return 1;
		}
		final String parameter = parameterParts[index];
		for (int i = 0; i < parameter.length(); i++) {
			if (parameter.charAt(i) < '0' || parameter.charAt(i) > '9') {
				return -1;
			}
		}
		try {
			return Integer.parseInt(parameter);
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return -1;
		}
	}
}
