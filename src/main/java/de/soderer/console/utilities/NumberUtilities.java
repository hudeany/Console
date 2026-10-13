package de.soderer.console.utilities;

import java.util.regex.Pattern;

/**
 * Internal number helpers of the console library.
 */
public class NumberUtilities {

	private NumberUtilities() {
		// Utility class
	}

	/**
	 * Number in english notation without grouping: optional sign, digits with optional decimals after a dot, optional exponent.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern.compile("[+-]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][+-]?[0-9]+)?");

	/**
	 * Check for a number in english notation without grouping separators, like {@code -12.5E-3}.
	 *
	 * @param numberString
	 *            string to check
	 * @return true if the string is a number, false for null
	 */
	public static boolean isNumber(final String numberString) {
		return numberString != null && NUMBER_PATTERN.matcher(numberString).matches();
	}
}
