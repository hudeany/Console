package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import de.soderer.console.ConsoleUtilities.TextAttribute;
import de.soderer.console.ConsoleUtilities.TextColor;

@SuppressWarnings("static-method")
public class ConsoleUtilitiesTest {
	private static final String ESC = "\033[";
	private static final String RESET = ESC + "0m";

	@Test
	public void testAnsiFormattedTextWithoutFormatting() {
		assertEquals("text" + RESET, ConsoleUtilities.getAnsiFormattedText("text", null, null));
	}

	@Test
	public void testAnsiColoredTextForeground() {
		assertEquals(ESC + "31mtext" + RESET, ConsoleUtilities.getAnsiColoredText("text", TextColor.Red));
	}

	@Test
	public void testAnsiColoredTextForegroundAndBackground() {
		assertEquals(ESC + "97m" + ESC + "44mtext" + RESET, ConsoleUtilities.getAnsiColoredText(TextColor.White, TextColor.Blue, "text"));
	}

	@Test
	public void testAnsiFormattedTextWithAttributes() {
		final String result = ConsoleUtilities.getAnsiFormattedText("text", TextColor.Green, null, TextAttribute.HIGH_INTENSITY, TextAttribute.UNDERLINE);
		assertEquals(ESC + "32m" + ESC + "1m" + ESC + "4mtext" + RESET, result);
	}

	@Test
	public void testAnsiFormattedTextWithNullAttributes() {
		assertEquals("text" + RESET, ConsoleUtilities.getAnsiFormattedText("text", null, null, (TextAttribute[]) null));
	}

	@Test
	public void testBackgroundColorCodesAreForegroundPlusTen() {
		for (final TextColor color : TextColor.values()) {
			assertEquals(color.getForeGroundColorCode() + 10, color.getBackGroundColorCode(), color.name());
		}
	}

	@Test
	public void testProgressStringUnknownTotal() {
		final String progress = ConsoleUtilities.getConsoleProgressString(60, LocalDateTime.now(), 0, 0, "B");
		assertEquals(60, progress.length());
		assertTrue(progress.startsWith(" 0% [>"), progress);
		assertTrue(progress.endsWith("] ?? ???/s eta ???"), progress);
	}

	@Test
	public void testProgressStringHalfDone() {
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now().minusSeconds(10), 1000, 500, "B");
		assertEquals(80, progress.length());
		assertTrue(progress.startsWith("50% [====="), progress);
		assertTrue(progress.contains("] 1.0000 kB 50B/s eta "), progress);

		// The bar head is in the middle of the bar
		final int barStart = progress.indexOf('[') + 1;
		final int barEnd = progress.indexOf(']');
		final int head = progress.indexOf('>');
		final int barWidth = barEnd - barStart;
		assertEquals(barWidth / 2, head - barStart + 1);
	}

	@Test
	public void testProgressStringAllDone() {
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now().minusSeconds(10), 1000, 1000, "B");
		assertEquals(80, progress.length());
		assertTrue(progress.startsWith("100% [===="), progress);
		assertTrue(progress.contains("=>]"), progress);
		assertFalse(progress.contains(" ]"), progress);
	}

	@Test
	public void testProgressStringMoreDoneThanExpected() {
		// No ETA can be calculated, but this must not fail
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now().minusSeconds(10), 1000, 1500, "B");
		assertEquals(80, progress.length());
		assertTrue(progress.startsWith("150% ["), progress);
		assertTrue(progress.endsWith("eta ???"), progress);
	}

	@Test
	public void testProgressStringStartInFuture() {
		final String progress = ConsoleUtilities.getConsoleProgressString(80, LocalDateTime.now().plusMinutes(1), 1000, 500, "B");
		assertTrue(progress.endsWith("eta ???"), progress);
	}

	@Test
	public void testProgressStringWithoutSpaceForBar() {
		final String progress = ConsoleUtilities.getConsoleProgressString(10, LocalDateTime.now().minusSeconds(10), 1000, 500, "B");
		assertTrue(progress.startsWith("50%"), progress);
		assertFalse(progress.contains("["), progress);
		assertFalse(progress.contains("]"), progress);
	}

	@Test
	public void testPrintBoxed() {
		final String output = captureSystemOut(() -> ConsoleUtilities.printBoxed("Hello"));
		final String lineSeparator = System.lineSeparator();
		assertEquals("*********" + lineSeparator + "* Hello *" + lineSeparator + "*********" + lineSeparator, output);
	}

	@Test
	public void testPrintBoxedWithCustomCharacter() {
		final String output = captureSystemOut(() -> ConsoleUtilities.printBoxed("Hi", '#'));
		final String lineSeparator = System.lineSeparator();
		assertEquals("######" + lineSeparator + "# Hi #" + lineSeparator + "######" + lineSeparator, output);
	}

	@Test
	public void testPrintBoxedNull() {
		assertEquals("", captureSystemOut(() -> ConsoleUtilities.printBoxed(null)));
	}

	private static String captureSystemOut(final Runnable action) {
		final PrintStream originalOut = System.out;
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (PrintStream captureStream = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
			System.setOut(captureStream);
			action.run();
		} finally {
			System.setOut(originalOut);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}
}
