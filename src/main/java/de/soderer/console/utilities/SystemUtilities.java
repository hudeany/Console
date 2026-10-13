package de.soderer.console.utilities;

import java.util.Locale;

/**
 * Internal operating system detection of the console library.
 */
public class SystemUtilities {
	private static final String OS_NAME_LOWER_CASE = getOsName() == null ? "" : getOsName().toLowerCase(Locale.ROOT);
	private static final boolean isWindows = OS_NAME_LOWER_CASE.contains("windows");

	private SystemUtilities() {
		// Utility class
	}

	/**
	 * Name of the operating system (system property "os.name").
	 *
	 * @return operating system name, e.g. "Linux" or "Windows 11"
	 */
	public static String getOsName() {
		return System.getProperty("os.name");
	}

	/**
	 * Whether the operating system is Windows.
	 *
	 * @return true on Windows
	 */
	public static boolean isWindowsSystem() {
		return isWindows;
	}
}
