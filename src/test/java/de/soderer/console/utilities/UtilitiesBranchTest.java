package de.soderer.console.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Additional tests of the console utility classes for less common values.
 */
@SuppressWarnings("static-method")
public class UtilitiesBranchTest {
	@Test
	public void testHumanReadableDecimalBelowUnit() {
		assertEquals("5.50 B", Utilities.getHumanReadableNumber(Double.valueOf(5.5), "B", true, 3, true, Locale.ENGLISH));
		assertEquals("0.500", Utilities.getHumanReadableNumber(Double.valueOf(0.5), null, true, 3, true, Locale.ENGLISH));
		assertEquals("123", Utilities.getHumanReadableNumber(Double.valueOf(123), "", true, 3, true, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableWithoutTrailingZeros() {
		assertEquals("1.5 kB", Utilities.getHumanReadableNumber(Long.valueOf(1500), "B", true, 3, false, Locale.ENGLISH));
		assertEquals("2.0 kB", Utilities.getHumanReadableNumber(Long.valueOf(2000), "B", true, 3, false, Locale.ENGLISH));
		assertEquals("123.0 MB", Utilities.getHumanReadableNumber(Long.valueOf(123_000_000), "B", true, 3, false, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableSpecialValues() {
		assertTrue(Utilities.getHumanReadableNumber(Double.valueOf(Double.NaN), "B", true, 3, true, Locale.ENGLISH).contains("NaN")
				|| Utilities.getHumanReadableNumber(Double.valueOf(Double.NaN), "B", true, 3, true, Locale.ENGLISH).contains("�"));
		final String infinity = Utilities.getHumanReadableNumber(Double.valueOf(Double.POSITIVE_INFINITY), "B", true, 3, true, Locale.ENGLISH);
		assertTrue(infinity.contains("∞"), infinity);
		// Larger than the largest unit prefix "E"
		final String huge = Utilities.getHumanReadableNumber(Double.valueOf(5e21), "B", true, 3, true, Locale.ENGLISH);
		assertTrue(huge.endsWith(" EB"), huge);
	}

	@Test
	public void testReplaceUsersHomeNullAndVariables() {
		assertNull(Utilities.replaceUsersHome(null));
		assertNull(Utilities.replaceUsersHomeByTilde(null));
		final String home = System.getProperty("user.home");
		assertEquals(home, Utilities.replaceUsersHome("~"));
		assertEquals(home + "/x", Utilities.replaceUsersHome("${HOME}/x"));
		assertEquals(home + "/x", Utilities.replaceUsersHome("$HOME/x"));
		assertEquals("~", Utilities.replaceUsersHomeByTilde(home));
	}

	@Test
	public void testTimespanLeadingZerosVariants() {
		final Duration duration = Duration.ofDays(9).plusHours(3).plusMinutes(4);
		assertEquals("01w 02d", DateUtilities.getShortHumanReadableTimespan(duration, false, true));
		assertEquals("1w 2d", DateUtilities.getShortHumanReadableTimespan(duration, false, false));
		assertEquals("00s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, true, true));
		assertEquals("0s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, true, false));
		assertEquals("5s 7ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(5007), true, false));
		assertEquals("05s 007ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(5007), true, true));
		assertEquals("2m 5s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(125), true, false));
		assertEquals("1y 1w", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(7 * 53), false, false));
	}

	@Test
	public void testPrivateConstructors() throws Exception {
		for (final Class<?> utilityClass : new Class<?>[] { Utilities.class, DateUtilities.class, NumberUtilities.class, SystemUtilities.class }) {
			try {
				final Constructor<?> constructor = utilityClass.getDeclaredConstructor();
				constructor.setAccessible(true);
				constructor.newInstance();
			} catch (@SuppressWarnings("unused") final NoSuchMethodException e) {
				// Class without explicit constructor
			}
		}
	}
}
