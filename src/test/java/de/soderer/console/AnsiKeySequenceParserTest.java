package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class AnsiKeySequenceParserTest {
	private static final String ESC = "\033";

	/**
	 * Parses all keys of an input, which arrived completely (no further input expected).
	 */
	private static List<Integer> parseAll(final String input) {
		final StringBuilder pending = new StringBuilder(input);
		final List<Integer> keyCodes = new ArrayList<>();
		while (pending.length() > 0) {
			int keyCode = AnsiKeySequenceParser.parse(pending, false);
			if (keyCode == AnsiKeySequenceParser.NEED_MORE_INPUT) {
				keyCode = AnsiKeySequenceParser.parse(pending, true);
			}
			if (keyCode != AnsiKeySequenceParser.NO_KEY) {
				keyCodes.add(keyCode);
			}
		}
		return keyCodes;
	}

	private static int parseSingle(final String input) {
		final List<Integer> keyCodes = parseAll(input);
		assertEquals(1, keyCodes.size(), "Number of keys in " + input.replace(ESC, "ESC"));
		return keyCodes.get(0);
	}

	@Test
	public void testPlainCharactersAndControlKeys() {
		assertEquals(List.of((int) 'a', (int) 'ä', (int) '€'), parseAll("aä€"));
		assertEquals(ConsoleUtilities.KeyCode_Enter, parseSingle("\r"));
		assertEquals(ConsoleUtilities.KeyCode_Tab, parseSingle("\t"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlC, parseSingle("\u0003"));
		assertEquals(ConsoleUtilities.KeyCode_Backspace, parseSingle("\u007F"));
		assertEquals(ConsoleUtilities.KeyCode_Backspace, parseSingle("\b"));
	}

	@Test
	public void testCursorKeys() {
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, parseSingle(ESC + "[A"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowDown, parseSingle(ESC + "[B"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowRight, parseSingle(ESC + "[C"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowLeft, parseSingle(ESC + "[D"));
		// Application cursor mode
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, parseSingle(ESC + "OA"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowLeft, parseSingle(ESC + "OD"));
	}

	@Test
	public void testHomeAndEndVariants() {
		for (final String home : new String[] { "[H", "OH", "[1~", "[7~" }) {
			assertEquals(ConsoleUtilities.KeyCode_Home, parseSingle(ESC + home), home);
		}
		for (final String end : new String[] { "[F", "OF", "[4~", "[8~" }) {
			assertEquals(ConsoleUtilities.KeyCode_End, parseSingle(ESC + end), end);
		}
	}

	@Test
	public void testEditingKeys() {
		assertEquals(ConsoleUtilities.KeyCode_Insert, parseSingle(ESC + "[2~"));
		assertEquals(ConsoleUtilities.KeyCode_Delete, parseSingle(ESC + "[3~"));
		assertEquals(ConsoleUtilities.KeyCode_PageUp, parseSingle(ESC + "[5~"));
		assertEquals(ConsoleUtilities.KeyCode_PageDown, parseSingle(ESC + "[6~"));
		assertEquals(ConsoleUtilities.KeyCode_ReverseTab, parseSingle(ESC + "[Z"));
	}

	@Test
	public void testModifiedKeys() {
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowUp, parseSingle(ESC + "[1;5A"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowLeft, parseSingle(ESC + "[1;5D"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlHome, parseSingle(ESC + "[1;5H"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlEnd, parseSingle(ESC + "[1;5F"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlHome, parseSingle(ESC + "[1;5~"));
		// Shift (2) and Alt (3) do not change cursor keys, Ctrl+Shift (6) counts as Ctrl
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, parseSingle(ESC + "[1;2A"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, parseSingle(ESC + "[1;3A"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowUp, parseSingle(ESC + "[1;6A"));
	}

	@Test
	public void testFunctionKeys() {
		assertEquals(ConsoleUtilities.KeyCode_F1, parseSingle(ESC + "OP"));
		assertEquals(ConsoleUtilities.KeyCode_F3, parseSingle(ESC + "OR"));
		assertEquals(ConsoleUtilities.KeyCode_F4, parseSingle(ESC + "OS"));
		assertEquals(ConsoleUtilities.KeyCode_F1, parseSingle(ESC + "[11~"));
		assertEquals(ConsoleUtilities.KeyCode_F5, parseSingle(ESC + "[15~"));
		assertEquals(ConsoleUtilities.KeyCode_F10, parseSingle(ESC + "[21~"));
		assertEquals(ConsoleUtilities.KeyCode_F12, parseSingle(ESC + "[24~"));
		assertEquals(ConsoleUtilities.KeyCode_F2, parseSingle(ESC + "[1;5Q"));
	}

	@Test
	public void testSequenceFollowedByKeysInSameRead() {
		// Before, a sequence was only recognized if it was read alone, "ESC[Ab" became Escape plus the text "[Ab"
		assertEquals(List.of(ConsoleUtilities.KeyCode_ArrowUp, (int) 'b'), parseAll(ESC + "[Ab"));
		assertEquals(List.of((int) 'a', ConsoleUtilities.KeyCode_ArrowLeft, ConsoleUtilities.KeyCode_Delete, ConsoleUtilities.KeyCode_Enter), parseAll("a" + ESC + "[D" + ESC + "[3~\r"));
	}

	@Test
	public void testSequenceSplitOverSeveralReads() {
		final StringBuilder pending = new StringBuilder(ESC + "[1;");
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(pending, false));
		pending.append("5C");
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowRight, AnsiKeySequenceParser.parse(pending, false));
		assertEquals(0, pending.length());
	}

	@Test
	public void testSingleEscapeNeedsTimeout() {
		final StringBuilder pending = new StringBuilder(ESC);
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(pending, false));
		assertEquals(ConsoleUtilities.KeyCode_Escape, AnsiKeySequenceParser.parse(pending, true));
		assertEquals(0, pending.length());
	}

	@Test
	public void testAltKeyKeepsTheKey() {
		// Before, Alt+x became Escape and the 'x' was lost
		assertEquals(List.of((int) 'x'), parseAll(ESC + "x"));
		assertEquals(List.of((int) 'O'), parseAll(ESC + "O"));
		assertEquals(List.of(ConsoleUtilities.KeyCode_Escape, ConsoleUtilities.KeyCode_Escape), parseAll(ESC + ESC));
	}

	@Test
	public void testUnknownSequencesAreSkipped() {
		// Unknown key, mouse report with private parameter, cursor position report
		assertEquals(List.of((int) 'a'), parseAll(ESC + "[99~" + ESC + "[<0;10;5M" + ESC + "[24;80Ra"));
	}
}
