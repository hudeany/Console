package de.soderer.console;

/**
 * Cursor position in a terminal.
 */
public class Position {
	private final int line;
	private final int column;

	/**
	 * Creates a position.
	 *
	 * @param line 1-based line
	 * @param column 1-based column
	 */
	public Position(final int line, final int column) {
		super();
		this.line = line;
		this.column = column;
	}

	/**
	 * Returns the line of this position.
	 *
	 * @return 1-based line
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the column of this position.
	 *
	 * @return 1-based column
	 */
	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return "line: " + line + ", column: " + column;
	}
}
