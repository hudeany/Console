package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.soderer.console.ConsoleUtilities.TextAttribute;

/**
 * Tests of the output of {@link InputLineRenderer#render(String, String, String, TextAttribute[])} and
 * {@link InputLineRenderer#renderFinal(String)}. The renderer is created without terminal requests.
 */
@SuppressWarnings("static-method")
public class InputLineRendererOutputTest {
	private static final String ESC = "\033[";

	@Test
	public void testRenderWithCursorCell() throws Exception {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 20, 1);
		final String output = ConsoleTestHelper.captureSystemOut(() -> renderer.render("ab", "c", "d", new TextAttribute[] { TextAttribute.UNDERLINE }));
		assertEquals(ESC + "1G> ab" + ESC + "4mc" + ESC + "0md" + ESC + "J", output);
	}

	@Test
	public void testRenderWrappedInputAndRedraw() throws Exception {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 1);
		// 2 + 9 + 1 = 12 characters in 10 columns: the cursor ends in the second row
		ConsoleTestHelper.captureSystemOut(() -> renderer.render("abcdefghi", " ", "", new TextAttribute[] { TextAttribute.REVERSE_VIDEO }));
		final String redraw = ConsoleTestHelper.captureSystemOut(() -> renderer.render("", " ", "", new TextAttribute[0]));
		assertEquals(ESC + "1A" + ESC + "1G> " + " " + ESC + "0m" + ESC + "J", redraw);
	}

	@Test
	public void testRenderEndingAtRightMargin() throws Exception {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 1);
		final String output = ConsoleTestHelper.captureSystemOut(() -> renderer.render("abcdefg", " ", "", new TextAttribute[0]));
		assertEquals(ESC + "1G> abcdefg " + ESC + "0m\r\n" + ESC + "J", output);
	}

	@Test
	public void testRenderFinal() throws Exception {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("Name: ", 10, 3);
		// Wraps into the second row first
		ConsoleTestHelper.captureSystemOut(() -> renderer.render("abcdef", " ", "", new TextAttribute[0]));
		final String finalOutput = ConsoleTestHelper.captureSystemOut(() -> renderer.renderFinal("abc"));
		assertEquals(ESC + "1A" + ESC + "3GName: abc" + ESC + "J" + System.lineSeparator(), finalOutput);

		// After the final drawing the renderer starts in the current row again
		final String nextOutput = ConsoleTestHelper.captureSystemOut(() -> renderer.renderFinal("x"));
		assertEquals(ESC + "3GName: x" + ESC + "J" + System.lineSeparator(), nextOutput);
	}
}
