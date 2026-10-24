package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import de.soderer.console.ConsoleUtilities.TextAttribute;
import de.soderer.console.ConsoleUtilities.TextColor;
import de.soderer.console.utilities.SystemUtilities;

/**
 * Additional tests of {@link ConsoleUtilities}, which need no real terminal: no key is read and the terminal mode
 * is never changed.
 */
@SuppressWarnings("static-method")
public class ConsoleUtilitiesAdditionalTest {
	private static final String ESC = "\033[";

	@Test
	public void testIsInsertableCharacter() {
		assertTrue(ConsoleUtilities.isInsertableCharacter('a'));
		assertTrue(ConsoleUtilities.isInsertableCharacter(' '));
		assertTrue(ConsoleUtilities.isInsertableCharacter('ä'));
		assertTrue(ConsoleUtilities.isInsertableCharacter('€'));
		assertTrue(ConsoleUtilities.isInsertableCharacter(0x1FFF));

		assertFalse(ConsoleUtilities.isInsertableCharacter(0x1F));
		assertFalse(ConsoleUtilities.isInsertableCharacter(ConsoleUtilities.KeyCode_Enter));
		assertFalse(ConsoleUtilities.isInsertableCharacter(ConsoleUtilities.KeyCode_Tab));
		assertFalse(ConsoleUtilities.isInsertableCharacter(127));
		assertFalse(ConsoleUtilities.isInsertableCharacter(0x85));
		assertFalse(ConsoleUtilities.isInsertableCharacter(0x2000));
		assertFalse(ConsoleUtilities.isInsertableCharacter(0x4E2D));
		assertFalse(ConsoleUtilities.isInsertableCharacter(ConsoleUtilities.KeyCode_ArrowUp));
		assertFalse(ConsoleUtilities.isInsertableCharacter(ConsoleUtilities.KeyCode_EndOfInput));
	}

	@Test
	public void testRemoveControlCharacters() {
		assertNull(ConsoleUtilities.removeControlCharacters(null));
		assertEquals("", ConsoleUtilities.removeControlCharacters(""));
		assertEquals("plain text", ConsoleUtilities.removeControlCharacters("plain text"));
		assertEquals("line1line2tab[31mred", ConsoleUtilities.removeControlCharacters("line1\r\nline2\ttab\033[31mred\u007F\u0085"));
	}

	@Test
	public void testCursorControlSequences() throws Exception {
		assertEquals(ESC + "s", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::saveCurrentCursorPosition));
		assertEquals(ESC + "u", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::moveCursorToSavedPosition));
		assertEquals(ESC + "12;34H", ConsoleTestHelper.captureSystemOut(() -> ConsoleUtilities.moveCursorToPosition(12, 34)));
		assertEquals(ESC + "K", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::clearLineContentAfterCursor));
		assertEquals(ESC + "?25l", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::hideCursor));
		assertEquals(ESC + "?25h", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::showCursor));
	}

	@Test
	public void testClearScreen() throws Exception {
		assumeTrue(!SystemUtilities.isWindowsSystem(), "On Windows the screen is cleared by 'cls'");
		assertEquals(ESC + "H" + ESC + "2J", ConsoleTestHelper.captureSystemOut(ConsoleUtilities::clearScreen));
	}

	@Test
	public void testTextAttributes() {
		assertEquals(ESC + "0m", TextAttribute.RESET.getAnsiCode());
		assertEquals(ESC + "7m", TextAttribute.REVERSE_VIDEO.getAnsiCode());
		assertEquals(ESC + "8m", TextAttribute.INVISIBLE_TEXT.getAnsiCode());
		assertEquals(9, TextAttribute.values().length);
		assertEquals(30, TextColor.Black.getForeGroundColorCode());
		assertEquals(107, TextColor.White.getBackGroundColorCode());
		assertEquals(ESC + "100mtext" + ESC + "0m", ConsoleUtilities.getAnsiFormattedText("text", null, TextColor.Dark_gray));
	}

	@Test
	public void testProgressStringWithoutUnit() {
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now().minusSeconds(10), 1000, 250, null);
		assertEquals(80, progress.length());
		assertTrue(progress.startsWith("25% [="), progress);
		assertTrue(progress.contains("/s eta "), progress);
	}

	@Test
	public void testProgressStringWithFastStart() {
		// Started right now: the elapsed time of 0 seconds must not cause a division by zero
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now(), 1000, 10, "B");
		assertTrue(progress.startsWith(" 1% [>"), progress);
	}

	@Test
	public void testConsoleTypeIsDetectedOnce() {
		final ConsoleType consoleType = ConsoleUtilities.getConsoleType();
		assertNotNull(consoleType);
		assertSame(consoleType, ConsoleUtilities.getConsoleType());
	}

	@Test
	public void testDeactivateRawModeWithoutActivation() throws Exception {
		assumeTrue(!Boolean.TRUE.equals(ConsoleTestHelper.getStaticField(ConsoleUtilities.class, "linuxConsoleActivatedRawMode")));
		// Nothing to reset: 'stty' is not called
		ConsoleUtilities.deactivateLinuxConsoleRawMode();
	}

	private static int[] parseAnsiPositionResponse(final String response) throws Exception {
		return (int[]) ConsoleTestHelper.invokeStatic(ConsoleUtilities.class, "parseAnsiPositionResponse", new Class<?>[] { StringBuilder.class, String.class }, new StringBuilder(response), "Context");
	}

	@Test
	public void testParseAnsiPositionResponse() throws Exception {
		assertArrayEquals(new int[] { 24, 80 }, parseAnsiPositionResponse("\\033[24;80R"));
		// The last report counts
		assertArrayEquals(new int[] { 3, 7 }, parseAnsiPositionResponse("\\033[1;1R\\033[3;7R"));
	}

	@Test
	public void testParseAnsiPositionResponseErrors() {
		for (final String response : new String[] { "", "24;80R", "[24R", "[24;80", "[a;80R", "[24;R" }) {
			final Exception exception = assertThrows(Exception.class, () -> parseAnsiPositionResponse(response), response);
			assertTrue(exception.getMessage().startsWith("Context: unexpected terminal response"), exception.getMessage());
		}
	}

	@Test
	public void testUnixTerminalSizeByTput() {
		// Depends on the environment: tput may be missing or have no terminal
		try {
			final Size size = ConsoleUtilities.getUnixTerminalSizeByTput();
			assertTrue(size.getHeight() > 0, size.toString());
			assertTrue(size.getWidth() > 0, size.toString());
		} catch (final Exception e) {
			assertEquals("Cannot detect terminal size", e.getMessage());
		}
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ConsoleTestHelper.invokePrivateConstructor(ConsoleUtilities.class);
	}
}
