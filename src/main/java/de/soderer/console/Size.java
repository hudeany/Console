package de.soderer.console;

/**
 * Size of a terminal in characters.
 */
public class Size {
	private final int height;
	private final int width;

	/**
	 * Creates a size.
	 *
	 * @param height number of lines
	 * @param width number of columns
	 */
	public Size(final int height, final int width) {
		super();
		this.height = height;
		this.width = width;
	}

	/**
	 * Returns the number of lines.
	 *
	 * @return number of lines
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the number of columns.
	 *
	 * @return number of columns
	 */
	public int getWidth() {
		return width;
	}

	@Override
	public String toString() {
		return "Height: " + height + ", Width: " + width;
	}
}
