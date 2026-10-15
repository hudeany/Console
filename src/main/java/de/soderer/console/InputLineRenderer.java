package de.soderer.console;

import de.soderer.console.ConsoleUtilities.TextAttribute;

/**
 * Draws an input line, which may be longer than the terminal is wide, for {@link SimpleConsoleInput} and
 * {@link PasswordConsoleInput}.
 * <p>
 * The input is redrawn relative to the current cursor position (cursor up by the number of rows drawn before, then to
 * the start column), instead of returning to a saved absolute screen position. A saved position becomes wrong as soon
 * as a long input wraps at the bottom of the screen and the terminal scrolls, which made the input drift downwards.
 * <p>
 * The real cursor stays hidden while drawing. The edit position is shown by a formatted cursor cell, so the real cursor
 * always stays at the end of the drawn text, where its row is known.
 */
final class InputLineRenderer {
	private static final int DEFAULT_TERMINAL_WIDTH = 80;

	private final String prompt;
	private final int terminalWidth;
	/** 1-based column the prompt starts in */
	private final int startColumn;
	/** Number of rows below the start row, the real cursor is in after the last drawing */
	private int renderedRows = 0;

	private InputLineRenderer(final String prompt, final int terminalWidth, final int startColumn) {
		this.prompt = prompt;
		this.terminalWidth = terminalWidth;
		this.startColumn = startColumn;
	}

	/**
	 * Prepares the drawing of an input at the current cursor position. Lines of a multi line prompt except the last
	 * one are printed here once, so only the last prompt line is redrawn with the input.
	 *
	 * @param prompt prompt shown in front of the input, never null
	 * @return renderer for this input
	 */
	static InputLineRenderer start(final String prompt) {
		String inputLinePrompt = prompt;
		final int lastLineBreakIndex = prompt.lastIndexOf('\n');
		if (lastLineBreakIndex >= 0) {
			System.out.print(prompt.substring(0, lastLineBreakIndex + 1));
			inputLinePrompt = prompt.substring(lastLineBreakIndex + 1);
		}

		int terminalWidth = DEFAULT_TERMINAL_WIDTH;
		try {
			terminalWidth = ConsoleUtilities.getTerminalSize().getWidth();
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Keep the default width
		}
		if (terminalWidth < 1) {
			terminalWidth = DEFAULT_TERMINAL_WIDTH;
		}

		int startColumn = 1;
		try {
			startColumn = ConsoleUtilities.getCursorPosition().getColumn();
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Assume the input starts at the beginning of the line
		}
		if (startColumn < 1 || startColumn > terminalWidth) {
			startColumn = 1;
		}

		return new InputLineRenderer(inputLinePrompt, terminalWidth, startColumn);
	}

	/**
	 * Package-private for tests: creates a renderer without terminal requests.
	 */
	static InputLineRenderer createForTest(final String prompt, final int terminalWidth, final int startColumn) {
		return new InputLineRenderer(prompt, terminalWidth, startColumn);
	}

	/**
	 * Draws the input with a formatted cursor cell.
	 *
	 * @param textBeforeCursor displayed text in front of the cursor
	 * @param cursorCellText displayed character at the cursor (a blank at the end of the input)
	 * @param textAfterCursor displayed text after the cursor
	 * @param cursorTextAttributes formatting of the cursor cell
	 */
	void render(final String textBeforeCursor, final String cursorCellText, final String textAfterCursor, final TextAttribute[] cursorTextAttributes) {
		final StringBuilder output = new StringBuilder();
		appendMoveToStart(output);
		output.append(prompt);
		output.append(textBeforeCursor);
		output.append(ConsoleUtilities.getAnsiFormattedText(cursorCellText, null, null, cursorTextAttributes));
		output.append(textAfterCursor);
		appendEndOfDrawing(output, prompt.length() + textBeforeCursor.length() + cursorCellText.length() + textAfterCursor.length());
		System.out.print(output);
		System.out.flush();
	}

	/**
	 * Draws the input a last time without cursor cell and moves to the next line.
	 *
	 * @param text displayed input
	 */
	void renderFinal(final String text) {
		final StringBuilder output = new StringBuilder();
		appendMoveToStart(output);
		output.append(prompt);
		output.append(text);
		// Clear the rest of an earlier, longer drawing
		output.append("\033[J");
		System.out.println(output);
		renderedRows = 0;
	}

	/**
	 * Package-private for tests: builds the output of {@link #render(String, String, String, TextAttribute[])}
	 * without cursor formatting and without printing it.
	 */
	String buildPlainOutputForTest(final String text) {
		final StringBuilder output = new StringBuilder();
		appendMoveToStart(output);
		output.append(prompt).append(text);
		appendEndOfDrawing(output, prompt.length() + text.length());
		return output.toString();
	}

	private void appendMoveToStart(final StringBuilder output) {
		if (renderedRows > 0) {
			// Cursor up: relative, so it stays correct when the terminal has scrolled
			output.append("\033[").append(renderedRows).append('A');
		}
		// Cursor to the start column of this row
		output.append("\033[").append(startColumn).append('G');
	}

	private void appendEndOfDrawing(final StringBuilder output, final int drawnLength) {
		final int endOffset = startColumn - 1 + drawnLength;
		if (endOffset > 0 && endOffset % terminalWidth == 0) {
			// The text ends exactly at the right margin: terminals keep the cursor in the last column then ("pending wrap"),
			// so the row of the cursor would be ambiguous. Moving to the next line explicitly makes it definite.
			output.append("\r\n");
		}
		// Clear the rest of an earlier, longer drawing up to the end of the screen
		output.append("\033[J");
		renderedRows = endOffset / terminalWidth;
	}
}
