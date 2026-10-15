package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class InputLineRendererTest {
	private static final String ESC = "\033[";

	@Test
	public void testShortInputStaysInOneRow() {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 1);
		assertEquals(ESC + "1G> abc" + ESC + "J", renderer.buildPlainOutputForTest("abc"));
		// Redrawn in the same row: no cursor up
		assertEquals(ESC + "1G> abcd" + ESC + "J", renderer.buildPlainOutputForTest("abcd"));
	}

	@Test
	public void testWrappedInputMovesUpRelatively() {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 1);
		// 2 + 12 = 14 characters in 10 columns: cursor ends in the second row
		renderer.buildPlainOutputForTest("abcdefghijkl");
		assertEquals(ESC + "1A" + ESC + "1G> x" + ESC + "J", renderer.buildPlainOutputForTest("x"));
	}

	@Test
	public void testTextEndingAtRightMarginMovesToNextRowExplicitly() {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 1);
		assertEquals(ESC + "1G> abcdefgh\r\n" + ESC + "J", renderer.buildPlainOutputForTest("abcdefgh"));
		assertEquals(ESC + "1A" + ESC + "1G> a" + ESC + "J", renderer.buildPlainOutputForTest("a"));
	}

	@Test
	public void testStartColumnIsKept() {
		final InputLineRenderer renderer = InputLineRenderer.createForTest("> ", 10, 6);
		// Starts in column 6: 5 + 2 + 4 = 11 characters reach into the second row
		renderer.buildPlainOutputForTest("abcd");
		assertEquals(ESC + "1A" + ESC + "6G> " + ESC + "J", renderer.buildPlainOutputForTest(""));
	}
}
