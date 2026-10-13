package de.soderer.console.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class DateUtilitiesTest {
	@Test
	public void testTimespanZero() {
		assertEquals("0s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, false, false));
		assertEquals("00s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, false, true));
	}

	@Test
	public void testTimespanSecondsAndMinutes() {
		assertEquals("5s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(5), false, false));
		assertEquals("1m 5s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(65), false, false));
		assertEquals("01m 05s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(65), false, true));
		assertEquals("2m", DateUtilities.getShortHumanReadableTimespan(Duration.ofMinutes(2), false, false));
	}

	@Test
	public void testTimespanMillis() {
		final Duration duration = Duration.ofMillis(1500);
		assertEquals("1s 500ms", DateUtilities.getShortHumanReadableTimespan(duration, true, false));
		assertEquals("01s 500ms", DateUtilities.getShortHumanReadableTimespan(duration, true, true));
		assertEquals("1s", DateUtilities.getShortHumanReadableTimespan(duration, false, false));
		assertEquals("007ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(7), true, true));
	}

	@Test
	public void testTimespanMillisHiddenFromOneMinute() {
		assertEquals("1m 1s", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(61_500), true, false));
	}

	@Test
	public void testTimespanHoursHideSeconds() {
		final Duration duration = Duration.ofHours(1).plusMinutes(2).plusSeconds(3);
		assertEquals("1h 2m", DateUtilities.getShortHumanReadableTimespan(duration, false, false));
		assertEquals("01h 02m", DateUtilities.getShortHumanReadableTimespan(duration, false, true));
	}

	@Test
	public void testTimespanDaysHideMinutes() {
		final Duration duration = Duration.ofDays(1).plusHours(2).plusMinutes(3);
		assertEquals("1d 2h", DateUtilities.getShortHumanReadableTimespan(duration, false, false));
	}

	@Test
	public void testTimespanWeeksHideHours() {
		final Duration duration = Duration.ofDays(8).plusHours(5);
		assertEquals("1w 1d", DateUtilities.getShortHumanReadableTimespan(duration, false, false));
	}

	@Test
	public void testTimespanYears() {
		// A year is counted as 52 weeks, smaller parts than weeks are hidden
		assertEquals("1y", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(52 * 7 + 1), false, false));
		assertEquals("2y 03w", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(2 * 52 * 7 + 3 * 7), false, true));
	}

	@Test
	public void testEtaInvalidInput() {
		final LocalDateTime start = LocalDateTime.now().minusSeconds(10);
		assertNull(DateUtilities.calculateETA(null, 100, 10));
		assertNull(DateUtilities.calculateETA(start, 0, 0));
		assertNull(DateUtilities.calculateETA(start, 100, 0));
		assertNull(DateUtilities.calculateETA(start, -1, 10));
		assertNull(DateUtilities.calculateETA(start, 100, 150));
		assertNull(DateUtilities.calculateETA(LocalDateTime.now().plusHours(1), 100, 10));
	}

	@Test
	public void testEtaAllDone() {
		final LocalDateTime before = LocalDateTime.now();
		final LocalDateTime eta = DateUtilities.calculateETA(before.minusSeconds(10), 100, 100);
		assertNotNull(eta);
		assertTrue(!eta.isBefore(before) && !eta.isAfter(LocalDateTime.now()), "ETA should be now: " + eta);
	}

	@Test
	public void testEtaHalfDone() {
		final LocalDateTime start = LocalDateTime.now().minusSeconds(10);
		final LocalDateTime eta = DateUtilities.calculateETA(start, 1000, 500);
		assertNotNull(eta);
		// Linear extrapolation: half done after 10 seconds means 20 seconds in total
		final long millisFromStart = Duration.between(start, eta).toMillis();
		assertTrue(millisFromStart >= 19_000 && millisFromStart <= 21_000, "Unexpected ETA offset: " + millisFromStart + "ms");
	}
}
