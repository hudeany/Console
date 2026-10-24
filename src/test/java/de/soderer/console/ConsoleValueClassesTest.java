package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests of the small value classes {@link Position}, {@link Size} and {@link ConsoleType}.
 */
@SuppressWarnings("static-method")
public class ConsoleValueClassesTest {
	@Test
	public void testPosition() {
		final Position position = new Position(3, 14);
		assertEquals(3, position.getLine());
		assertEquals(14, position.getColumn());
		assertEquals("line: 3, column: 14", position.toString());
	}

	@Test
	public void testSize() {
		final Size size = new Size(24, 80);
		assertEquals(24, size.getHeight());
		assertEquals(80, size.getWidth());
		assertEquals("Height: 24, Width: 80", size.toString());
	}

	@Test
	public void testConsoleType() {
		assertEquals(3, ConsoleType.values().length);
		assertEquals(ConsoleType.NO_ANSI, ConsoleType.valueOf("NO_ANSI"));
	}
}
