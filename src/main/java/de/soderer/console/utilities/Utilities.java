package de.soderer.console.utilities;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Internal string, number and file path helpers of the console library.
 */
public class Utilities {
	private Utilities() {
		// Utility class
	}

	/**
	 * Make a number with unitsign human readable
	 *
	 * @param value
	 *            number to format
	 * @param unitTypeSign
	 *            unit sign appended to the unit prefix, e.g. "B" for bytes
	 * @param siUnits
	 *            true for SI units (factor 1000), false for binary units (factor 1024)
	 * @param amountOfSignifiantDigits
	 *            number of significant digits
	 * @param keepTrailingZeros
	 *            keep trailing zeros of the decimals
	 * @param locale
	 *            locale for the decimal separator
	 * @return formatted number with unit, e.g. "1.5 KiB"
	 */
	public static String getHumanReadableNumber(final Number value, final String unitTypeSign, final boolean siUnits, final int amountOfSignifiantDigits, final boolean keepTrailingZeros, final Locale locale) {
		final int unit = siUnits ? 1000 : 1024;
		final double doubleValue = value.doubleValue();

		if (doubleValue < unit && (value instanceof Integer || value instanceof Long)) {
			return value + getHumanReadableUnitExtension(0, unitTypeSign, siUnits);
		}

		int exponent = 0;
		if (doubleValue >= unit && !Double.isInfinite(doubleValue)) {
			// "E" is the largest available unit prefix
			exponent = Math.min((int) (Math.log(doubleValue) / Math.log(unit)), HUMAN_READABLE_MAX_EXPONENT);
		}
		double interimValue = doubleValue / Math.pow(unit, exponent);

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		DecimalFormat numberFormat = createHumanReadableNumberFormat(interimValue, amountOfSignifiantDigits, keepTrailingZeros, decimalFormatSymbols);

		if (!Double.isNaN(interimValue) && !Double.isInfinite(interimValue)) {
			// Rounding may carry over into the next decade or even the next unit prefix (e.g. 999.96 -> "1000.0"),
			// so the format is chosen by the rounded value instead of the raw value
			double roundedValue = new BigDecimal(interimValue).setScale(numberFormat.getMaximumFractionDigits(), numberFormat.getRoundingMode()).doubleValue();
			if (roundedValue >= unit && exponent < HUMAN_READABLE_MAX_EXPONENT) {
				exponent++;
				interimValue = doubleValue / Math.pow(unit, exponent);
				roundedValue = roundedValue / unit;
			}
			numberFormat = createHumanReadableNumberFormat(roundedValue, amountOfSignifiantDigits, keepTrailingZeros, decimalFormatSymbols);
		}

		return numberFormat.format(interimValue) + getHumanReadableUnitExtension(exponent, unitTypeSign, siUnits);
	}

	/** Index of the largest unit prefix "E" in "kMGTPE" (1-based exponent) */
	private static final int HUMAN_READABLE_MAX_EXPONENT = 6;

	private static String getHumanReadableUnitExtension(final int exponent, final String unitTypeSign, final boolean siUnits) {
		if (exponent == 0) {
			return isNotBlank(unitTypeSign) ? " " + unitTypeSign : "";
		} else {
			return " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exponent - 1) + (siUnits ? "" : "i") + (isNotBlank(unitTypeSign) ? unitTypeSign : "");
		}
	}

	private static DecimalFormat createHumanReadableNumberFormat(final double magnitudeValue, final int amountOfSignifiantDigits, final boolean keepTrailingZeros, final DecimalFormatSymbols decimalFormatSymbols) {
		final int integerDigits;
		if (magnitudeValue >= 1000) {
			integerDigits = 4;
		} else if (magnitudeValue >= 100) {
			integerDigits = 3;
		} else if (magnitudeValue >= 10) {
			integerDigits = 2;
		} else if (magnitudeValue >= 1) {
			integerDigits = 1;
		} else {
			integerDigits = 0;
		}

		if (keepTrailingZeros) {
			final int fractionDigits = amountOfSignifiantDigits - integerDigits;
			// Pattern "#0." would always show the decimal separator, e.g. "1000,"
			return new DecimalFormat(fractionDigits > 0 ? "#0." + repeat("0", fractionDigits) : "#0", decimalFormatSymbols);
		} else {
			// At least one decimal is always shown, further ones only up to the amount of significant digits
			return new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - integerDigits - 1), decimalFormatSymbols);
		}
	}

	/**
	 * Check whether a string is null, empty or contains only whitespace (characters up to U+0020, see {@link String#trim()}).
	 *
	 * @param value string to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final String value) {
		return value == null || value.length() == 0 || value.trim().length() == 0;
	}

	/**
	 * Check whether a string contains at least one non-whitespace character (see {@link #isBlank(String)}).
	 *
	 * @param value string to check
	 * @return true if the string is not blank
	 */
	public static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	/**
	 * Overwrite all characters of an array with zero, e.g. to remove a password from memory.
	 *
	 * @param array array to clear, may be null
	 */
	public static void clear(final char[] array) {
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				array[i] = 0;
			}
		}
	}

	/**
	 * Check whether a character array is null, empty or contains only whitespace (see {@link Character#isWhitespace(char)}).
	 *
	 * @param value array to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final char[] value) {
		if (value == null || value.length == 0) {
			return true;
		} else {
			for (final char character : value) {
				if (!Character.isWhitespace(character)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Build a string by repeating a character.
	 *
	 * @param valueChar character to repeat
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated characters
	 */
	public static String repeat(final char valueChar, final int count) {
		return repeat(Character.toString(valueChar), count, null);
	}

	/**
	 * Build a string by repeating a text.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count) {
		return repeat(value, count, null);
	}

	/**
	 * Build a string by repeating a text with a separator between the repetitions.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @param separatorString separator between the repetitions, null for none
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count, final String separatorString) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count == 0) {
			return "";
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				if (separatorString != null && returnValue.length() > 0) {
					returnValue.append(separatorString);
				}
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	/**
	 * Prepend blanks at the left of a string to make it fit the given minimum length
	 *
	 * @param value
	 *            string to pad
	 * @param minimumLength
	 *            minimum length of the result
	 * @return padded string, unchanged if it is already long enough
	 */
	public static String leftPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	/**
	 * Replace a leading "~" as well as "$HOME" and "${HOME}" in a file path by the user's home directory.
	 *
	 * @param filePath file path, may be null
	 * @return file path with the home directory, or null for null input
	 */
	public static String replaceUsersHome(String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");

		// "~" only stands for the home directory at the start of the path, otherwise it is a normal file name character
		if ("~".equals(filePath)) {
			filePath = homePath;
		} else if (filePath.startsWith("~/") || filePath.startsWith("~" + File.separator)) {
			filePath = homePath + filePath.substring(1);
		}

		return filePath
				.replace("${HOME}", homePath)
				.replace("$HOME", homePath);
	}

	/**
	 * Replace a leading user's home directory in a file path by "~".
	 *
	 * @param filePath file path, may be null
	 * @return file path starting with "~" if it is within the home directory, otherwise unchanged
	 */
	public static String replaceUsersHomeByTilde(final String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");

		// Only a complete leading home path is replaced, so "/home/user2" is not changed to "~2" for home "/home/user"
		if (filePath.equals(homePath)) {
			return "~";
		} else if (filePath.startsWith(homePath + "/") || filePath.startsWith(homePath + File.separator)) {
			return "~" + filePath.substring(homePath.length());
		} else {
			return filePath;
		}
	}

	/**
	 * Convert up to 4 bytes in big endian order into an int.
	 *
	 * @param bytes 0 to 4 bytes, most significant first
	 * @return int value (unsigned interpretation for less than 4 bytes)
	 * @throws Exception if more than 4 bytes are given
	 */
	public static int convertByteArrayToInt(final byte[] bytes) throws Exception {
		if (bytes.length > 4) {
			throw new Exception("ByteArray is too big for int value");
		} else {
			// Big endian, also for less than 4 bytes (before, ByteBuffer.getInt() threw a BufferUnderflowException for them)
			int value = 0;
			for (final byte nextByte : bytes) {
				value = (value << 8) | (nextByte & 0xFF);
			}
			return value;
		}
	}
}
