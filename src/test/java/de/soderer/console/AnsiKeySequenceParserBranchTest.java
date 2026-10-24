package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Additional tests of {@link AnsiKeySequenceParser} for the less common key sequences and malformed input.
 */
@SuppressWarnings("static-method")
public class AnsiKeySequenceParserBranchTest {
	private static final String ESC = "\033";

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
	public void testEmptyInputNeedsMoreInput() {
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(), false));
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(), true));
	}

	@Test
	public void testIncompleteSequencesNeedMoreInput() {
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(ESC), false));
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(ESC + "["), false));
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(ESC + "[1;5"), false));
		assertEquals(AnsiKeySequenceParser.NEED_MORE_INPUT, AnsiKeySequenceParser.parse(new StringBuilder(ESC + "O"), false));
	}

	@Test
	public void testIncompleteCsiSequenceAtEndOfInput() {
		final StringBuilder pending = new StringBuilder(ESC + "[1;5");
		assertEquals(ConsoleUtilities.KeyCode_Escape, AnsiKeySequenceParser.parse(pending, true));
		assertEquals(0, pending.length());
	}

	@Test
	public void testCtrlArrowKeys() {
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowDown, parseSingle(ESC + "[1;5B"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowRight, parseSingle(ESC + "[1;5C"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowDown, parseSingle(ESC + "[B"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowRight, parseSingle(ESC + "[C"));
	}

	@Test
	public void testCsiFunctionKeys() {
		assertEquals(ConsoleUtilities.KeyCode_F1, parseSingle(ESC + "[P"));
		assertEquals(ConsoleUtilities.KeyCode_F2, parseSingle(ESC + "[Q"));
		assertEquals(ConsoleUtilities.KeyCode_F4, parseSingle(ESC + "[S"));
	}

	@Test
	public void testTildeSequences() {
		assertEquals(ConsoleUtilities.KeyCode_Home, parseSingle(ESC + "[1~"));
		assertEquals(ConsoleUtilities.KeyCode_Home, parseSingle(ESC + "[7~"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlHome, parseSingle(ESC + "[7;5~"));
		assertEquals(ConsoleUtilities.KeyCode_End, parseSingle(ESC + "[4~"));
		assertEquals(ConsoleUtilities.KeyCode_End, parseSingle(ESC + "[8~"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlEnd, parseSingle(ESC + "[4;5~"));
		assertEquals(ConsoleUtilities.KeyCode_CtrlEnd, parseSingle(ESC + "[8;5~"));
		assertEquals(ConsoleUtilities.KeyCode_F2, parseSingle(ESC + "[12~"));
		assertEquals(ConsoleUtilities.KeyCode_F3, parseSingle(ESC + "[13~"));
		assertEquals(ConsoleUtilities.KeyCode_F4, parseSingle(ESC + "[14~"));
		assertEquals(ConsoleUtilities.KeyCode_F6, parseSingle(ESC + "[17~"));
		assertEquals(ConsoleUtilities.KeyCode_F7, parseSingle(ESC + "[18~"));
		assertEquals(ConsoleUtilities.KeyCode_F8, parseSingle(ESC + "[19~"));
		assertEquals(ConsoleUtilities.KeyCode_F9, parseSingle(ESC + "[20~"));
		assertEquals(ConsoleUtilities.KeyCode_F11, parseSingle(ESC + "[23~"));
	}

	@Test
	public void testSs3Sequences() {
		assertEquals(ConsoleUtilities.KeyCode_ArrowDown, parseSingle(ESC + "OB"));
		assertEquals(ConsoleUtilities.KeyCode_ArrowRight, parseSingle(ESC + "OC"));
		assertEquals(ConsoleUtilities.KeyCode_Home, parseSingle(ESC + "OH"));
		assertEquals(ConsoleUtilities.KeyCode_End, parseSingle(ESC + "OF"));
		assertEquals(ConsoleUtilities.KeyCode_F2, parseSingle(ESC + "OQ"));
		assertEquals(ConsoleUtilities.KeyCode_Enter, parseSingle(ESC + "OM"));
		// Unknown SS3 final character: no key
		assertEquals(List.of(), parseAll(ESC + "OX"));
	}

	@Test
	public void testInvalidCsiFinalCharacterKeepsTheRest() {
		// A control character instead of a final character: "ESC [" is dropped, the rest is normal input
		assertEquals(List.of(ConsoleUtilities.KeyCode_Escape, (int) '1', ConsoleUtilities.KeyCode_Enter), parseAll(ESC + "[1\r"));
	}

	@Test
	public void testCsiSequencesWithoutKey() {
		// Intermediate characters
		assertEquals(List.of((int) 'x'), parseAll(ESC + "[1 qx"));
		// Not numeric parameters
		assertEquals(List.of((int) 'x'), parseAll(ESC + "[?25hx"));
		assertEquals(List.of((int) 'x'), parseAll(ESC + "[1;?Ax"));
		// Number too large for an int
		assertEquals(List.of((int) 'x'), parseAll(ESC + "[99999999999~x"));
		// Unknown final character
		assertEquals(List.of((int) 'x'), parseAll(ESC + "[1Jx"));
	}

	@Test
	public void testEmptyParametersUseDefaults() {
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, parseSingle(ESC + "[;A"));
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ConsoleTestHelper.invokePrivateConstructor(AnsiKeySequenceParser.class);
	}
}
