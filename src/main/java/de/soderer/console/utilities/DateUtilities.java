package de.soderer.console.utilities;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Internal date and time helpers of the console library: ETA estimation and human readable durations.
 */
public class DateUtilities {
	/**
	 * Utility class
	 */
	private DateUtilities() {
		// Utility class
	}

	/**
	 * Estimates the end time of a running task by linear extrapolation of the progress so far.
	 *
	 * @param start
	 *            the start time of the task
	 * @param itemsToDo
	 *            the total number of items to process
	 * @param itemsDone
	 *            the number of items processed so far
	 * @return the estimated end time, the current time if all items are done, or null if the values
	 *         do not allow an estimation (no progress yet, invalid counts or start in the future)
	 */
	public static LocalDateTime calculateETA(final LocalDateTime start, final long itemsToDo, final long itemsDone) {
		if (start == null || itemsToDo <= 0 || itemsDone <= 0 || itemsToDo < itemsDone) {
			return null;
		} else {
			final LocalDateTime now = LocalDateTime.now();
			if (start.isAfter(now)) {
				return null;
			} else if (itemsDone >= itemsToDo) {
				return now;
			} else {
				final Duration durationSinceStartToNow = Duration.between(start, now);
				final Duration durationFromStartToEnd = Duration.ofNanos((long) ((float) itemsToDo / itemsDone * durationSinceStartToNow.toNanos()));
				final LocalDateTime estimatedEnd = start.plus(durationFromStartToEnd);
				return estimatedEnd;
			}
		}
	}

	/**
	 * Formats a duration in short form like "1h 02m 03s". Only the most significant parts are shown:
	 * seconds only below one hour, minutes only below one day, hours only below one week and days only
	 * below one year. A year is counted as 52 weeks.
	 *
	 * @param duration
	 *            the duration
	 * @param showMillis
	 *            true to show milliseconds for durations below one minute
	 * @param showLeadingZeros
	 *            true to pad the values with leading zeros
	 * @return the formatted duration, "0s" or "00s" for a zero duration
	 */
	public static String getShortHumanReadableTimespan(final Duration duration, final boolean showMillis, final boolean showLeadingZeros) {
		final StringBuilder returnValue = new StringBuilder();

		final long millis = duration.toMillisPart();
		final long seconds = duration.toSecondsPart();
		final long minutes = duration.toMinutesPart();
		final long hours = duration.toHoursPart();
		final long days = duration.toDays() % 7;
		final long weeks = duration.toDays() / 7 % 52;
		final long years = duration.toDays() / 7 / 52;

		if (showMillis && millis != 0 && minutes == 0 && hours == 0 && days == 0 && weeks == 0 && years == 0) {
			returnValue.insert(0, "ms");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%03d", millis));
			} else {
				returnValue.insert(0, millis);
			}
		}

		if (seconds != 0 && hours == 0 && days == 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "s");

			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", seconds));
			} else {
				returnValue.insert(0, seconds);
			}
		}

		if (minutes != 0 && days == 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "m");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", minutes));
			} else {
				returnValue.insert(0, minutes);
			}
		}

		if (hours != 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "h");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", hours));
			} else {
				returnValue.insert(0, hours);
			}
		}

		if (days != 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "d");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", days));
			} else {
				returnValue.insert(0, days);
			}
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "w");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", weeks));
			} else {
				returnValue.insert(0, weeks);
			}
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "y");
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else {
			if (showLeadingZeros) {
				return "00s";
			} else {
				return "0s";
			}
		}
	}
}
